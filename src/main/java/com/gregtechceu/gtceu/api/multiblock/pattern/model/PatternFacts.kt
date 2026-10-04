package com.gregtechceu.gtceu.api.multiblock.pattern.model

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import org.jspecify.annotations.NullMarked

@NullMarked
class PatternFacts {
    private val data = Object2ObjectOpenHashMap<String, Any>()
    private val counts = Object2IntOpenHashMap<String>()

    fun reset() {
        data.clear()
        counts.clear()
    }

    fun set(key: String, value: Any) {
        data[key] = value
    }

    fun getInt(key: String): Int = (data[key] as? Int) ?: 0

    fun increment(key: String, value: Int) {
        set(key, getInt(key) + value)
    }

    fun addFact(fact: String) {
        require(fact.isNotBlank()) { "Fact name must not be blank" }
        counts.addTo(fact, 1)
    }

    fun merge(other: PatternFacts) {
        other.counts.object2IntEntrySet().forEach { counts.addTo(it.key, it.intValue) }
        data.putAll(other.data)
    }

    fun count(fact: String): Int = counts.getInt(fact)

    fun counts(): Object2IntMap<String> = Object2IntMaps.unmodifiable(counts)

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrDefault(key: String, defaultValue: T): T = (data[key] ?: defaultValue) as T

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? = data[key] as T?

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrCreate(key: String, creator: java.util.function.Supplier<T>): T {
        val existing = data[key]
        if (existing != null) return existing as T
        val created = creator.get()
        data[key] = created as Any
        return created
    }

    fun <T> getOrPut(key: String, initialValue: T): T {
        @Suppress("UNCHECKED_CAST")
        val existing = data[key] as T?
        if (existing != null) return existing
        data[key] = initialValue as Any
        return initialValue
    }

    fun containsKey(key: String): Boolean = data.containsKey(key)

    fun entrySet(): MutableSet<MutableMap.MutableEntry<String, Any>> = data.entries
}
