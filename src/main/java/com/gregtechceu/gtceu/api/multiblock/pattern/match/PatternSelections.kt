package com.gregtechceu.gtceu.api.multiblock.pattern.match

import it.unimi.dsi.fastutil.objects.*

/** Immutable branch selections. An external read-only source is shared; edits copy only a hash-trie path. */
internal class PatternSelections private constructor(private val source: Map<String, String>, private val root: Node?, override val size: Int) : AbstractObject2ObjectMap<String, String>() {
	private sealed interface Node
	private class Leaf(val hash: Int, val key: String, val value: String?) : Node
	private class Collision(val hash: Int, val leaves: Array<Leaf>) : Node
	private class Branch(val bitmap: Int, val children: Array<Node>) : Node

	override fun get(key: String): String? {
		val leaf = find(root, key.hashCode(), key, 0)
		return if (leaf == null) source[key] else leaf.value
	}
	override fun containsKey(key: String): Boolean {
		val leaf = find(root, key.hashCode(), key, 0)
		return if (leaf == null) source.containsKey(key) else leaf.value != null
	}

	fun with(key: String, value: String): PatternSelections {
		if (get(key) == value) return this
		return PatternSelections(source, insert(root, Leaf(key.hashCode(), key, value), 0), size + if (containsKey(key)) 0 else 1)
	}

	fun without(key: String): PatternSelections {
		if (!containsKey(key)) return this
		return PatternSelections(source, insert(root, Leaf(key.hashCode(), key, null), 0), size - 1)
	}

	fun withoutPrefix(prefix: String): PatternSelections {
		var result = this
		for (entry in object2ObjectEntrySet()) if (entry.key.startsWith(prefix)) result = result.without(entry.key)
		return result
	}

	override fun object2ObjectEntrySet(): ObjectSet<Object2ObjectMap.Entry<String, String>> = object : AbstractObjectSet<Object2ObjectMap.Entry<String, String>>() {
		override val size: Int get() = this@PatternSelections.size
		override fun iterator(): ObjectIterator<Object2ObjectMap.Entry<String, String>> = object : AbstractObjectIterator<Object2ObjectMap.Entry<String, String>>() {
			override fun remove(): Unit = throw UnsupportedOperationException()
			private val inherited = source.entries.iterator()
			private val pending = ObjectArrayList<Node>().also { if (root != null) it.add(root) }
			private var ready: Object2ObjectMap.Entry<String, String>? = null

			override fun hasNext(): Boolean {
				if (ready != null) return true
				while (inherited.hasNext()) {
					val entry = inherited.next()
					if (find(root, entry.key.hashCode(), entry.key, 0) == null) {
						ready = BasicEntry(entry.key, entry.value)
						return true
					}
				}
				while (!pending.isEmpty) {
					when (val node = pending.removeAt(pending.size - 1)) {
						is Leaf -> if (node.value != null) {
							ready = BasicEntry(node.key, node.value)
							return true
						}

						is Collision -> for (index in node.leaves.indices.reversed()) pending.add(node.leaves[index])

						is Branch -> for (index in node.children.indices.reversed()) pending.add(node.children[index])
					}
				}
				return false
			}

			override fun next(): Object2ObjectMap.Entry<String, String> {
				if (!hasNext()) throw NoSuchElementException()
				val entry = ready!!
				ready = null
				return entry
			}
		}
	}

	companion object {
		private val EMPTY = PatternSelections(Object2ObjectMaps.emptyMap(), null, 0)

		@JvmStatic
		fun empty(): PatternSelections = EMPTY

		@JvmStatic
		fun of(values: Map<String, String>): PatternSelections = values as? PatternSelections ?: if (values.isEmpty()) EMPTY else PatternSelections(values, null, values.size)

		private fun find(node: Node?, hash: Int, key: String, shift: Int): Leaf? = when (node) {
			null -> null

			is Leaf -> if (node.hash == hash && node.key == key) node else null

			is Collision -> if (node.hash != hash) null else node.leaves.firstOrNull { it.key == key }

			is Branch -> {
				val bit = 1 shl ((hash ushr shift) and 31)
				if (node.bitmap and bit == 0) null else find(node.children[(node.bitmap and (bit - 1)).countOneBits()], hash, key, shift + 5)
			}
		}

		private fun insert(node: Node?, leaf: Leaf, shift: Int): Node = when (node) {
			null -> leaf

			is Leaf -> when {
				node.key == leaf.key -> leaf
				node.hash == leaf.hash -> Collision(leaf.hash, arrayOf(node, leaf))
				else -> merge(node.hash, node, leaf.hash, leaf, shift)
			}

			is Collision -> {
				if (node.hash != leaf.hash) {
					merge(node.hash, node, leaf.hash, leaf, shift)
				} else {
					val index = node.leaves.indexOfFirst { it.key == leaf.key }
					if (index < 0) {
						Collision(node.hash, Array(node.leaves.size + 1) { if (it == node.leaves.size) leaf else node.leaves[it] })
					} else {
						Collision(node.hash, node.leaves.copyOf().also { it[index] = leaf })
					}
				}
			}

			is Branch -> {
				val bit = 1 shl ((leaf.hash ushr shift) and 31)
				val index = (node.bitmap and (bit - 1)).countOneBits()
				if (node.bitmap and bit == 0) {
					Branch(
						node.bitmap or bit,
						Array(node.children.size + 1) {
							if (it < index) {
								node.children[it]
							} else if (it == index) {
								leaf
							} else {
								node.children[it - 1]
							}
						},
					)
				} else {
					Branch(node.bitmap, node.children.copyOf().also { it[index] = insert(node.children[index], leaf, shift + 5) })
				}
			}
		}

		private fun merge(firstHash: Int, first: Node, secondHash: Int, second: Node, shift: Int): Node {
			val firstSlot = (firstHash ushr shift) and 31
			val secondSlot = (secondHash ushr shift) and 31
			if (firstSlot == secondSlot) return Branch(1 shl firstSlot, arrayOf(merge(firstHash, first, secondHash, second, shift + 5)))
			return Branch((1 shl firstSlot) or (1 shl secondSlot), if (firstSlot < secondSlot) arrayOf(first, second) else arrayOf(second, first))
		}
	}
}
