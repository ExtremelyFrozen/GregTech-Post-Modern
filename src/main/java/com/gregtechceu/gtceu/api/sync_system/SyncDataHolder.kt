package com.gregtechceu.gtceu.api.sync_system

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged
import com.gregtechceu.gtceu.api.sync_system.serialization.CollectionCodecs

import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess

import com.mojang.serialization.Dynamic
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import org.jetbrains.annotations.Nullable

import java.lang.invoke.MethodHandle
import java.lang.invoke.WrongMethodTypeException

/**
 * Result of validating and transactionally applying one client-to-server field update batch.
 */
data class ServerFieldUpdateResult(val accepted: Boolean, val changed: Boolean, @field:Nullable val rejectionReason: String?) {
	companion object {
		@JvmStatic
		fun accepted(changed: Boolean): ServerFieldUpdateResult = ServerFieldUpdateResult(true, changed, null)

		@JvmStatic
		fun rejected(reason: String): ServerFieldUpdateResult = ServerFieldUpdateResult(false, false, reason)
	}
}

/**
 * Class that holds all sync info for an [ISyncManaged] object.
 */
class SyncDataHolder(private val holder: ISyncManaged) {
	private val syncData: ClassSyncData = ClassSyncData.getClassData(holder.javaClass)
	private val cachedClientValues: MutableMap<FieldSyncData, Any?> = Reference2ReferenceOpenHashMap()
	private val cachedClientData: MutableMap<FieldSyncData, Any?> = Reference2ReferenceOpenHashMap()
	private val cachedServerData: MutableMap<FieldSyncData, Any?> = Reference2ReferenceOpenHashMap()
	private val dirtySyncFields: ObjectSet<String> = ObjectOpenHashSet()
	private val authoritativeClientFields: ObjectSet<FieldSyncData> = ObjectOpenHashSet()
	private val rpcChildren: MutableList<ISyncManaged?> = ObjectArrayList()

	/** Returns the same ordered managed receivers used by LDLib2's root storage RPC lookup. */
	fun getManaged(): List<ISyncManaged> {
		val result = ObjectArrayList<ISyncManaged>()
		val seen = ReferenceOpenHashSet<ISyncManaged>()
		fun collect(value: ISyncManaged) {
			if (!seen.add(value)) return
			result.add(value)
			val storage = value.getSyncDataHolder()
			for (child in storage.rpcChildren) if (child != null) collect(child)
			for (field in storage.syncData.getManagedFields()) {
				val child = field.handle.get(value)
				if (child is ISyncManaged) collect(child)
			}
		}
		collect(holder)
		return result
	}

	/** Attaches a child in the same construction order on both sides. Call on the owning game thread. */
	fun attachRPC(child: ISyncManaged) {
		rpcChildren.add(child)
	}

	/** Detaches an object without changing the addresses of the remaining children. */
	fun detachRPC(child: ISyncManaged) {
		val index = rpcChildren.indexOfFirst { it === child }
		if (index >= 0) rpcChildren[index] = null
	}

	@field:Nullable
	private var pendingClientChanges: SyncFieldData? = null
	private var resyncAll = true

	init {
		for (field in syncData.getClientSyncFields()) {
			cachedClientValues[field] = field.handle.get(holder)
		}
	}

	/**
	 * Instructs the sync system that this field has been updated and must be synced with clients.
	 *
	 * @param fieldName The field that has changed.
	 */
	fun markClientSyncFieldDirty(fieldName: String) {
		dirtySyncFields.add(fieldName)
		holder.markAsChanged()
	}

	fun resyncAllFields() {
		resyncAll = true
		holder.markAsChanged()
	}

	/**
	 * Returns whether the next changed-only scan is the holder's initial authoritative sync.
	 * The value is read before scanning because the scan consumes the resync marker.
	 */
	fun isFullSyncPending(): Boolean = resyncAll

