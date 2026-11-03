package com.gregtechceu.gtceu.api.sync_system

import com.gregtechceu.gtceu.api.sync_system.serialization.CollectionCodecs

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.NbtOps

import com.mojang.serialization.Codec
import com.mojang.serialization.Dynamic
import org.jetbrains.annotations.ApiStatus

import java.lang.invoke.VarHandle

@ApiStatus.Internal
object FieldSyncHandler {
	@JvmStatic
	fun serializeFieldData(registries: HolderLookup.Provider, holder: Any, field: FieldSyncData, writeClientFields: Boolean, fullSync: Boolean, readOnly: Boolean = false): Dynamic<*>? {
		val value = field.handle.get(holder) ?: return null
		val ops = registries.createSerializationContext(NbtOps.INSTANCE)
		return Dynamic(ops, codec(registries, holder, field, value, writeClientFields, fullSync, readOnly = readOnly).encodeStart(ops, value).getOrThrow())
	}

	@JvmStatic
	fun decodeDetachedServerCandidate(registries: HolderLookup.Provider, holder: Any, field: FieldSyncData, savedValue: Dynamic<*>?): Any? = decode(registries, ordinaryCodec(field), savedValue)

	@JvmStatic
	fun encodeDetachedServerCandidate(registries: HolderLookup.Provider, holder: Any, field: FieldSyncData): Dynamic<*>? {
		val value = field.handle.get(holder) ?: return null
		val ops = registries.createSerializationContext(NbtOps.INSTANCE)
		return Dynamic(ops, ordinaryCodec(field).encodeStart(ops, value).getOrThrow())
	}

	@JvmOverloads
	@JvmStatic
	fun deserializeFieldData(registries: HolderLookup.Provider, holder: Any, field: FieldSyncData, savedValue: Dynamic<*>?, readingClientFields: Boolean, parseExplicitNull: Boolean = false, fullSync: Boolean = false, notifyUnchangedOnFullSync: Boolean = true) {
		val current = field.handle.get(holder)
		val result = if (savedValue == null) {
			null
		} else {
			decode(
				registries,
				codec(registries, holder, field, current, readingClientFields, fullSync, parseExplicitNull, notifyUnchangedOnFullSync),
				savedValue,
			)
		}
		if (result === current) return
		if (current != null && result != null && CollectionCodecs.update(current, result)) return
		if (!field.handle.isAccessModeSupported(VarHandle.AccessMode.SET)) {
			throw IllegalStateException("Sync: cannot replace final field ${field.fieldName}")
		}
		field.handle.set(holder, result)
	}

	@Suppress("UNCHECKED_CAST")
	private fun codec(registries: HolderLookup.Provider, holder: Any, field: FieldSyncData, current: Any?, clientSync: Boolean, fullSync: Boolean, parseExplicitNull: Boolean = false, notifyUnchangedOnFullSync: Boolean = true, readOnly: Boolean = false): Codec<Any> {
		val contextual = field.contextualCodec as ContextualFieldCodec<Any>?
		return contextual?.codec(ContextualFieldCodec.Context(holder, field.type, current, field.fieldName, clientSync, fullSync, registries, parseExplicitNull, notifyUnchangedOnFullSync, readOnly))
			?: ordinaryCodec(field)
	}

	@Suppress("UNCHECKED_CAST")
	private fun ordinaryCodec(field: FieldSyncData): Codec<Any> = field.codec as Codec<Any>? ?: throw IllegalArgumentException("Sync: missing Codec for ${field.fieldName}: ${field.type}")

	private fun <T> decode(registries: HolderLookup.Provider, codec: Codec<T>, value: Dynamic<*>?): T? {
		val ops = registries.createSerializationContext(NbtOps.INSTANCE)
		return value?.convert(ops)?.let { codec.parse(ops, it.value).getOrThrow() }
	}
}
