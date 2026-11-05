package com.gregtechceu.gtceu.api.gui.slot;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Assigns an immutable slot-id range to one lifecycle of a logical target.
 *
 * <p>
 * A tombstone keeps its range reserved after removal so later bindings never reuse ids still known by a client.
 * </p>
 *
 * @param bindingId         globally unique identity of this target lifecycle
 * @param targetId          stable business identity of the logical target
 * @param targetIncarnation identity of the target's logical lifecycle
 * @param firstSlotId       first menu slot id reserved for this lifecycle
 * @param slotCount         number of consecutive slots reserved for this lifecycle
 * @param present           whether the lifecycle is active; {@code false} denotes a tombstone
 */
public record DynamicItemSlotBinding(UUID bindingId, UUID targetId, UUID targetIncarnation,
                                     int firstSlotId, int slotCount, boolean present) {

    /**
     * Network codec for a lifecycle binding.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, DynamicItemSlotBinding> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public @NotNull DynamicItemSlotBinding decode(RegistryFriendlyByteBuf buffer) {
            return new DynamicItemSlotBinding(
                    buffer.readUUID(),
                    buffer.readUUID(),
                    buffer.readUUID(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, DynamicItemSlotBinding value) {
            buffer.writeUUID(value.bindingId());
            buffer.writeUUID(value.targetId());
            buffer.writeUUID(value.targetIncarnation());
            buffer.writeVarInt(value.firstSlotId());
            buffer.writeVarInt(value.slotCount());
            buffer.writeBoolean(value.present());
        }
    };

    /**
     * Returns whether this reserved range is a removed lifecycle tombstone.
     */
    public boolean tombstone() {
        return !present;
    }
}