	fun serializeToItemFieldData(registries: HolderLookup.Provider): SyncFieldData {
		val builder = SyncFieldData.builder()
		for (field in syncData.getItemSaveFields()) {
			val value = FieldSyncHandler.serializeFieldData(
				registries,
				holder,
				field,
				writeClientFields = false,
				fullSync = false,
			)
			if (value != null) {
				builder.put(field.componentKey, value)
			}
		}
		return builder.build()
	}

	/** Reads menu data without consuming changes waiting for world tracking clients. */
	fun getClientData(registries: HolderLookup.Provider): SyncFieldData {
		val builder = SyncFieldData.builder()
		for (field in syncData.getClientSyncFields()) {
			builder.put(field.componentKey, FieldSyncHandler.serializeFieldData(registries, holder, field, true, fullSync = true, readOnly = true))
		}
		return builder.build()
	}

	fun serializeFullClientSyncData(registries: HolderLookup.Provider): SyncFieldData {
		val builder = SyncFieldData.builder()
		for (field in syncData.getClientSyncFields()) {
			val encoded = FieldSyncHandler.serializeFieldData(registries, holder, field, true, fullSync = true)
			builder.put(field.componentKey, encoded)
			cachedClientData[field] = encoded?.value
			val currentValue = field.handle.get(holder)
			cachedClientValues[field] = currentValue
			if (field.hasSyncBoth) {
				cachedServerData[field] = encoded?.value
			}
		}
		resyncAll = false
		dirtySyncFields.clear()
		pendingClientChanges = null
		return builder.build()
	}

	/**
	 * Serializes one client field for validation without consuming pending changes or updating synchronization caches.
	 */
	fun serializeClientField(registries: HolderLookup.Provider, fieldName: String): Dynamic<*>? {
		val field = syncData.getClientSyncFields().singleOrNull { candidate -> candidate.fieldName == fieldName }
			?: throw IllegalArgumentException("Unknown or ambiguous client sync field: $fieldName")
		return FieldSyncHandler.serializeFieldData(
			registries,
			holder,
			field,
			writeClientFields = true,
			fullSync = true,
		)
	}

	fun serializeToFieldData(registries: HolderLookup.Provider, writeClientFields: Boolean, fullSync: Boolean): SyncFieldData = if (writeClientFields) {
		if (fullSync) {
			serializeFullClientSyncData(registries)
		} else {
			if (pendingClientChanges == null) {
				scanAndMarkChanges(registries)
			}
			getPendingChanges()
		}
	} else {
		serializeToSaveFieldData(registries)
	}

	fun serializeToSaveFieldData(registries: HolderLookup.Provider): SyncFieldData {
		val builder = SyncFieldData.builder()
		for (field in syncData.getServerSaveFields()) {
			val value = FieldSyncHandler.serializeFieldData(
				registries,
				holder,
				field,
				writeClientFields = false,
				fullSync = false,
			)
			if (value != null) {
				builder.put(field.componentKey, value)
			}
		}
		return builder.build()
	}

	fun scanAndMarkChanges(registries: HolderLookup.Provider): Boolean {
		val changes = SyncFieldData.builder()
		var hasChanges = false
		val fullSync = resyncAll

		for (field in syncData.getClientSyncFields()) {
			val currentValue = field.handle.get(holder)
			val previousValue = cachedClientValues[field]
			val manuallyDirty = dirtySyncFields.contains(field.fieldName)
			val container = currentValue is Map<*, *> || currentValue is Collection<*> || field.handle.varType().isArray
			val immutable = field.contextualCodec?.isImmutable() == true
			if (!fullSync && !manuallyDirty && immutable && currentValue === previousValue) continue
			val encoded = if (field.contextualCodec != null && !immutable && !container) {
				if (!fullSync && !manuallyDirty && currentValue == previousValue &&
					!shouldSyncContextualField(registries, field, currentValue, fullSync, manuallyDirty)
				) {
					continue
				}
				FieldSyncHandler.serializeFieldData(registries, holder, field, true, fullSync)
			} else {
				val value = FieldSyncHandler.serializeFieldData(registries, holder, field, true, fullSync = true, readOnly = true)
				if (!fullSync && !manuallyDirty && value?.value == cachedClientData[field]) {
					cachedClientValues[field] = currentValue
					continue
				}
				value
			}
			changes.put(field.componentKey, encoded)
			cachedClientValues[field] = currentValue
			cachedClientData[field] = encoded?.value
			hasChanges = true
		}

		resyncAll = false
		dirtySyncFields.clear()
		if (hasChanges) {
			pendingClientChanges = changes.build()
			return true
		}
		return false
	}

