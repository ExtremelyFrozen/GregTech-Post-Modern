package com.gregtechceu.gtceu.api.sync_system

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nullable

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

@ApiStatus.Internal
class TypeDeclaration(type: Type) {
	val rawType: Type = type

	@field:Nullable
	val classValue: Class<*>?
	val genericTypeArgs: Array<TypeDeclaration>
	private val arrayComponentType: TypeDeclaration?

	init {
		when (type) {
			is ParameterizedType -> {
				classValue = type.rawType as Class<*>
				genericTypeArgs = type.actualTypeArguments.map(::TypeDeclaration).toTypedArray()
				arrayComponentType = null
			}

			is GenericArrayType -> {
				classValue = null
				arrayComponentType = TypeDeclaration(type.genericComponentType)
				genericTypeArgs = emptyArray()
			}

			is WildcardType -> {
				val bound = TypeDeclaration(type.upperBounds.single())
				classValue = bound.classValue
				genericTypeArgs = bound.genericTypeArgs
				arrayComponentType = bound.arrayComponentType
			}

			else -> {
				classValue = type as Class<*>
				genericTypeArgs = emptyArray()
				arrayComponentType = if (classValue.isArray) TypeDeclaration(classValue.componentType) else null
			}
		}
	}

	override fun toString(): String = rawType.toString()
}
