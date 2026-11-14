package com.gregtechceu.gtceu.api.gui.slot;

import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Describes the slot capacity requested by one logical target before a menu assigns stable slot ids.
 *
 * @param targetId          stable business identity of the target that owns the slots
 * @param targetIncarnation identity of the target's current logical lifecycle
 * @param slotCount         number of slots requested by the target
 */
public record DynamicItemSlotDefinition(UUID targetId, UUID targetIncarnation, int slotCount) {

    /**
     * Network codec for a target slot definition.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, DynamicItemSlotDefinition> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public @NotNull DynamicItemSlotDefinition decode(RegistryFriendlyByteBuf buffer) {
            return new DynamicItemSlotDefinition(buffer.readUUID(), buffer.readUUID(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, DynamicItemSlotDefinition value) {
            buffer.writeUUID(value.targetId());
            buffer.writeUUID(value.targetIncarnation());
            buffer.writeVarInt(value.slotCount());
        }
    };

    static {
        FieldCodecs.registerStream(DynamicItemSlotDefinition.class, STREAM_CODEC);
    }
}
