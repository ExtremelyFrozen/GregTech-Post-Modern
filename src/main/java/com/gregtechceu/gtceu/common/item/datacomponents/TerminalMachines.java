package com.gregtechceu.gtceu.common.item.datacomponents;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import org.jspecify.annotations.NullMarked;

/** Stores independent automatic-build machines by multiblock machine definition. */
@NullMarked
public record TerminalMachines(Object2ObjectMap<ResourceLocation, TerminalMachine> machines) {

    public static final TerminalMachines EMPTY = new TerminalMachines(Object2ObjectMaps.emptyMap());

    public static final Codec<TerminalMachines> CODEC = RecordCodecBuilder
            .<TerminalMachines>create(instance -> instance
                    .group(
                            Codec.unboundedMap(ResourceLocation.CODEC, TerminalMachine.CODEC)
                                    .<Object2ObjectMap<ResourceLocation, TerminalMachine>>xmap(
                                            Object2ObjectLinkedOpenHashMap::new, value -> value)
                                    .fieldOf("machines")
                                    .forGetter(TerminalMachines::machines))
                    .apply(instance, TerminalMachines::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalMachines> STREAM_CODEC = StreamCodec
            .of(TerminalMachines::encode, TerminalMachines::decode);

    public TerminalMachine getOrCreate(MultiblockMachineDefinition definition) {
        TerminalMachine machine = machines.get(definition.getId());
        return machine == null ? TerminalMachine.defaults(definition) : machine;
    }

    public TerminalMachines withMachine(ResourceLocation definitionId, TerminalMachine machine) {
        Object2ObjectMap<ResourceLocation, TerminalMachine> updated = new Object2ObjectLinkedOpenHashMap<>(
                machines);
        updated.put(definitionId, machine);
        return new TerminalMachines(updated);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalMachines value) {
        buffer.writeVarInt(value.machines.size());
        value.machines.forEach((definitionId, machine) -> {
            buffer.writeResourceLocation(definitionId);
            TerminalMachine.STREAM_CODEC.encode(buffer, machine);
        });
    }

    private static TerminalMachines decode(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Object2ObjectMap<ResourceLocation, TerminalMachine> machines = new Object2ObjectLinkedOpenHashMap<>();
        for (int index = 0; index < size; index++) {
            ResourceLocation id = buffer.readResourceLocation();
            machines.put(id, TerminalMachine.STREAM_CODEC.decode(buffer));
        }
        return new TerminalMachines(machines);
    }
}
