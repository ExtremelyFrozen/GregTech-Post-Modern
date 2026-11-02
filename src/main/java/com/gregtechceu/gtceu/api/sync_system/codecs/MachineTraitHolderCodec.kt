package com.gregtechceu.gtceu.api.sync_system.codecs

import com.gregtechceu.gtceu.api.machine.trait.MachineTraitHolder
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec

import com.gregtechceu.gtceu.api.sync_system.SyncFieldData

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps

class MachineTraitHolderCodec private constructor() : ContextualFieldCodec<MachineTraitHolder> {
	override fun serializeField(value: MachineTraitHolder, context: ContextualFieldCodec.Context<MachineTraitHolder>): JsonElement = SyncFieldData.CODEC
		.encodeStart(
			context.lookup.createSerializationContext(JsonOps.INSTANCE),
			value.serializeSyncFieldData(context.lookup, context.isClientSync, context.isClientFullSyncUpdate),
		)
		.getOrThrow()

	override fun shouldSyncField(value: MachineTraitHolder, context: ContextualFieldCodec.Context<MachineTraitHolder>, fullSync: Boolean, manuallyDirty: Boolean): Boolean {
		if (!context.isClientSync) return fullSync || manuallyDirty
		return value.scanAndMarkClientChanges(context.lookup, fullSync || manuallyDirty)
	}

	override fun deserializeField(value: JsonElement, context: ContextualFieldCodec.Context<MachineTraitHolder>): MachineTraitHolder {
		val holder = requireNotNull(context.currentValue)
		val components = SyncFieldData.CODEC
			.parse(context.lookup.createSerializationContext(JsonOps.INSTANCE), value)
			.getOrThrow()
		holder.deserializeSyncFieldData(
			context.lookup,
			components,
			context.isClientSync,
			context.parseExplicitNull,
			context.isClientFullSyncUpdate,
			false,
		)
		return holder
	}

	companion object {
		@JvmField
		val TYPE: Class<MachineTraitHolder> = MachineTraitHolder::class.java

		@JvmField
		val INSTANCE: MachineTraitHolderCodec = MachineTraitHolderCodec()
	}
}
