package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchStatus;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Bounded, client-safe projection of the last batch result shown by a terminal menu. */
public record TerminalExecutionSummary(AutoBuildBatchStatus status, int placed, int removed,
                                       List<Structure> structures) {

    private static final int MAX_STRUCTURES = 128;
    private static final int MAX_PROBLEMS = 64;
    private static final int MAX_TEXT = 128;

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalExecutionSummary> STREAM_CODEC = StreamCodec
            .of(TerminalExecutionSummary::encode, TerminalExecutionSummary::decode);

    public TerminalExecutionSummary {
        if (placed < 0 || removed < 0 || structures.size() > MAX_STRUCTURES) {
            throw new IllegalArgumentException("Terminal execution summary values are invalid or unbounded");
        }
        structures = List.copyOf(structures);
    }

    public static TerminalExecutionSummary from(AutoBuildBatchResult result) {
        return new TerminalExecutionSummary(result.status(), result.placed(), result.removed(),
                result.structures().stream().map(structure -> new Structure(
                        structure.structureName(), structure.mode(), structure.success(), structure.placed(),
                        structure.removed(), structure.problems().stream().map(problem -> problem.type().name())
                                .toList()))
                        .toList());
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalExecutionSummary value) {
        buffer.writeEnum(value.status);
        buffer.writeVarInt(value.placed);
        buffer.writeVarInt(value.removed);
        writeSize(buffer, value.structures.size(), MAX_STRUCTURES, "execution structures");
        for (Structure structure : value.structures) {
            structure.encode(buffer);
        }
    }

    private static TerminalExecutionSummary decode(RegistryFriendlyByteBuf buffer) {
        AutoBuildBatchStatus status = buffer.readEnum(AutoBuildBatchStatus.class);
        int placed = readNonNegative(buffer, "placed");
        int removed = readNonNegative(buffer, "removed");
        int size = readSize(buffer, MAX_STRUCTURES, "execution structures");
        List<Structure> structures = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            structures.add(Structure.decode(buffer));
        }
        return new TerminalExecutionSummary(status, placed, removed, structures);
    }

    /** One structure's compact result and stable problem codes. */
    public record Structure(String name, AutoBuildMode mode, boolean success, int placed, int removed,
                            List<String> problems) {

        public Structure {
            if (name.isBlank() || name.length() > MAX_TEXT || placed < 0 ||
                    removed < 0 || problems.size() > MAX_PROBLEMS || problems.stream()
                            .anyMatch(problem -> problem.isBlank() || problem.length() > MAX_TEXT)) {
                throw new IllegalArgumentException("Terminal structure result is invalid or unbounded");
            }
            problems = List.copyOf(problems);
        }

        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(name, MAX_TEXT);
            buffer.writeEnum(mode);
            buffer.writeBoolean(success);
            buffer.writeVarInt(placed);
            buffer.writeVarInt(removed);
            writeSize(buffer, problems.size(), MAX_PROBLEMS, "execution problems");
            problems.forEach(problem -> buffer.writeUtf(problem, MAX_TEXT));
        }

        private static Structure decode(RegistryFriendlyByteBuf buffer) {
            String name = buffer.readUtf(MAX_TEXT);
            AutoBuildMode mode = buffer.readEnum(AutoBuildMode.class);
            boolean success = buffer.readBoolean();
            int placed = readNonNegative(buffer, "placed");
            int removed = readNonNegative(buffer, "removed");
            int size = readSize(buffer, MAX_PROBLEMS, "execution problems");
            List<String> problems = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                problems.add(buffer.readUtf(MAX_TEXT));
            }
            return new Structure(name, mode, success, placed, removed, problems);
        }
    }

    private static void writeSize(RegistryFriendlyByteBuf buffer, int value, int maximum, String name) {
        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException(name + " exceeds " + maximum);
        }
        buffer.writeVarInt(value);
    }

    private static int readSize(RegistryFriendlyByteBuf buffer, int maximum, String name) {
        int value = buffer.readVarInt();
        if (value < 0 || value > maximum) {
            throw new IllegalArgumentException(name + " exceeds " + maximum);
        }
        return value;
    }

    private static int readNonNegative(RegistryFriendlyByteBuf buffer, String name) {
        int value = buffer.readVarInt();
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        return value;
    }
}
