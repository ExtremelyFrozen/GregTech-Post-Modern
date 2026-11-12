package com.gregtechceu.gtceu.api.multiblock.diagnosis.network;

import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisBasis;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisIssue;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisModule;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisReport;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisStatus;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.StructureDiagnosis;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/** Bounded, transient menu transport. The diagnosis model has no persistence or NBT dependency. */
public final class DiagnosisReportCodec {

    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final int MAX_METADATA = 4096;
    private static final int MAX_TEXT = 4096;

    public static final StreamCodec<FriendlyByteBuf, DiagnosisReport> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public DiagnosisReport decode(FriendlyByteBuf buffer) {
            DiagnosisStatus status = buffer.readEnum(DiagnosisStatus.class);
            String structure = buffer.readUtf(MAX_TEXT);
            int visited = buffer.readVarInt();
            int attempts = buffer.readVarInt();
            boolean incomplete = buffer.readBoolean();
            boolean truncated = buffer.readBoolean();
            DiagnosisBasis basis = null;
            if (buffer.readBoolean()) {
                String front = buffer.readUtf(MAX_TEXT);
                boolean flipped = buffer.readBoolean();
                int[] repetitions = new int[size(buffer, MAX_METADATA)];
                for (int i = 0; i < repetitions.length; i++) repetitions[i] = buffer.readVarInt();
                var choices = strings(buffer);
                var measurements = new Object2IntLinkedOpenHashMap<String>();
                int parameters = size(buffer, MAX_METADATA);
                for (int i = 0; i < parameters; i++) measurements.put(buffer.readUtf(MAX_TEXT), buffer.readVarInt());
                String source = buffer.readUtf(MAX_TEXT);
                var modules = new ObjectArrayList<DiagnosisModule>();
                int count = size(buffer, MAX_METADATA);
                for (int i = 0; i < count; i++) modules.add(new DiagnosisModule(buffer.readUtf(MAX_TEXT),
                        buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT)));
                basis = new DiagnosisBasis(front, flipped, repetitions, choices, measurements, source, modules);
            }
            var issues = new ObjectArrayList<DiagnosisIssue>();
            int count = size(buffer, StructureDiagnosis.MAX_GROUPS);
            int remaining = StructureDiagnosis.MAX_POSITIONS;
            for (int i = 0; i < count; i++) {
                String expected = buffer.readUtf(MAX_TEXT);
                String fact = buffer.readUtf(MAX_TEXT);
                int difference = buffer.readVarInt();
                String path = buffer.readUtf(MAX_TEXT);
                String repeatPath = buffer.readUtf(MAX_TEXT);
                int occurrences = buffer.readVarInt();
                long[] positions = new long[size(buffer, Math.min(remaining, StructureDiagnosis.MAX_ROW_POSITIONS))];
                for (int j = 0; j < positions.length; j++) positions[j] = buffer.readLong();
                remaining -= positions.length;
                issues.add(new DiagnosisIssue(expected, fact, difference, path, repeatPath, occurrences, positions,
                        buffer.readUtf(MAX_TEXT)));
            }
            return new DiagnosisReport(status, structure, visited, attempts, incomplete, truncated, basis, issues);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, DiagnosisReport value) {
            buffer.writeEnum(value.getStatus());
            buffer.writeUtf(value.getStructure(), MAX_TEXT);
            buffer.writeVarInt(value.getVisited());
            buffer.writeVarInt(value.getAttempts());
            buffer.writeBoolean(value.getIncomplete());
            buffer.writeBoolean(value.getTruncated());
            DiagnosisBasis basis = value.getBasis();
            buffer.writeBoolean(basis != null);
            if (basis != null) {
                buffer.writeUtf(basis.getFront(), MAX_TEXT);
                buffer.writeBoolean(basis.getFlipped());
                buffer.writeVarInt(basis.getRepetitions().length);
                for (int count : basis.getRepetitions()) buffer.writeVarInt(count);
                strings(buffer, basis.getChoices());
                buffer.writeVarInt(basis.getMeasurements().size());
                for (var entry : basis.getMeasurements().object2IntEntrySet()) {
                    buffer.writeUtf(entry.getKey(), MAX_TEXT);
                    buffer.writeVarInt(entry.getIntValue());
                }
                buffer.writeUtf(basis.getSource(), MAX_TEXT);
                buffer.writeVarInt(basis.getModules().size());
                for (DiagnosisModule module : basis.getModules()) {
                    buffer.writeUtf(module.getId(), MAX_TEXT);
                    buffer.writeUtf(module.getPath(), MAX_TEXT);
                    buffer.writeUtf(module.getKind(), MAX_TEXT);
                    buffer.writeUtf(module.getSelection(), MAX_TEXT);
                }
            }
            buffer.writeVarInt(value.getIssues().size());
            for (DiagnosisIssue issue : value.getIssues()) {
                buffer.writeUtf(issue.getExpected(), MAX_TEXT);
                buffer.writeUtf(issue.getFact(), MAX_TEXT);
                buffer.writeVarInt(issue.getDifference());
                buffer.writeUtf(issue.getPath(), MAX_TEXT);
                buffer.writeUtf(issue.getRepeatPath(), MAX_TEXT);
                buffer.writeVarInt(issue.getCount());
                buffer.writeVarInt(issue.getPositions().length);
                for (long position : issue.getPositions()) buffer.writeLong(position);
                buffer.writeUtf(issue.getActual(), MAX_TEXT);
            }
        }
    };

    private DiagnosisReportCodec() {}

    public static byte[] encode(DiagnosisReport report) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            STREAM_CODEC.encode(buffer, report);
            if (buffer.readableBytes() > MAX_BYTES)
                throw new IllegalArgumentException("Diagnosis report exceeds transport limit");
            byte[] result = new byte[buffer.readableBytes()];
            buffer.readBytes(result);
            return result;
        } finally {
            buffer.release();
        }
    }

    public static DiagnosisReport decode(byte[] encoded) {
        if (encoded.length > MAX_BYTES) throw new DecoderException("Diagnosis report exceeds transport limit");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(encoded));
        try {
            DiagnosisReport report = STREAM_CODEC.decode(buffer);
            if (buffer.isReadable()) throw new DecoderException("Trailing diagnosis data");
            return report;
        } finally {
            buffer.release();
        }
    }

    private static int size(FriendlyByteBuf buffer, int maximum) {
        int size = buffer.readVarInt();
        if (size < 0 || size > maximum) throw new DecoderException("Diagnosis collection exceeds " + maximum);
        return size;
    }

    private static Object2ObjectLinkedOpenHashMap<String, String> strings(FriendlyByteBuf buffer) {
        var values = new Object2ObjectLinkedOpenHashMap<String, String>();
        int size = size(buffer, MAX_METADATA);
        for (int i = 0; i < size; i++) values.put(buffer.readUtf(MAX_TEXT), buffer.readUtf(MAX_TEXT));
        return values;
    }

    private static void strings(FriendlyByteBuf buffer, Object2ObjectMap<String, String> values) {
        buffer.writeVarInt(values.size());
        values.forEach((key, value) -> {
            buffer.writeUtf(key, MAX_TEXT);
            buffer.writeUtf(value, MAX_TEXT);
        });
    }
}
