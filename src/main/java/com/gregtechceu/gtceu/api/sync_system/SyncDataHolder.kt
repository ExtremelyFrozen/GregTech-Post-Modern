package com.gregtechceu.gtceu.api.sync_system

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged

import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistryAccess

import com.mojang.serialization.Dynamic
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSet
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap
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
	private val cachedServerValues: MutableMap<FieldSyncData, Any?> = Reference2ReferenceOpenHashMap()
	private val dirtySyncFields: ObjectSet<String> = ObjectOpenHashSet()
	private val authoritativeClientFields: ObjectSet<FieldSyncData> = ObjectOpenHashSet()

	@field:Nullable
	private var pendingClientChanges: SyncFieldData? = null
	private var resyncAll = true

	init {
		for (field in syncData.getClientSyncFields()) {
			cachedClientValues[field] = field.handle.get(holder)
		}
		for (field in syncData.getServerUpdateFields()) {
			cachedServerValues[field] = field.handle.get(holder)
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
				builder.put(itemFieldKey(field), value)
			}
		}
		return builder.build()
	}

	fun serializeFullClientSyncData(registries: HolderLookup.Provider): SyncFieldData {
		val builder = SyncFieldData.builder()
		for (field in syncData.getClientSyncFields()) {
			builder.put(field.componentKey, FieldSyncHandler.serializeFieldData(registries, holder, field, true, fullSync = true))
			val currentValue = field.handle.get(holder)
			cachedClientValues[field] = currentValue
			if (field.hasSyncBoth) {
				cachedServerValues[field] = currentValue
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
	fun serializeClientFieldSnapshot(registries: HolderLookup.Provider, fieldName: String): Dynamic<*>? {
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
			val changed = fullSync ||
				manuallyDirty ||
				currentValue != previousValue ||
				shouldSyncContextualField(registries, field, currentValue, fullSync, manuallyDirty)
			if (changed) {
				changes.put(field.componentKey, FieldSyncHandler.serializeFieldData(registries, holder, field, true, fullSync))
				cachedClientValues[field] = currentValue
				hasChanges = true
			}
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
			val currentValue = field.handle.get(holder)
			val previousValue = cachedServerValues[field]
			if (currentValue == previousValue) {
				continue
			}
			changes.put(
				field.componentKey,
				FieldSyncHandler.encodeDetachedServerCandidate(registries, holder, field),
			)
			cachedServerValues[field] = currentValue
		}

		return changes.build()
	}

	fun deserializeItemFieldData(registries: HolderLookup.Provider, fieldData: SyncFieldData) {
		for (field in syncData.getItemSaveFields()) {
			if (!fieldData.fields.containsKey(itemFieldKey(field))) {
				continue
			}
			val savedValue = fieldData[itemFieldKey(field)]?.orElse(null)
			FieldSyncHandler.deserializeFieldData(registries, holder, field, savedValue, false)
		}
	}

	@JvmOverloads
	fun deserializeFieldData(registries: HolderLookup.Provider, fieldData: SyncFieldData, readingClientFields: Boolean, parseExplicitNull: Boolean = false, fullSync: Boolean = false, notifyUnchangedOnFullSync: Boolean = true) {
		deserializeFieldDataInternal(
			registries,
			fieldData,
			readingClientFields,
			parseExplicitNull,
			fullSync,
			notifyUnchangedOnFullSync,
		)
	}

	private fun deserializeFieldDataInternal(registries: HolderLookup.Provider, fieldData: SyncFieldData, readingClientFields: Boolean, parseExplicitNull: Boolean, fullSync: Boolean, notifyUnchangedOnFullSync: Boolean) {
		val fieldsToCheck = if (readingClientFields) syncData.getClientSyncFields() else syncData.getServerSaveFields()
		val changedClientFields = ArrayList<FieldSyncData>()
		for (field in fieldsToCheck) {
			if (!fieldData.fields.containsKey(field.componentKey)) {
				continue
			}
			val savedValue = fieldData[field.componentKey]?.orElse(null)
			val previousClientValue = if (readingClientFields) {
				snapshotClientValue(field.handle.get(holder))
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
				if (field.hasSyncBoth) {
					cachedServerValues[field] = currentValue
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

		val serverFields = syncData.getOrderedServerUpdateFields()
		val selectedFields = serverFields.filter { field -> changes.fields.containsKey(field.componentKey) }
		val selectedKeys = selectedFields.mapTo(HashSet()) { field -> field.componentKey }
		val rejectedKeys = changes.fields.keys.filterNot(selectedKeys::contains)
		if (rejectedKeys.isNotEmpty()) {
			requestAuthoritativeServerAcks(selectedFields)
			return ServerFieldUpdateResult.rejected(
				"Sync: Server field update contains unknown or non-server-updatable fields: ${rejectedKeys.sortedBy { key -> key.toString() }}",
			)
		}
		if (selectedFields.isEmpty()) {
			return ServerFieldUpdateResult.rejected("Sync: Server field update contains no server-updatable fields")
		}

		val pendingUpdates = ArrayList<PendingServerFieldUpdate>(selectedFields.size)
		try {
			// Decode every candidate without consulting or mutating the holder.
			for (field in selectedFields) {
				val encodedValue = changes[field.componentKey]?.orElse(null)
				val candidate = FieldSyncHandler.decodeDetachedServerCandidate(registries, holder, field, encodedValue)
				pendingUpdates.add(PendingServerFieldUpdate(field, candidate))
			}

			// Snapshot every old value before a normalizer is allowed to run.
			for (update in pendingUpdates) {
				update.oldValue = update.field.handle.get(holder)
				validateServerCandidateType(update.field, update.decodedCandidate)
			}

			// Normalize the complete decoded batch before committing any field.
			for (update in pendingUpdates) {
				update.normalizedCandidate = normalizeServerCandidate(update)
			}
			for ((field, _, _, normalizedCandidate) in pendingUpdates) {
				validateServerCandidateType(field, normalizedCandidate)
			}

			commitServerUpdates(pendingUpdates)
		} catch (e: RuntimeException) {
			requestAuthoritativeServerAcks(selectedFields)
			return ServerFieldUpdateResult.rejected(
				e.message ?: "Sync: Server field update failed with ${e.javaClass.simpleName}",
			)
		}

		requestAuthoritativeServerAcks(selectedFields)
		val changed = pendingUpdates.any { update -> update.oldValue != update.normalizedCandidate }
		for (update in pendingUpdates) {
			if (update.oldValue != update.normalizedCandidate) {
				invokeServerChangeListener(update)
			}
		}
		return ServerFieldUpdateResult.accepted(changed)
	}

	@JvmOverloads
	fun applyClientNetworkUpdate(registries: RegistryAccess, changes: SyncFieldData, fullSync: Boolean = false, notifyUnchangedOnFullSync: Boolean = true) {
		val changedClientFields = ArrayList<FieldSyncData>()
		for (field in syncData.getClientSyncFields()) {
			if (!changes.fields.containsKey(field.componentKey)) {
				continue
			}
			val value = changes[field.componentKey]?.orElse(null)
			val previousValue = snapshotClientValue(field.handle.get(holder))
			FieldSyncHandler.deserializeFieldData(
				registries,
				holder,
				field,
				value,
				true,
				parseExplicitNull = true,
				fullSync = fullSync,
				notifyUnchangedOnFullSync = notifyUnchangedOnFullSync,
			)
			val currentValue = field.handle.get(holder)
			cachedClientValues[field] = currentValue
			if (field.hasSyncBoth) {
				cachedServerValues[field] = currentValue
			}
			if (shouldNotifyClientField(field, previousValue, currentValue, fullSync, notifyUnchangedOnFullSync)) {
				changedClientFields.add(field)
			}
		}
		notifyClientFields(changedClientFields)
		if (fullSync) {
			resyncAll = false
			dirtySyncFields.clear()
			pendingClientChanges = null
		}
	}

	private fun snapshotClientValue(value: Any?): Any? = when (value) {
		is Map<*, *> -> value.entries.associateTo(LinkedHashMap(value.size)) { entry ->
			snapshotClientValue(entry.key) to snapshotClientValue(entry.value)
		}

		is Set<*> -> value.mapTo(LinkedHashSet(value.size), ::snapshotClientValue)

		is Collection<*> -> value.map(::snapshotClientValue)

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
		previous is Map<*, *> && current is Map<*, *> -> previous == current
		previous is Set<*> && current is Set<*> -> previous == current
		previous is Collection<*> && current is Collection<*> -> previous == current
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
		val normalizer = update.field.serverNormalizerHandle ?: return update.decodedCandidate
		return try {
			normalizer.invoke(holder, update.decodedCandidate)
		} catch (e: Throwable) {
			throw IllegalArgumentException(
				"Sync: Server normalizer rejected field ${update.field.fieldName} of type ${update.field.type}",
				e,
			)
		}
	}

	private fun validateServerCandidateType(field: FieldSyncData, @Nullable candidate: Any?) {
		val fieldType = field.handle.varType()
		if (candidate == null) {
			if (fieldType.isPrimitive) {
				throw IllegalArgumentException(
					"Sync: Server candidate for primitive field ${field.fieldName} of type ${field.type} was null",
				)
			}
			return
		}
		if (!boxedType(fieldType).isInstance(candidate)) {
			throw IllegalArgumentException(
				"Sync: Server candidate for field ${field.fieldName} expected ${fieldType.typeName} but decoded ${candidate.javaClass.name}",
			)
		}
	}

	private fun commitServerUpdates(updates: List<PendingServerFieldUpdate>) {
		var committed = 0
		try {
			for ((field, _, _, normalizedCandidate) in updates) {
				field.handle.set(holder, normalizedCandidate)
				committed++
			}
		} catch (e: Throwable) {
			for (index in committed - 1 downTo 0) {
				val update = updates[index]
				try {
					update.field.handle.set(holder, update.oldValue)
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
			listener.invoke(holder, update.oldValue, update.normalizedCandidate)
		} catch (e: Throwable) {
			GTCEu.LOGGER.error(
				"Sync: Error while invoking server change listener for field {} of type {}",
				update.field.fieldName,
				update.field.type,
				e,
			)
		}
	}

	private fun boxedType(type: Class<*>): Class<*> = when (type) {
		Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
		Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
		Char::class.javaPrimitiveType -> Char::class.javaObjectType
		Short::class.javaPrimitiveType -> Short::class.javaObjectType
		Int::class.javaPrimitiveType -> Int::class.javaObjectType
		Long::class.javaPrimitiveType -> Long::class.javaObjectType
		Float::class.javaPrimitiveType -> Float::class.javaObjectType
		Double::class.javaPrimitiveType -> Double::class.javaObjectType
		else -> type
	}

	private fun itemFieldKey(field: FieldSyncData) = field.itemDataKey
		?: throw IllegalArgumentException("Sync: @ItemSave field ${field.fieldName} has no item data component key")

	private data class PendingServerFieldUpdate(val field: FieldSyncData, @field:Nullable val decodedCandidate: Any?, @field:Nullable var oldValue: Any? = null, @field:Nullable var normalizedCandidate: Any? = null)
}
