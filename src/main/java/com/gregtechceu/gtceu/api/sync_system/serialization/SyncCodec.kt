package com.gregtechceu.gtceu.api.sync_system.serialization

import com.gregtechceu.gtceu.api.sync_system.ClassSyncData
import com.gregtechceu.gtceu.api.sync_system.FieldSyncData

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps

import java.util.function.Supplier

/** Reads saved fields into a caller-created value; field bindings share the synchronization metadata. */
internal class SyncCodec<T : Any>(private val factory: Supplier<T>) : Codec<T> {
	override fun <D> decode(ops: DynamicOps<D>, input: D): DataResult<Pair<T, D>> = read(factory.get(), ops, input).map { Pair.of(it, ops.empty()) }

	override fun <D> encode(input: T, ops: DynamicOps<D>, prefix: D): DataResult<D> = write(input, ops, prefix)

	companion object {
		private val fields = object : ClassValue<List<Field>>() {
			override fun computeValue(type: Class<*>): List<Field> = ClassSyncData.getClassData(type).getWorldSaveFields().map(::Field)
		}

		fun serializeNBT(value: Any, registries: HolderLookup.Provider): CompoundTag {
			val ops = registries.createSerializationContext(NbtOps.INSTANCE)
			return write(value, ops, ops.emptyMap()).getOrThrow() as CompoundTag
		}

		fun deserializeNBT(value: Any, registries: HolderLookup.Provider, tag: CompoundTag) {
			read(value, registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow()
		}

		private fun <T : Any, D> read(value: T, ops: DynamicOps<D>, input: D): DataResult<T> = ops.getMap(input).flatMap { map ->
			try {
				for (field in fields.get(value.javaClass)) {
					val encoded = map.get(field.data.nbtSaveKey) ?: continue
					val decoded = field.codec.parse(ops, encoded).getOrThrow()
					val current = field.data.handle.get(value)
					if (decoded === current) continue
					if (current != null && CollectionCodecs.update(current, decoded)) continue
					field.data.handle.set(value, decoded)
				}
				DataResult.success(value)
			} catch (exception: RuntimeException) {
				DataResult.error { "Cannot decode ${value.javaClass.name}: ${exception.message}" }
			}
		}

		private fun <D> write(value: Any, ops: DynamicOps<D>, prefix: D): DataResult<D> {
			val builder = ops.mapBuilder()
			for (field in fields.get(value.javaClass)) {
				val current = field.data.handle.get(value) ?: continue
				builder.add(field.data.nbtSaveKey, field.codec.encodeStart(ops, current))
			}
			return builder.build(prefix)
		}
	}

	private class Field(val data: FieldSyncData) {
		@Suppress("UNCHECKED_CAST") // Field metadata binds the codec to its declared type during class initialization.
		val codec: Codec<Any> = requireNotNull(data.codec) { "Missing Codec for ${data.fieldName}: ${data.type}" } as Codec<Any>
	}
}
