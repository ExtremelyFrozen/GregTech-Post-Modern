package com.gregtechceu.gtceu.api.sync_system.serialization

import net.minecraft.core.RegistryAccess
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.neoforged.neoforge.network.connection.ConnectionType

import com.mojang.serialization.Codec
import io.netty.buffer.Unpooled

import java.nio.ByteBuffer

/** Reuses a transient value's registry-aware wire format inside annotated menu fields. */
internal object StreamFieldCodec {
	fun <T : Any> create(streamCodec: StreamCodec<in RegistryFriendlyByteBuf, T>, registries: RegistryAccess): Codec<T> = Codec.BYTE_BUFFER.xmap(
		{ encoded ->
			val buffer = RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(encoded.duplicate()), registries, ConnectionType.NEOFORGE)
			try {
				streamCodec.decode(buffer)
			} finally {
				buffer.release()
			}
		},
		{ value ->
			val buffer = RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE)
			try {
				streamCodec.encode(buffer, value)
				ByteBuffer.wrap(ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) })
			} finally {
				buffer.release()
			}
		},
	)
}
