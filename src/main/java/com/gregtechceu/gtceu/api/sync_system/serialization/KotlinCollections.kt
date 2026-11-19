package com.gregtechceu.gtceu.api.sync_system.serialization

import com.gregtechceu.gtceu.api.sync_system.serialization.CollectionCodecs.Construction

import java.lang.reflect.Type

import kotlin.collections.ArrayDeque as KotlinArrayDeque

internal object KotlinCollections {
	private val listBuilder = buildList { add(Unit) }.javaClass
	private val setBuilder = buildSet { add(Unit) }.javaClass
	private val mapBuilder = buildMap { put(Unit, Unit) }.javaClass
	private val readOnly = setOf(emptyMap<Any?, Any?>().javaClass, emptyList<Any?>().javaClass, emptySet<Any?>().javaClass, listBuilder, setBuilder, mapBuilder)

	fun isReadOnly(value: Any): Boolean = value.javaClass in readOnly

	fun construction(type: Type, current: Any?): Construction? {
		val raw = Types.raw(type) ?: return null
		return when (current?.javaClass ?: raw) {
			KotlinArrayDeque::class.java -> Construction({ KotlinArrayDeque<Any?>() })
			emptyList<Any?>().javaClass -> Construction({ ArrayList<Any?>() }, { (it as List<*>).toList() })
			emptySet<Any?>().javaClass -> Construction({ LinkedHashSet<Any?>() }, { (it as Set<*>).toSet() })
			emptyMap<Any?, Any?>().javaClass -> Construction({ LinkedHashMap<Any?, Any?>() }, { (it as Map<*, *>).toMap() })
			listBuilder -> Construction({ ArrayList<Any?>() }, { buildList { addAll(it as Collection<*>) } })
			setBuilder -> Construction({ LinkedHashSet<Any?>() }, { buildSet { addAll(it as Collection<*>) } })
			mapBuilder -> Construction({ LinkedHashMap<Any?, Any?>() }, { buildMap { putAll(it as Map<*, *>) } })
			else -> null
		}
	}
}
