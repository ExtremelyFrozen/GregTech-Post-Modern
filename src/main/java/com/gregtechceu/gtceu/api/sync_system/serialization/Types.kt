package com.gregtechceu.gtceu.api.sync_system.serialization

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

import java.lang.reflect.Array as ReflectArray

/** Resolves collection contracts through generic superclass and interface bindings, including primitive fastutil APIs. */
object Types {

	@JvmStatic
	fun raw(type: Type): Class<*>? = when (type) {
		is Class<*> -> type
		is ParameterizedType -> raw(type.rawType)
		is GenericArrayType -> raw(type.genericComponentType)?.let { ReflectArray.newInstance(it, 0).javaClass }
		is WildcardType -> type.upperBounds.firstOrNull()?.let(::raw)
		is TypeVariable<*> -> type.bounds.firstOrNull()?.let(::raw)
		else -> null
	}

	/** Returns the actual arguments of [contract], without mistaking an unresolved variable for Object. */
	@JvmStatic
	fun arguments(type: Type, contract: Class<*>): List<Type>? = hierarchy(type)
		.firstOrNull { raw(it) == contract }
		?.let { resolved ->
			if (resolved is ParameterizedType) resolved.actualTypeArguments.toList() else contract.typeParameters.toList()
		}

	/** Visits each resolved ancestor once; specializations keep their complete nested generic argument types. */
	@JvmStatic
	fun hierarchy(type: Type): List<Type> {
		val result = ObjectArrayList<Type>()
		val visited = ObjectOpenHashSet<Type>()
		fun visit(current: Type) {
			if (!visited.add(current)) return
			result.add(current)
			val clazz = raw(current) ?: return
			val bindings = bindings(current)
			clazz.genericInterfaces.forEach { visit(resolve(it, bindings)) }
			clazz.genericSuperclass?.let { visit(resolve(it, bindings)) }
		}
		visit(type)
		return result
	}

	private fun bindings(type: Type): Map<TypeVariable<*>, Type> {
		if (type !is ParameterizedType) return emptyMap()
		val bindings = Object2ObjectOpenHashMap<TypeVariable<*>, Type>(type.ownerType?.let(::bindings) ?: emptyMap())
		val clazz = raw(type) ?: return bindings
		clazz.typeParameters.zip(type.actualTypeArguments).forEach { (variable, argument) ->
			bindings[variable] = resolve(argument, bindings)
		}
		return bindings
	}

	@JvmStatic
	fun resolve(type: Type, bindings: Map<out TypeVariable<*>, Type>): Type = when (type) {
		is TypeVariable<*> -> bindings[type]?.takeIf { it != type }?.let { resolve(it, bindings) } ?: type

		is ParameterizedType -> Parameterized(
			type.rawType,
			type.ownerType?.let { resolve(it, bindings) },
			type.actualTypeArguments.map { resolve(it, bindings) }.toTypedArray(),
		)

		is GenericArrayType -> {
			val component = resolve(type.genericComponentType, bindings)
			if (component is Class<*>) ReflectArray.newInstance(component, 0).javaClass else GenericArray(component)
		}

		is WildcardType -> type.upperBounds.firstOrNull()?.let { resolve(it, bindings) } ?: Any::class.java

		else -> type
	}

	private class Parameterized(private val raw: Type, private val owner: Type?, private val arguments: Array<Type>) : ParameterizedType {
		override fun getRawType(): Type = raw
		override fun getOwnerType(): Type? = owner
		override fun getActualTypeArguments(): Array<Type> = arguments.clone()
		override fun equals(other: Any?): Boolean = other is ParameterizedType && raw == other.rawType && owner == other.ownerType && arguments.contentEquals(other.actualTypeArguments)
		override fun hashCode(): Int = raw.hashCode() xor (owner?.hashCode() ?: 0) xor arguments.contentHashCode()
		override fun getTypeName(): String = raw.typeName + arguments.joinToString(", ", "<", ">") { it.typeName }
		override fun toString(): String = typeName
	}

	private class GenericArray(private val component: Type) : GenericArrayType {
		override fun getGenericComponentType(): Type = component
		override fun getTypeName(): String = "${component.typeName}[]"
		override fun equals(other: Any?): Boolean = other is GenericArrayType && component == other.genericComponentType
		override fun hashCode(): Int = component.hashCode()
	}
}
