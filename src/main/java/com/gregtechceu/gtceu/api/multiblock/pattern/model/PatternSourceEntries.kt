package com.gregtechceu.gtceu.api.multiblock.pattern.model

import it.unimi.dsi.fastutil.chars.AbstractChar2ObjectMap
import it.unimi.dsi.fastutil.chars.Char2ObjectMap
import it.unimi.dsi.fastutil.objects.AbstractObject2ObjectMap
import it.unimi.dsi.fastutil.objects.AbstractObject2ObjectMap.BasicEntry
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator
import it.unimi.dsi.fastutil.objects.AbstractObjectSet
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet

/** Source-model interoperability views. The source owns immutable entries; these views never clone a table. */
internal object PatternSourceEntries {
	fun <K, V> objects(source: Map<K, V>): Object2ObjectMap<K, V> = object : AbstractObject2ObjectMap<K, V>() {
		override val size: Int get() = source.size
		override fun get(key: K): V? = source[key]
		override fun containsKey(key: K): Boolean = source.containsKey(key)
		override fun object2ObjectEntrySet(): ObjectSet<Object2ObjectMap.Entry<K, V>> = object : AbstractObjectSet<Object2ObjectMap.Entry<K, V>>() {
			override val size: Int get() = source.size
			override fun iterator(): ObjectIterator<Object2ObjectMap.Entry<K, V>> = object : AbstractObjectIterator<Object2ObjectMap.Entry<K, V>>() {
				override fun remove(): Unit = throw UnsupportedOperationException()
				private val entries = source.entries.iterator()
				override fun hasNext(): Boolean = entries.hasNext()
				override fun next(): Object2ObjectMap.Entry<K, V> = entries.next().let { BasicEntry(it.key, it.value) }
			}
		}
	}

	fun <V> characters(source: Map<Char, V>): Char2ObjectMap<V> = object : AbstractChar2ObjectMap<V>() {
		override val size: Int get() = source.size
		override fun get(key: Char): V? = source[key]
		override fun containsKey(key: Char): Boolean = source.containsKey(key)
		override fun char2ObjectEntrySet(): ObjectSet<Char2ObjectMap.Entry<V>> = object : AbstractObjectSet<Char2ObjectMap.Entry<V>>() {
			override val size: Int get() = source.size
			override fun iterator(): ObjectIterator<Char2ObjectMap.Entry<V>> = object : AbstractObjectIterator<Char2ObjectMap.Entry<V>>() {
				override fun remove(): Unit = throw UnsupportedOperationException()
				private val entries = source.entries.iterator()
				override fun hasNext(): Boolean = entries.hasNext()
				override fun next(): Char2ObjectMap.Entry<V> = entries.next().let { AbstractChar2ObjectMap.BasicEntry(it.key, it.value) }
			}
		}
	}
}
