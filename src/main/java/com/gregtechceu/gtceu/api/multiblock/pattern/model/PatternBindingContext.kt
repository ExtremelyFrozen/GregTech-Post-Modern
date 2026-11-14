package com.gregtechceu.gtceu.api.multiblock.pattern.model

import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap
import it.unimi.dsi.fastutil.objects.AbstractObject2ObjectMap
import it.unimi.dsi.fastutil.objects.AbstractObject2ObjectMap.BasicEntry
import it.unimi.dsi.fastutil.objects.AbstractObjectIterator
import it.unimi.dsi.fastutil.objects.AbstractObjectSet
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet

/** Read-only lexical bindings: a call site owns only its local entries and shares its parent scope. */
internal class PatternBindingContext private constructor(private val parent: Map<String, PatternBinding>, private val local: Map<String, PatternBinding>, private val caller: Map<String, PatternBinding>?) : AbstractObject2ObjectMap<String, PatternBinding>() {
	override val size: Int get() {
		var count = local.size
		for (key in parent.keys) if (!local.containsKey(key)) count++
		return count
	}
	override fun containsKey(key: String): Boolean = local.containsKey(key) || parent.containsKey(key)
	override fun get(key: String): PatternBinding? {
		val binding = local[key] ?: return parent[key]
		val source = caller ?: return binding
		return when (binding) {
			is PatternBinding.IntegerExpression -> PatternBinding.IntegerValue(binding.expression.evaluate(integers(source)))
			is PatternBinding.Token if binding.value.startsWith("$") -> source[binding.value.substring(1)] ?: error("Unknown forwarded parameter '${binding.value}'")
			else -> binding
		}
	}
	override fun object2ObjectEntrySet(): ObjectSet<Object2ObjectMap.Entry<String, PatternBinding>> = object : AbstractObjectSet<Object2ObjectMap.Entry<String, PatternBinding>>() {
		override val size: Int get() = this@PatternBindingContext.size
		override fun iterator(): ObjectIterator<Object2ObjectMap.Entry<String, PatternBinding>> = object : AbstractObjectIterator<Object2ObjectMap.Entry<String, PatternBinding>>() {
			override fun remove(): Unit = throw UnsupportedOperationException()
			private val inherited = parent.keys.iterator()
			private val own = local.keys.iterator()
			private var ready: String? = null
			override fun hasNext(): Boolean {
				if (ready != null) return true
				while (inherited.hasNext()) {
					val key = inherited.next()
					if (!local.containsKey(key)) {
						ready = key
						return true
					}
				}
				if (!own.hasNext()) return false
				ready = own.next()
				return true
			}
			override fun next(): Object2ObjectMap.Entry<String, PatternBinding> {
				if (!hasNext()) throw NoSuchElementException()
				val key = ready!!
				ready = null
				return BasicEntry(key, this@PatternBindingContext[key]!!)
			}
		}
	}

	companion object {
		fun overlay(parent: Map<String, PatternBinding>, local: Map<String, PatternBinding>): Object2ObjectMap<String, PatternBinding> = PatternBindingContext(parent, local, null)
		fun resolve(local: Map<String, PatternBinding>, caller: Map<String, PatternBinding>): Object2ObjectMap<String, PatternBinding> = PatternBindingContext(Object2ObjectMaps.emptyMap(), local, caller)
		fun integers(bindings: Map<String, PatternBinding>): Object2IntMap<String> = object : AbstractObject2IntMap<String>() {
			override val size: Int get() {
				var count = 0
				for (value in bindings.values) if (value is PatternBinding.IntegerValue) count++
				return count
			}
			override fun containsKey(key: String): Boolean = bindings[key] is PatternBinding.IntegerValue
			override fun getInt(key: Any?): Int = if (key is String) (bindings[key] as? PatternBinding.IntegerValue)?.value ?: 0 else 0
			override fun object2IntEntrySet(): ObjectSet<Object2IntMap.Entry<String>> = object : AbstractObjectSet<Object2IntMap.Entry<String>>() {
				override val size: Int get() {
					var count = 0
					for (value in bindings.values) if (value is PatternBinding.IntegerValue) count++
					return count
				}
				override fun iterator(): ObjectIterator<Object2IntMap.Entry<String>> = object : AbstractObjectIterator<Object2IntMap.Entry<String>>() {
					override fun remove(): Unit = throw UnsupportedOperationException()
					private val source = bindings.entries.iterator()
					private var ready: Object2IntMap.Entry<String>? = null
					override fun hasNext(): Boolean {
						if (ready != null) return true
						while (source.hasNext()) {
							val entry = source.next()
							val value = entry.value
							if (value is PatternBinding.IntegerValue) {
								ready = AbstractObject2IntMap.BasicEntry(entry.key, value.value)
								return true
							}
						}
						return false
					}
					override fun next(): Object2IntMap.Entry<String> {
						if (!hasNext()) throw NoSuchElementException()
						val entry = ready!!
						ready = null
						return entry
					}
				}
			}
		}
	}
}