	@Suppress("UNCHECKED_CAST")
	private fun shouldSyncContextualField(registries: HolderLookup.Provider, field: FieldSyncData, @Nullable currentValue: Any?, fullSync: Boolean, manuallyDirty: Boolean): Boolean {
		if (currentValue == null) {
			return false
		}
		if (field.contextualCodec == null) {
			return false
		}
		return (field.contextualCodec as ContextualFieldCodec<Any>).shouldSyncField(
			currentValue,
			ContextualFieldCodec.Context(
				holder,
				field.type,
				currentValue,
				field.fieldName,
				true,
				fullSync,
				registries,
			),
			fullSync,
			manuallyDirty,
		)
	}

	fun getPendingChanges(): SyncFieldData = (pendingClientChanges ?: SyncFieldData.EMPTY).also { pendingClientChanges = null }

	fun collectClientNetworkChanges(registries: RegistryAccess, force: Boolean): SyncFieldData {
		if (force) {
			pendingClientChanges = serializeFullClientSyncData(registries)
		}

		return getPendingChanges()
	}

	fun collectServerNetworkChanges(registries: RegistryAccess): SyncFieldData {
		val changes = SyncFieldData.builder()
		val fields = syncData.getOrderedServerUpdateFields()
		for (field in fields) {
			val encoded = FieldSyncHandler.encodeDetachedServerCandidate(registries, holder, field)
			if (encoded?.value == cachedServerData[field]) continue
			changes.put(field.componentKey, encoded)
			cachedServerData[field] = encoded?.value
		}

		return changes.build()
	}

	fun deserializeItemFieldData(registries: HolderLookup.Provider, fieldData: SyncFieldData) {
		for (field in syncData.getItemSaveFields()) {
			val savedValue = (fieldData[field.componentKey] ?: continue).orElse(null)
			FieldSyncHandler.deserializeFieldData(registries, holder, field, savedValue, false)
		}
	}

	@JvmOverloads
	fun deserializeFieldData(registries: HolderLookup.Provider, fieldData: SyncFieldData, readingClientFields: Boolean, parseExplicitNull: Boolean = false, fullSync: Boolean = false, notifyUnchangedOnFullSync: Boolean = true) {
		val fieldsToCheck = if (readingClientFields) syncData.getClientSyncFields() else syncData.getServerSaveFields()
		val changedClientFields = ObjectArrayList<FieldSyncData>()
		for (field in fieldsToCheck) {
			val savedValue = (fieldData[field.componentKey] ?: continue).orElse(null)
			val previousClientValue = if (readingClientFields) {
				copyClientValue(field.handle.get(holder))
			} else {
				null
			}
			FieldSyncHandler.deserializeFieldData(
				registries,
				holder,
				field,
				savedValue,
				readingClientFields,
				parseExplicitNull,
				fullSync = fullSync,
				notifyUnchangedOnFullSync = notifyUnchangedOnFullSync,
			)

			if (readingClientFields) {
				val currentValue = field.handle.get(holder)
				cachedClientValues[field] = currentValue
				cachedClientData[field] = savedValue?.value
				if (field.hasSyncBoth) {
					cachedServerData[field] = savedValue?.value
				}
				if (shouldNotifyClientField(
						field,
						previousClientValue,
						currentValue,
						fullSync,
						notifyUnchangedOnFullSync,
					)
				) {
					changedClientFields.add(field)
				}
			}
		}
		notifyClientFields(changedClientFields)
		if (readingClientFields && fullSync) {
			resyncAll = false
			dirtySyncFields.clear()
			pendingClientChanges = null
		}
	}

