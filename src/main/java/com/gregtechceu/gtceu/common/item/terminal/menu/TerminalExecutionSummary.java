package com.gregtechceu.gtceu.common.item.terminal.menu;

import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchStatus;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;

/** Projection of the last batch result shown by a terminal menu. */
public record TerminalExecutionSummary(BatchStatus status, int placed, int removed,
                                       ObjectList<Structure> structures) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalExecutionSummary> STREAM_CODEC = StreamCodec
            .of(TerminalExecutionSummary::encode, TerminalExecutionSummary::decode);

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
        buffer.writeVarInt(value.structures.size());
        for (Structure structure : value.structures) {
            structure.encode(buffer);
        }
    }

    private static TerminalExecutionSummary decode(RegistryFriendlyByteBuf buffer) {
        BatchStatus status = buffer.readEnum(BatchStatus.class);
        int placed = buffer.readVarInt();
        int removed = buffer.readVarInt();
        int size = buffer.readVarInt();
        ObjectList<Structure> structures = new ObjectArrayList<>(size);
        for (int index = 0; index < size; index++) {
            structures.add(Structure.decode(buffer));
        }
        return new TerminalExecutionSummary(status, placed, removed, structures);
    }

    /** One structure's compact result and stable problem codes. */
    public record Structure(String name, StructureOperation mode, boolean success, int placed, int removed,
                            ObjectList<String> problems) {

        private void encode(RegistryFriendlyByteBuf buffer) {
            buffer.writeUtf(name);
            buffer.writeEnum(mode);
            buffer.writeBoolean(success);
            buffer.writeVarInt(placed);
            buffer.writeVarInt(removed);
            buffer.writeVarInt(problems.size());
            problems.forEach(buffer::writeUtf);
        }

        private static Structure decode(RegistryFriendlyByteBuf buffer) {
            String name = buffer.readUtf();
            StructureOperation mode = buffer.readEnum(StructureOperation.class);
            boolean success = buffer.readBoolean();
            int placed = buffer.readVarInt();
            int removed = buffer.readVarInt();
            int size = buffer.readVarInt();
            ObjectList<String> problems = new ObjectArrayList<>(size);
            for (int index = 0; index < size; index++) {
                problems.add(buffer.readUtf());
            }
            return new Structure(name, mode, success, placed, removed, problems);
        }
    }
}
