package com.gregtechceu.gtceu.data.pattern.binary;

import com.gregtechceu.gtceu.api.multiblock.structurepredicate.StructurePredicate;
import com.gregtechceu.gtceu.data.pattern.StructurePatternResolver;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.jspecify.annotations.NullMarked;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Reads and writes compressed multiblock definitions without serializing runtime objects.
 *
 * <p>
 * The payload contains only repeat units, slices and registered predicate definitions. Runtime
 * predicates are restored through the explicit {@code StructurePredicate} codec registry and are
 * compiled by the normal structure resolver after decoding.
 * </p>
 */
@NullMarked
public final class PatternBinaryCodec {

    private static final int MAGIC = 0x4754504D;
    private static final int FORMAT_VERSION = 1;
    private static final int COMPRESSION_ZSTD = 1;
    private static final int HEADER_SIZE = Integer.BYTES + 4 * Integer.BYTES;
    private static final int MAX_PAYLOAD_BYTES = 16 * 1024 * 1024;
    private static final int MAX_UNITS = 4096;
    private static final int MAX_SLICES_PER_UNIT = 4096;
    private static final int MAX_ROWS_PER_SLICE = 4096;
    private static final int MAX_ROW_BYTES = 1 << 20;
    private static final int MAX_PREDICATES = 4096;
    private static final int ZSTD_LEVEL = 5;

    private PatternBinaryCodec() {}

    public static StructurePatternResolver.StringArrayDefinition read(Path file) throws IOException {
        byte[] encoded = Files.readAllBytes(file);
        if (encoded.length < HEADER_SIZE) {
            throw new IOException("Compressed structure definition is shorter than its header: " + file);
        }

        ByteBuffer header = ByteBuffer.wrap(encoded);
        int magic = header.getInt();
        int version = header.getInt();
        int compression = header.getInt();
        int uncompressedLength = header.getInt();
        int checksum = header.getInt();

        if (magic != MAGIC) {
            throw new IOException("Invalid structure binary magic in " + file);
        }
        if (version != FORMAT_VERSION) {
            throw new IOException("Unsupported structure binary version " + version + " in " + file);
        }
        if (compression != COMPRESSION_ZSTD) {
            throw new IOException("Unsupported structure binary compression " + compression + " in " + file);
        }
        if (uncompressedLength < 0 || uncompressedLength > MAX_PAYLOAD_BYTES) {
            throw new IOException("Invalid uncompressed structure binary length " + uncompressedLength);
        }

        byte[] compressed = new byte[encoded.length - HEADER_SIZE];
        System.arraycopy(encoded, HEADER_SIZE, compressed, 0, compressed.length);
        byte[] payload = decompress(compressed, uncompressedLength);
        if (crc32(payload) != checksum) {
            throw new IOException("Structure binary checksum mismatch in " + file);
        }
        return decodePayload(payload, file);
    }

