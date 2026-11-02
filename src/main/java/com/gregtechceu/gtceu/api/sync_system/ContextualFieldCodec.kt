package com.gregtechceu.gtceu.api.sync_system

import net.minecraft.core.HolderLookup

import com.google.gson.JsonElement
interface ContextualFieldCodec<T> {
	fun serializeField(value: T, context: Context<T>): JsonElement

	fun deserializeField(value: JsonElement, context: Context<T>): T?

	fun shouldSyncField(value: T, context: Context<T>, fullSync: Boolean, manuallyDirty: Boolean): Boolean = fullSync || manuallyDirty

	@JvmRecord
	data class Context<T>(
		val holder: Any,
		val type: TypeDeclaration,
		val currentValue: T?,
		val fieldName: String,
		val isClientSync: Boolean,
		val isClientFullSyncUpdate: Boolean,
		val lookup: HolderLookup.Provider,
		val parseExplicitNull: Boolean = false,
		val notifyUnchangedOnFullSync: Boolean = true,
	)
}
