package com.gregtechceu.gtceu.api.sync_system

import com.gregtechceu.gtceu.GTCEu

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation

import com.mojang.serialization.Codec
import com.mojang.serialization.Dynamic
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap

import java.util.Optional

import java.util.Map.copyOf as immutableCopyOf

/**
 * Field data shared by persistence and network synchronization.
 *
 * Missing keys mean "unchanged"; an empty value means the field was explicitly set to `null`.
 */
class SyncFieldData(fields: Map<ResourceLocation, @JvmSuppressWildcards Optional<Dynamic<*>>>) {
	@get:JvmName("fields")
	val fields: Map<ResourceLocation, Optional<Dynamic<*>>> = immutableCopyOf(fields)

	fun isEmpty(): Boolean = fields.isEmpty()

	operator fun contains(key: ResourceLocation): Boolean = fields.containsKey(key)

	operator fun get(key: ResourceLocation): Optional<Dynamic<*>>? = fields[key]

	fun <T> read(key: ResourceLocation, codec: Codec<T>): T? {
		val value = fields[key]?.orElse(null) ?: return null
		return codec.parse(value).getOrThrow()
	}

	fun <T> read(key: ResourceLocation, codec: Codec<T>, registries: HolderLookup.Provider): T? {
		val value = fields[key]?.orElse(null) ?: return null
		val ops = registries.createSerializationContext(NbtOps.INSTANCE)
		return codec.parse(ops, value.convert(ops).value).getOrThrow()
	}

	override fun equals(other: Any?): Boolean = this === other || (other is SyncFieldData && fields == other.fields)

	override fun hashCode(): Int = fields.hashCode()

	override fun toString(): String = "SyncFieldData[fields=$fields]"

	class Builder {
		private val fields: MutableMap<ResourceLocation, Optional<Dynamic<*>>> = Object2ObjectLinkedOpenHashMap()

		fun putAll(data: SyncFieldData): Builder = apply { fields.putAll(data.fields) }

		fun remove(key: ResourceLocation): Builder = apply { fields.remove(key) }

		fun put(key: ResourceLocation, value: Dynamic<*>?): Builder {
			fields[key] = Optional.ofNullable(value)
			return this
		}

		fun put(key: ResourceLocation, value: String): Builder = put(key, Codec.STRING, value)

		fun put(key: ResourceLocation, value: Boolean): Builder = put(key, Codec.BOOL, value)

		fun put(key: ResourceLocation, value: Int): Builder = put(key, Codec.INT, value)

		fun put(key: ResourceLocation, value: Long): Builder = put(key, Codec.LONG, value)

		fun put(key: ResourceLocation, value: SyncFieldData): Builder = put(key, CODEC, value)

		fun <T> put(key: ResourceLocation, codec: Codec<T>, value: T): Builder = put(
			key,
			Dynamic(NbtOps.INSTANCE, codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow()),
		)

		fun <T> put(key: ResourceLocation, codec: Codec<T>, value: T, registries: HolderLookup.Provider): Builder {
			val ops = registries.createSerializationContext(NbtOps.INSTANCE)
			return put(key, Dynamic(ops, codec.encodeStart(ops, value).getOrThrow()))
		}

		fun build(): SyncFieldData = if (fields.isEmpty()) EMPTY else SyncFieldData(fields)
	}

	companion object {
		@JvmField
		val EMPTY = SyncFieldData(emptyMap())

		@JvmField
		val CODEC: Codec<SyncFieldData> = Codec.unboundedMap(ResourceLocation.CODEC, Codec.PASSTHROUGH.optionalFieldOf("value").codec())
			.xmap(::SyncFieldData) { it.fields }

		@JvmField
		val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SyncFieldData> = ByteBufCodecs.fromCodecWithRegistries(CODEC)

		@JvmStatic
		fun builder(): Builder = Builder()

		@JvmStatic
		fun key(key: String): ResourceLocation = if (key.indexOf(':') >= 0) {
			ResourceLocation.parse(key)
		} else {
			GTCEu.id(escapePath(key))
		}

		private fun escapePath(key: String): String {
			val path = StringBuilder(key.length)
			for (character in key) {
				if (
					character in 'a'..'z' || character in '0'..'9' ||
					character == '_' || character == '-' || character == '.' || character == '/'
				) {
					path.append(character)
				} else {
					path.append("_u")
					val hex = character.code.toString(16)
					repeat(4 - hex.length) {
						path.append('0')
					}
					path.append(hex)
				}
			}
			return path.toString()
		}
	}
}
