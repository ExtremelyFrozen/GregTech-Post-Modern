package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchStatus;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;

/** Bounded, client-safe projection of the last batch result shown by a terminal menu. */
public record TerminalExecutionSummary(BatchStatus status, int placed, int removed,
                                       ObjectList<Structure> structures) {

    private static final int MAX_STRUCTURES = 128;
    private static final int MAX_PROBLEMS = 64;
    private static final int MAX_TEXT = 128;

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalExecutionSummary> STREAM_CODEC = StreamCodec
            .of(TerminalExecutionSummary::encode, TerminalExecutionSummary::decode);

    public TerminalExecutionSummary {
        if (placed < 0 || removed < 0 || structures.size() > MAX_STRUCTURES) {
            throw new IllegalArgumentException("Terminal execution summary values are invalid or unbounded");
        }
        structures = new ObjectImmutableList<>(structures);
    }

    public static TerminalExecutionSummary from(BatchResult result) {
        return new TerminalExecutionSummary(result.status(), result.placed(), result.removed(),
                result.structures().stream().map(structure -> new Structure(
                        structure.structureName(), structure.mode(), structure.success(), structure.placed(),
                        structure.removed(), structure.problems().stream().map(problem -> problem.type().name())
                                .collect(ObjectImmutableList.toList())))
                        .collect(ObjectImmutableList.toList()));
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
        BatchStatus status = buffer.readEnum(BatchStatus.class);
        int placed = readNonNegative(buffer, "placed");
        int removed = readNonNegative(buffer, "removed");
        int size = readSize(buffer, MAX_STRUCTURES, "execution structures");
        ObjectList<Structure> structures = new ObjectArrayList<>(size);
        for (int index = 0; index < size; index++) {
            structures.add(Structure.decode(buffer));
        }
        return new TerminalExecutionSummary(status, placed, removed, structures);
    }

    /** One structure's compact result and stable problem codes. */
    public record Structure(String name, StructureOperation mode, boolean success, int placed, int removed,
                            ObjectList<String> problems) {

        public Structure {
            if (name.isBlank() || name.length() > MAX_TEXT || placed < 0 ||
                    removed < 0 || problems.size() > MAX_PROBLEMS || problems.stream()
                            .anyMatch(problem -> problem.isBlank() || problem.length() > MAX_TEXT)) {
                throw new IllegalArgumentException("Terminal structure result is invalid or unbounded");
            }
            problems = new ObjectImmutableList<>(problems);
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
            StructureOperation mode = buffer.readEnum(StructureOperation.class);
            boolean success = buffer.readBoolean();
            int placed = readNonNegative(buffer, "placed");
            int removed = readNonNegative(buffer, "removed");
            int size = readSize(buffer, MAX_PROBLEMS, "execution problems");
            ObjectList<String> problems = new ObjectArrayList<>(size);
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
