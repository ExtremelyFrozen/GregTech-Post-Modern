package com.gregtechceu.gtceu.common.item.datacomponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import org.jspecify.annotations.NullMarked;

/** Persistent controller identity; binding never loads a chunk or performs a build. */
@NullMarked
public record TerminalControllerBinding(BlockPos position, ResourceKey<Level> dimension,
                                        ResourceLocation definitionId) {

    public static final Codec<TerminalControllerBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("position").forGetter(TerminalControllerBinding::position),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(TerminalControllerBinding::dimension),
            ResourceLocation.CODEC.fieldOf("definition").forGetter(TerminalControllerBinding::definitionId))
            .apply(instance, TerminalControllerBinding::new));

    public static final StreamCodec<ByteBuf, TerminalControllerBinding> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerminalControllerBinding::position,
            ResourceKey.streamCodec(Registries.DIMENSION), TerminalControllerBinding::dimension,
            ResourceLocation.STREAM_CODEC, TerminalControllerBinding::definitionId,
            TerminalControllerBinding::new);

    public TerminalControllerBinding {
        position = position.immutable();
    }
}