    public static void write(Path file, StructurePatternResolver.StringArrayDefinition definition) throws IOException {
        byte[] payload = encodePayload(definition);
        byte[] compressed = compress(payload);
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE)
                .putInt(MAGIC)
                .putInt(FORMAT_VERSION)
                .putInt(COMPRESSION_ZSTD)
                .putInt(payload.length)
                .putInt(crc32(payload));

        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent == null) {
            throw new IOException("Structure binary has no parent directory: " + file);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                channel.write((ByteBuffer) header.flip());
                channel.write(ByteBuffer.wrap(compressed));
                channel.force(true);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Atomic replacement is not supported for structure binary " + file, exception);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static byte[] encodePayload(StructurePatternResolver.StringArrayDefinition definition) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream output = new DataOutputStream(bytes)) {
            List<StructurePatternResolver.Unit> units = definition.units();
            writeBoundedCount(output, units.size(), MAX_UNITS, "units");
            for (StructurePatternResolver.Unit unit : units) {
                if (unit.minRepeat() < 0 || unit.maxRepeat() < unit.minRepeat()) {
                    throw new IOException("Invalid repeat range " + unit.minRepeat() + ".." + unit.maxRepeat());
                }
                writeVarInt(output, unit.minRepeat());
                writeVarInt(output, unit.maxRepeat());
                writeBoundedCount(output, unit.slices().size(), MAX_SLICES_PER_UNIT, "slices");
                for (String[] slice : unit.slices()) {
                    writeBoundedCount(output, slice.length, MAX_ROWS_PER_SLICE, "rows");
                    for (String row : slice) {
                        writeString(output, row);
                    }
                }
            }

            List<Map.Entry<Character, StructurePredicate>> predicates = new ArrayList<>(
                    definition.predicates().entrySet());
            predicates.sort(Comparator.comparingInt(entry -> entry.getKey()));
            writeBoundedCount(output, predicates.size(), MAX_PREDICATES, "predicates");
            for (Map.Entry<Character, StructurePredicate> entry : predicates) {
                output.writeChar(entry.getKey());
                JsonElement encoded = StructurePredicate.CODEC
                        .encodeStart(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow(error -> new IOException(
                                "Failed to encode predicate " + entry.getKey() + ": " + error));
                writeString(output, encoded.toString());
            }
            output.flush();
            if (bytes.size() > MAX_PAYLOAD_BYTES) {
                throw new IOException("Structure binary payload exceeds " + MAX_PAYLOAD_BYTES + " bytes");
            }
            return bytes.toByteArray();
        }
    }

    private static StructurePatternResolver.StringArrayDefinition decodePayload(byte[] payload, Path file)
                                                                                                           throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            int unitCount = readBoundedCount(input, MAX_UNITS, "units");
            List<StructurePatternResolver.Unit> units = new ArrayList<>(unitCount);
            for (int unitIndex = 0; unitIndex < unitCount; unitIndex++) {
                int minRepeat = readVarInt(input, "unit min repeat");
                int maxRepeat = readVarInt(input, "unit max repeat");
                if (minRepeat < 0 || maxRepeat < minRepeat) {
                    throw new IOException("Invalid repeat range in " + file);
                }
                int sliceCount = readBoundedCount(input, MAX_SLICES_PER_UNIT, "slices");
                List<String[]> slices = new ArrayList<>(sliceCount);
                for (int sliceIndex = 0; sliceIndex < sliceCount; sliceIndex++) {
                    int rowCount = readBoundedCount(input, MAX_ROWS_PER_SLICE, "rows");
                    String[] rows = new String[rowCount];
                    for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
                        rows[rowIndex] = readString(input, MAX_ROW_BYTES, "row");
                    }
                    slices.add(rows);
                }
                units.add(new StructurePatternResolver.Unit(slices, minRepeat, maxRepeat));
            }

            int predicateCount = readBoundedCount(input, MAX_PREDICATES, "predicates");
            Map<Character, StructurePredicate> predicates = new LinkedHashMap<>(
                    predicateCount);
            for (int index = 0; index < predicateCount; index++) {
                char symbol = input.readChar();
                String json = readString(input, MAX_ROW_BYTES, "predicate");
                try {
                    var predicate = StructurePredicate.CODEC
                            .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                            .getOrThrow(IllegalArgumentException::new);
                    if (predicates.put(symbol, predicate) != null) {
                        throw new IOException("Duplicate predicate symbol '" + symbol + "' in " + file);
                    }
                } catch (RuntimeException exception) {
                    throw new IOException("Failed to decode predicate '" + symbol + "' in " + file, exception);
                }
            }
            if (input.available() != 0) {
                throw new IOException("Trailing bytes in structure binary " + file);
            }
            return new StructurePatternResolver.StringArrayDefinition(units, predicates);
        }
    }

    private static byte[] compress(byte[] payload) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ZstdOutputStream output = new ZstdOutputStream(bytes, ZSTD_LEVEL)) {
            output.write(payload);
            output.close();
            return bytes.toByteArray();
        }
    }

    private static byte[] decompress(byte[] compressed, int expectedLength) throws IOException {
        try (ZstdInputStream input = new ZstdInputStream(new ByteArrayInputStream(compressed));
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(expectedLength)) {
            input.transferTo(bytes);
            byte[] payload = bytes.toByteArray();
            if (payload.length != expectedLength) {
                throw new IOException(
                        "Structure binary length mismatch: expected " + expectedLength + ", got " + payload.length);
            }
            return payload;
        }
    }

    private static int crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return (int) crc.getValue();
    }

    private static void writeBoundedCount(DataOutputStream output, int value, int maximum, String name)
                                                                                                        throws IOException {
        if (value < 0 || value > maximum) {
            throw new IOException("Invalid " + name + " count " + value);
        }
        writeVarInt(output, value);
    }

    private static int readBoundedCount(DataInputStream input, int maximum, String name) throws IOException {
        int value = readVarInt(input, name + " count");
        if (value < 0 || value > maximum) {
            throw new IOException("Invalid " + name + " count " + value);
        }
        return value;
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_ROW_BYTES) {
            throw new IOException("String exceeds " + MAX_ROW_BYTES + " bytes");
        }
        writeVarInt(output, bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input, int maximum, String name) throws IOException {
        int length = readVarInt(input, name + " length");
        if (length < 0 || length > maximum) {
            throw new IOException("Invalid " + name + " length " + length);
        }
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("Truncated " + name);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeVarInt(DataOutputStream output, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            output.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.writeByte(value);
    }

    private static int readVarInt(DataInputStream input, String name) throws IOException {
        int result = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int next = input.readUnsignedByte();
            result |= (next & 0x7F) << shift;
            if ((next & 0x80) == 0) {
                return result;
            }
        }
        throw new IOException("Invalid varint for " + name);
    }
}
