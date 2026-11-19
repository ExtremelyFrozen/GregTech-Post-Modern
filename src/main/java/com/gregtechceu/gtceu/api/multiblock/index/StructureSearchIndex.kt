package com.gregtechceu.gtceu.api.multiblock.index

import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData

import net.minecraft.core.BlockPos

import it.unimi.dsi.fastutil.HashCommon
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap

/** Sparse interval treap for unformed structures; even huge repeat ranges allocate one region per structure. */
class StructureSearchIndex {

	private val regions = Reference2ObjectOpenHashMap<MultiblockState, Node>()
	private var root: Node? = null
	private var order = 0L

	fun contains(state: MultiblockState): Boolean = regions.containsKey(state)

	fun add(state: MultiblockState, radii: IntArray) {
		val radius = maxOf(radii[0], radii[1], radii[2]).toLong()
		val pos = state.controllerPos
		val region = Region(state, pos.x - radius, pos.x + radius, pos.y - radius, pos.y + radius, pos.z - radius, pos.z + radius)
		regions[state]?.let { previous ->
			if (previous.region == region) return
			root = remove(root, previous)
		}
		val node = Node(region, order++)
		regions[state] = node
		root = insert(root, node)
	}

	fun remove(state: MultiblockState) {
		regions.remove(state)?.let { root = remove(root, it) }
	}

	fun changed(position: BlockPos, data: MultiblockWorldSavedData) = visit(root, position, data)

	fun clear() {
		regions.clear()
		root = null
	}

	private data class Region(val state: MultiblockState, val minimumX: Long, val maximumX: Long, val minimumY: Long, val maximumY: Long, val minimumZ: Long, val maximumZ: Long)

	private class Node(val region: Region, val order: Long) {
		val priority = HashCommon.mix(order)
		var left: Node? = null
		var right: Node? = null
		var maximumX = region.maximumX
		fun update() {
			maximumX = maxOf(region.maximumX, left?.maximumX ?: Long.MIN_VALUE, right?.maximumX ?: Long.MIN_VALUE)
		}
	}

	private fun insert(root: Node?, node: Node): Node {
		if (root == null) return node
		var result = root
		if (compare(node, root) < 0) {
			val left = insert(root.left, node)
			root.left = left
			if (left.priority < root.priority) {
				root.left = left.right
				left.right = root
				root.update()
				result = left
			}
		} else {
			val right = insert(root.right, node)
			root.right = right
			if (right.priority < root.priority) {
				root.right = right.left
				right.left = root
				root.update()
				result = right
			}
		}
		result.update()
		return result
	}

	private fun remove(root: Node?, node: Node): Node? {
		if (root == null) return null
		val comparison = compare(node, root)
		when {
			comparison < 0 -> root.left = remove(root.left, node)
			comparison > 0 -> root.right = remove(root.right, node)
			else -> return merge(root.left, root.right)
		}
		root.update()
		return root
	}

	private fun merge(left: Node?, right: Node?): Node? {
		if (left == null) return right
		if (right == null) return left
		return if (left.priority < right.priority) {
			left.right = merge(left.right, right)
			left.also { it.update() }
		} else {
			right.left = merge(left, right.left)
			right.also { it.update() }
		}
	}

	private fun compare(left: Node, right: Node): Int = left.region.minimumX.compareTo(right.region.minimumX).let { if (it == 0) left.order.compareTo(right.order) else it }

	private fun visit(node: Node?, position: BlockPos, data: MultiblockWorldSavedData) {
		if (node == null || position.x > node.maximumX) return
		val region = node.region
		visit(node.left, position, data)
		if (position.x < region.minimumX) return
		if (position.x <= region.maximumX && position.y >= region.minimumY && position.y <= region.maximumY && position.z >= region.minimumZ && position.z <= region.maximumZ) {
			data.queueSearchPosition(region.state, position.asLong())
		}
		visit(node.right, position, data)
	}
}