	fun applyServerNetworkUpdate(registries: RegistryAccess, changes: SyncFieldData) {
		val result = tryApplyServerNetworkUpdate(registries, changes)
		if (!result.accepted) {
			throw IllegalArgumentException(result.rejectionReason ?: "Sync: Server field update was rejected")
		}
	}

	/**
	 * Applies a client field-update batch without exposing partially decoded or normalized values to the holder.
	 */
	fun tryApplyServerNetworkUpdate(registries: RegistryAccess, changes: SyncFieldData): ServerFieldUpdateResult {
		if (changes.isEmpty()) return ServerFieldUpdateResult.accepted(false)

		val selectedFields = syncData.getOrderedServerUpdateFields().filter { field -> changes.fields.containsKey(field.componentKey) }
		if (selectedFields.size != changes.fields.size) {
			val selectedKeys = selectedFields.mapTo(ObjectOpenHashSet()) { field -> field.componentKey }
			val rejectedKeys = changes.fields.keys.filterNot(selectedKeys::contains)
			requestAuthoritativeServerAcks(selectedFields)
			return ServerFieldUpdateResult.rejected(
				"Sync: Server field update contains unknown or non-server-updatable fields: ${rejectedKeys.sortedBy { key -> key.toString() }}",
			)
		}
		val pendingUpdates = ObjectArrayList<PendingServerFieldUpdate>(selectedFields.size)
		try {
			// Decode and snapshot the complete batch before running any normalizer.
			for (field in selectedFields) {
				val encodedValue = changes[field.componentKey]?.orElse(null)
				val current = field.handle.get(holder)
				pendingUpdates.add(
					PendingServerFieldUpdate(
						field,
						current,
						CollectionCodecs.copyCurrent(field.type.rawType, current),
						FieldSyncHandler.decodeDetachedServerCandidate(registries, field, encodedValue),
					),
				)
			}

			// Normalize the complete decoded batch before committing any field.
			for (update in pendingUpdates) {
				update.candidate = normalizeServerCandidate(update)
			}

			commitServerUpdates(pendingUpdates)
		} catch (e: RuntimeException) {
			requestAuthoritativeServerAcks(selectedFields)
			return ServerFieldUpdateResult.rejected(
				e.message ?: "Sync: Server field update failed with ${e.javaClass.simpleName}",
			)
		}

		requestAuthoritativeServerAcks(selectedFields)
		var changed = false
		for (update in pendingUpdates) {
			if (update.oldValue != update.candidate) {
				changed = true
				invokeServerChangeListener(update)
			}
		}
		return ServerFieldUpdateResult.accepted(changed)
	}

	@JvmOverloads
	fun applyClientNetworkUpdate(registries: RegistryAccess, changes: SyncFieldData, fullSync: Boolean = false, notifyUnchangedOnFullSync: Boolean = true) {
		deserializeFieldData(registries, changes, readingClientFields = true, parseExplicitNull = true, fullSync, notifyUnchangedOnFullSync)
	}

	private fun copyClientValue(value: Any?): Any? = when (value) {
		is Map<*, *> -> value.entries.associateTo(LinkedHashMap(value.size)) { entry ->
			copyClientValue(entry.key) to copyClientValue(entry.value)
		}

		is Set<*> -> value.mapTo(LinkedHashSet(value.size), ::copyClientValue)

		is Collection<*> -> value.map(::copyClientValue)

		is Array<*> -> value.copyOf()

		is BooleanArray -> value.copyOf()

		is ByteArray -> value.copyOf()

		is CharArray -> value.copyOf()

		is ShortArray -> value.copyOf()

		is IntArray -> value.copyOf()

		is LongArray -> value.copyOf()

		is FloatArray -> value.copyOf()

		is DoubleArray -> value.copyOf()

		else -> value
	}

