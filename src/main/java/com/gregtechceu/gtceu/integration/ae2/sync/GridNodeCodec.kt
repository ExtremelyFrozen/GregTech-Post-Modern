package com.gregtechceu.gtceu.integration.ae2.sync

import net.minecraft.nbt.CompoundTag

import appeng.api.networking.IManagedGridNode
import com.mojang.serialization.Codec

/** AE2 exposes live node persistence through NBT; keep that boundary inside the integration. */
object GridNodeCodec {
	@JvmStatic
	fun <T : IManagedGridNode> of(node: T): Codec<T> = CompoundTag.CODEC.xmap(
		{ tag -> node.also { it.loadFromNBT(tag) } },
		{ value -> CompoundTag().also(value::saveToNBT) },
	)
}
