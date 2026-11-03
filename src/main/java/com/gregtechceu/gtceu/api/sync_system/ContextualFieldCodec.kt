package com.gregtechceu.gtceu.api.sync_system

import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged

import net.minecraft.core.HolderLookup

import com.mojang.serialization.Codec

/**
 * Supplies a value Codec for a field that must retain its owning object or existing runtime instance.
 * The context is scoped to a single save or synchronization operation on the owning game thread.
 * Codec composition controls the data shape; this interface never selects a storage format.
 */
interface ContextualFieldCodec<T> {
	/** Decoding may update the existing instance in [Context.currentValue] after the payload has decoded. */
	fun codec(context: Context<T>): Codec<T>

	fun shouldSyncField(value: T, context: Context<T>, fullSync: Boolean, manuallyDirty: Boolean): Boolean {
		if (!context.isClientSync || value !is ISyncManaged) return fullSync || manuallyDirty
		if (fullSync || manuallyDirty) {
			value.getSyncDataHolder().resyncAllFields()
			return true
		}
		return value.getSyncDataHolder().scanAndMarkChanges(context.lookup)
	}

	@JvmRecord
	data class Context<T>(val holder: Any, val type: TypeDeclaration, val currentValue: T?, val fieldName: String, val isClientSync: Boolean, val isClientFullSyncUpdate: Boolean, val lookup: HolderLookup.Provider, val parseExplicitNull: Boolean = false, val notifyUnchangedOnFullSync: Boolean = true)
}
