package com.gregtechceu.gtceu.api.sync_system.serialization

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import it.unimi.dsi.fastutil.BigList

import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * Builds collection codecs from declared generic contracts and collection-specific factory tables.
 * Each nullable element occupies a map-shaped slot so lists remain homogeneous in NBT.
 */
object CollectionCodecs {

	class Construction @JvmOverloads constructor(val create: () -> Any, val finish: (Any) -> Any = { it })

	private val factories = ConcurrentHashMap<Class<*>, (Type, Any?) -> Construction>()

	@JvmStatic
	fun isContainer(type: Type): Boolean = Types.raw(type)?.let {
		Map::class.java.isAssignableFrom(it) || Collection::class.java.isAssignableFrom(it)
	} == true

	@JvmStatic
	fun codec(type: Type, current: Any?, elementCodec: (Type, Any?, String) -> Codec<Any?>): Codec<Any>? {
		val raw = Types.raw(type) ?: return null
		if (Map::class.java.isAssignableFrom(raw)) {
			val arguments = arguments(type, current, Map::class.java)
			return mapCodec(type, current, arguments[0], arguments[1], elementCodec)
		}
		if (Collection::class.java.isAssignableFrom(raw)) {
			val element = arguments(type, current, Collection::class.java).single()
			return sequenceCodec(type, current, element, elementCodec)
		}
		return null
	}

	private fun arguments(type: Type, current: Any?, contract: Class<*>): List<Type> {
		val declared = requireNotNull(Types.arguments(type, contract)) { "Missing ${contract.simpleName} contract: $type" }
		val inherited = current?.let { Types.arguments(it.javaClass, contract) }
		return declared.mapIndexed { index, argument ->
			val resolved = if (argument is TypeVariable<*>) inherited?.get(index) ?: argument else argument
			require(resolved !is TypeVariable<*>) { "Unbound element type $resolved in $type" }
			resolved
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun nullable(codec: Codec<Any?>): Codec<Any?> = (codec as Codec<Any>).optionalFieldOf("value").codec()
		.xmap({ it.orElse(null) }, { Optional.ofNullable(it) })

	@Suppress("UNCHECKED_CAST") // The field's generic contract supplies the element codec.
	private fun sequenceCodec(type: Type, current: Any?, elementType: Type, elementCodec: (Type, Any?, String) -> Codec<Any?>): Codec<Any> = object : Codec<Any> {
		override fun <D> decode(ops: DynamicOps<D>, input: D): DataResult<Pair<Any, D>> = ops.getStream(input).flatMap { stream ->
			attempt {
				val construction = construction(type, current)
				val target = construction.create() as MutableCollection<Any?>
				target.clear()
				stream.use {
					val iterator = it.iterator()
					var index = 0L
					while (iterator.hasNext()) {
						val value = nullable(elementCodec(elementType, previous(current, index), index.toString()))
							.parse(ops, iterator.next()).getOrThrow()
						target.add(value)
						index++
					}
				}
				Pair.of(construction.finish(target), ops.empty())
			}
		}

		override fun <D> encode(input: Any, ops: DynamicOps<D>, prefix: D): DataResult<D> = attempt {
			val builder = ops.listBuilder()
			var index = 0L
			for (value in input as Collection<*>) {
				builder.add(nullable(elementCodec(elementType, value, index.toString())).encodeStart(ops, value))
				index++
			}
			builder.build(prefix).getOrThrow()
		}
	}

	@Suppress("UNCHECKED_CAST") // The field's generic contract supplies both entry codecs.
	private fun mapCodec(type: Type, current: Any?, keyType: Type, valueType: Type, elementCodec: (Type, Any?, String) -> Codec<Any?>): Codec<Any> = object : Codec<Any> {
		override fun <D> decode(ops: DynamicOps<D>, input: D): DataResult<Pair<Any, D>> = ops.getStream(input).flatMap { stream ->
			attempt {
				val construction = construction(type, current)
				val target = construction.create() as MutableMap<Any?, Any?>
				target.clear()
				stream.use {
					val iterator = it.iterator()
					var index = 0L
					while (iterator.hasNext()) {
						val entry = ops.getMap(iterator.next()).getOrThrow()
						val encodedKey = requireNotNull(entry.get("key")) { "Map entry $index has no key slot" }
						val encodedValue = requireNotNull(entry.get("value")) { "Map entry $index has no value slot" }
						val key = nullable(elementCodec(keyType, null, "$index.key")).parse(ops, encodedKey).getOrThrow()
						val previous = (current as? Map<*, *>)?.get(key)
						val value = nullable(elementCodec(valueType, previous, "$index.value")).parse(ops, encodedValue).getOrThrow()
						target[key] = value
						index++
					}
				}
				Pair.of(construction.finish(target), ops.empty())
			}
		}

		override fun <D> encode(input: Any, ops: DynamicOps<D>, prefix: D): DataResult<D> = attempt {
			val builder = ops.listBuilder()
			var index = 0L
			for ((key, value) in input as Map<*, *>) {
				val entry = ops.mapBuilder()
				entry.add("key", nullable(elementCodec(keyType, key, "$index.key")).encodeStart(ops, key))
				entry.add("value", nullable(elementCodec(valueType, value, "$index.value")).encodeStart(ops, value))
				builder.add(entry.build(ops.empty()))
				index++
			}
			builder.build(prefix).getOrThrow()
		}
	}

	/** Applies detached decoded contents while preserving the existing container's policies and references. */
	@JvmStatic
	fun update(current: Any, decoded: Any): Boolean {
		if (JavaCollections.isReadOnly(current) || FastutilCollections.isReadOnly(current) || KotlinCollections.isReadOnly(current)) return false
		if (JavaCollections.isFixedSize(current)) {
			@Suppress("UNCHECKED_CAST")
			val target = current as MutableList<Any?>
			val values = decoded as List<*>
			if (target.size != values.size) return false
			for (index in values.indices) target[index] = values[index]
			return true
		}

		when (current) {
			is MutableMap<*, *> -> {
				@Suppress("UNCHECKED_CAST")
				val target = current as MutableMap<Any?, Any?>
				target.clear()
				target.putAll(decoded as Map<*, *>)
			}

			is MutableCollection<*> -> {
				@Suppress("UNCHECKED_CAST")
				val target = current as MutableCollection<Any?>
				target.clear()
				target.addAll(decoded as Collection<*>)
			}

			else -> return false
		}
		return true
	}

	/** Copies container contents with the registered collection policy before an in-place transaction. */
	fun copyCurrent(type: Type, current: Any?): Any? {
		if (current == null || !isContainer(current.javaClass)) return current
		val construction = construction(type, current)
		val copy = construction.create()
		update(copy, current)
		return construction.finish(copy)
	}

	private fun construction(type: Type, current: Any?): Construction {
		val declared = requireNotNull(Types.raw(type))
		(current?.javaClass?.let(factories::get) ?: factories[declared])?.let { return it(type, current) }
		return FastutilCollections.construction(type, current)
			?: KotlinCollections.construction(type, current)
			?: JavaCollections.construction(type, current)
			?: throw IllegalArgumentException("No collection factory registered for $type")
	}

	private fun previous(current: Any?, index: Long): Any? = when (current) {
		is BigList<*> -> if (index < current.size64()) current[index] else null
		is List<*> -> if (index < current.size) current[index.toInt()] else null
		else -> null
	}

	private inline fun <T> attempt(operation: () -> T): DataResult<T> = try {
		DataResult.success(operation())
	} catch (exception: RuntimeException) {
		DataResult.error { "Collection codec: ${exception.message ?: exception.javaClass.simpleName}" }
	}
}