	private fun clientValuesEqual(previous: Any?, current: Any?): Boolean = when {
		previous is Array<*> && current is Array<*> -> previous.contentDeepEquals(current)
		previous is BooleanArray && current is BooleanArray -> previous.contentEquals(current)
		previous is ByteArray && current is ByteArray -> previous.contentEquals(current)
		previous is CharArray && current is CharArray -> previous.contentEquals(current)
		previous is ShortArray && current is ShortArray -> previous.contentEquals(current)
		previous is IntArray && current is IntArray -> previous.contentEquals(current)
		previous is LongArray && current is LongArray -> previous.contentEquals(current)
		previous is FloatArray && current is FloatArray -> previous.contentEquals(current)
		previous is DoubleArray && current is DoubleArray -> previous.contentEquals(current)
		else -> previous == current
	}

	private fun shouldNotifyClientField(field: FieldSyncData, previous: Any?, current: Any?, fullSync: Boolean, notifyUnchangedOnFullSync: Boolean): Boolean {
		val firstAuthoritativeSync = fullSync && authoritativeClientFields.add(field)
		return (firstAuthoritativeSync && notifyUnchangedOnFullSync) || !clientValuesEqual(previous, current)
	}

	private fun notifyClientFields(fields: Collection<FieldSyncData>) {
		for (field in fields) {
			invokeClientChangeListeners(field)
			if (field.triggerClientRerender) holder.scheduleRenderUpdate()
		}
	}

	private fun invokeClientChangeListeners(field: FieldSyncData) {
		try {
			for (changeListenerHandle: MethodHandle in field.changeListenerHandles) {
				changeListenerHandle.invoke(holder)
			}
		} catch (e: Throwable) {
			if (e is WrongMethodTypeException) {
				throw IllegalArgumentException("Invalid method signature for change listener for field ${field.fieldName} ${holder.javaClass.name}")
			}
			GTCEu.LOGGER.error("Sync: Error while invoking change listener for field {}", field.fieldName, e)
		}
	}

	private fun normalizeServerCandidate(update: PendingServerFieldUpdate): Any? {
		val normalizer = update.field.serverNormalizerHandle ?: return update.candidate
		return try {
			normalizer.invoke(holder, update.candidate)
		} catch (e: Throwable) {
			throw IllegalArgumentException(
				"Sync: Server normalizer rejected field ${update.field.fieldName} of type ${update.field.type}",
				e,
			)
		}
	}

	private fun commitServerUpdates(updates: List<PendingServerFieldUpdate>) {
		var committed = 0
		try {
			for (update in updates) {
				committed++
				FieldSyncHandler.applyValue(holder, update.field, update.candidate)
			}
		} catch (e: Throwable) {
			for (index in committed - 1 downTo 0) {
				val update = updates[index]
				try {
					if (update.oldReference != null && update.oldValue != null) {
						CollectionCodecs.update(update.oldReference, update.oldValue)
					}
					if (update.field.handle.get(holder) !== update.oldReference) update.field.handle.set(holder, update.oldReference)
				} catch (rollbackFailure: Throwable) {
					GTCEu.LOGGER.error(
						"Sync: Failed to roll back field {} of type {} after server batch commit failure",
						update.field.fieldName,
						update.field.type,
						rollbackFailure,
					)
				}
			}
			throw IllegalStateException("Sync: Failed to commit server field update batch", e)
		}
	}

	private fun requestAuthoritativeServerAcks(fields: Collection<FieldSyncData>) {
		var marked = false
		for (field in fields) {
			if (field.hasSyncBoth) {
				dirtySyncFields.add(field.fieldName)
				marked = true
			}
		}
		if (marked) {
			holder.markAsChanged()
		}
	}

	private fun invokeServerChangeListener(update: PendingServerFieldUpdate) {
		val listener = update.field.serverChangeListenerHandle ?: return
		try {
			listener.invoke(holder, update.oldValue, update.candidate)
		} catch (e: Throwable) {
			GTCEu.LOGGER.error(
				"Sync: Error while invoking server change listener for field {} of type {}",
				update.field.fieldName,
				update.field.type,
				e,
			)
		}
	}

	private data class PendingServerFieldUpdate(val field: FieldSyncData, @field:Nullable val oldReference: Any?, @field:Nullable val oldValue: Any?, @field:Nullable var candidate: Any?)
}
