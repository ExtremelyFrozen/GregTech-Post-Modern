package com.gregtechceu.gtceu.data.pattern.binary;

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition;
import com.gregtechceu.gtceu.data.pattern.json.PatternJsonCodec;

import net.minecraft.resources.ResourceLocation;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.google.gson.JsonParser;
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
import java.util.zip.CRC32;

/** Compressed canonical pattern definitions with a bounded binary envelope and atomic writes. */
@NullMarked
public final class PatternBinaryCodec {

    private static final int MAGIC = 0x4754504D;
    private static final int FORMAT_VERSION = 1;
    private static final int COMPRESSION_ZSTD = 1;
    private static final int HEADER_SIZE = Integer.BYTES * 5;
    private static final int MAX_PAYLOAD_BYTES = 16 * 1024 * 1024;
    private static final int MAX_JSON_BYTES = MAX_PAYLOAD_BYTES - Integer.BYTES;
    private static final int ZSTD_LEVEL = 5;

    private PatternBinaryCodec() {}

    public static PatternDefinition read(Path file) throws IOException {
        byte[] encoded = Files.readAllBytes(file);
        if (encoded.length <= HEADER_SIZE)
            throw new IOException("Compressed pattern definition is shorter than its header: " + file);
        ByteBuffer header = ByteBuffer.wrap(encoded, 0, HEADER_SIZE);
        int magic = header.getInt();
        int version = header.getInt();
        int compression = header.getInt();
        int uncompressedLength = header.getInt();
        int checksum = header.getInt();
        if (magic != MAGIC) throw new IOException("Invalid pattern binary magic in " + file);
        if (version != FORMAT_VERSION)
            throw new IOException("Unsupported pattern binary version " + version + " in " + file);
        if (compression != COMPRESSION_ZSTD)
            throw new IOException("Unsupported pattern binary compression " + compression + " in " + file);
        if (uncompressedLength <= 0 || uncompressedLength > MAX_PAYLOAD_BYTES)
            throw new IOException("Invalid pattern binary payload length " + uncompressedLength);
        byte[] compressed = new byte[encoded.length - HEADER_SIZE];
        System.arraycopy(encoded, HEADER_SIZE, compressed, 0, compressed.length);
        byte[] payload = decompress(compressed, uncompressedLength);
        if (crc32(payload) != checksum) throw new IOException("Pattern binary checksum mismatch in " + file);
        return decodePayload(payload, file);
    }

    public static void write(Path file, PatternDefinition definition) throws IOException {
        byte[] payload = encodePayload(definition);
        byte[] compressed = compress(payload);
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE)
                .putInt(MAGIC).putInt(FORMAT_VERSION).putInt(COMPRESSION_ZSTD)
                .putInt(payload.length).putInt(crc32(payload));
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent == null) throw new IOException("Pattern binary has no parent directory: " + file);
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                writeFully(channel, (ByteBuffer) header.flip());
                writeFully(channel, ByteBuffer.wrap(compressed));
                channel.force(true);
            }
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Atomic replacement is not supported for pattern binary " + file, exception);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static byte[] encodePayload(PatternDefinition definition) throws IOException {
        byte[] json = PatternJsonCodec.encode(definition).toString().getBytes(StandardCharsets.UTF_8);
        if (json.length > MAX_JSON_BYTES) throw new IOException("Pattern JSON payload exceeds binary limit");
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(json.length + Integer.BYTES);
                DataOutputStream output = new DataOutputStream(bytes)) {
            writeVarInt(output, json.length);
            output.write(json);
            output.flush();
            return bytes.toByteArray();
        }
    }

    private static PatternDefinition decodePayload(byte[] payload, Path file) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload))) {
            int length = readVarInt(input);
            if (length <= 0 || length > MAX_JSON_BYTES || length > input.available())
                throw new IOException("Invalid pattern JSON length in " + file);
            byte[] json = input.readNBytes(length);
            if (json.length != length || input.available() != 0)
                throw new IOException("Trailing pattern binary data in " + file);
            try {
                return PatternJsonCodec.decode(fileResource(file),
                        JsonParser.parseString(new String(json, StandardCharsets.UTF_8)));
            } catch (RuntimeException exception) {
                throw new IOException("Failed to decode pattern JSON payload in " + file, exception);
            }
        }
    }

    private static ResourceLocation fileResource(Path file) {
        String name = file.getFileName().toString().replace(".bin.zst", "");
        return ResourceLocation.parse("gtpm:" + name);
    }

    private static byte[] compress(byte[] payload) throws IOException {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(payload.length);
                ZstdOutputStream output = new ZstdOutputStream(bytes, ZSTD_LEVEL)) {
            output.write(payload);
            output.flush();
            return bytes.toByteArray();
        }
    }

    private static byte[] decompress(byte[] compressed, int expectedLength) throws IOException {
        try (ZstdInputStream input = new ZstdInputStream(new ByteArrayInputStream(compressed));
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(expectedLength)) {
            input.transferTo(bytes);
            byte[] payload = bytes.toByteArray();
            if (payload.length != expectedLength) throw new IOException("Pattern binary length mismatch");
            return payload;
        }
    }

    private static void writeFully(FileChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) channel.write(buffer);
    }

    private static int crc32(byte[] bytes) {
        CRC32 crc = new CRC32();
        crc.update(bytes);
        return (int) crc.getValue();
    }

    private static void writeVarInt(DataOutputStream output, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            output.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.writeByte(value);
    }

    private static int readVarInt(DataInputStream input) throws IOException {
        int result = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int next = input.readUnsignedByte();
            result |= (next & 0x7F) << shift;
            if ((next & 0x80) == 0) return result;
        }
        throw new IOException("Invalid pattern binary varint");
    }
}
