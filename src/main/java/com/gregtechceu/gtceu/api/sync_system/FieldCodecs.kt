@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package com.gregtechceu.gtceu.api.sync_system

import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged
import com.gregtechceu.gtceu.api.sync_system.serialization.CollectionCodecs
import com.gregtechceu.gtceu.api.sync_system.serialization.SyncCodec
import com.gregtechceu.gtceu.api.sync_system.serialization.Types

import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.fluids.FluidStack

import com.mojang.datafixers.util.Pair
import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import com.mojang.serialization.DynamicOps
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import org.jetbrains.annotations.Nullable

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.util.Arrays
import java.util.HashMap
import java.util.Optional
import java.util.UUID
import java.util.function.Supplier
import java.util.stream.IntStream
import java.util.stream.LongStream

import java.lang.Enum as JavaEnum
import java.lang.reflect.Array as ReflectArray

object FieldCodecs {
	private val codecLock = Any()
	private val REGISTERED: MutableMap<Type, Codec<*>> = Object2ObjectOpenHashMap()
	private val REGISTERED_SUPPLIERS: MutableMap<Class<*>, Supplier<Codec<*>>> = Object2ObjectOpenHashMap()
	private val FACTORIES: MutableMap<Class<*>, Supplier<*>> = Object2ObjectOpenHashMap()
	private val STREAM_CODECS: MutableMap<Type, StreamCodec<RegistryFriendlyByteBuf, *>?> = Object2ObjectOpenHashMap()
	private val TYPE_CACHE: MutableMap<Type, Codec<*>?> = Object2ObjectOpenHashMap()
	private val CONTEXTUAL_REGISTERED: MutableMap<Type, ContextualFieldCodec<*>> = Object2ObjectOpenHashMap()
	private val CONTEXTUAL_TYPE_CACHE: MutableMap<Type, ContextualFieldCodec<*>?> = Object2ObjectOpenHashMap()

	private val PRIMITIVE_TO_BOXED: Map<Type, Type> = mapOf(
		Boolean::class.javaPrimitiveType!! to Boolean::class.javaObjectType,
		Byte::class.javaPrimitiveType!! to Byte::class.javaObjectType,
		Char::class.javaPrimitiveType!! to Char::class.javaObjectType,
		Short::class.javaPrimitiveType!! to Short::class.javaObjectType,
		Int::class.javaPrimitiveType!! to Int::class.javaObjectType,
		Long::class.javaPrimitiveType!! to Long::class.javaObjectType,
		Float::class.javaPrimitiveType!! to Float::class.javaObjectType,
		Double::class.javaPrimitiveType!! to Double::class.javaObjectType,
		Void.TYPE to Void::class.java,
	)

	@JvmStatic
	@Nullable
	fun get(type: Type): Codec<*>? {
		if (type is WildcardType) return get(type.upperBounds.single())
		val normalized = if (type is Class<*> && type.isPrimitive) PRIMITIVE_TO_BOXED[type] ?: type else type
		return cached(TYPE_CACHE, normalized, FieldCodecs::generateOrGetCodec)
	}

	@JvmStatic
	fun register(type: Type, codec: Codec<*>): Unit = synchronized(codecLock) {
		REGISTERED.putIfAbsent(type, codec)
		TYPE_CACHE.remove(type)
		CONTEXTUAL_TYPE_CACHE.remove(type)
	}

	/** Creates a value through the supplied factory, then restores its annotated saved fields. */
	@JvmStatic
	fun <T : Any> createCodec(factory: Supplier<T>): Codec<T> = SyncCodec(factory)

	@JvmStatic
	fun <T : Any> createStreamCodec(factory: Supplier<T>): StreamCodec<RegistryFriendlyByteBuf, T> = ByteBufCodecs.fromCodecWithRegistries(createCodec(factory))

	@JvmStatic
	fun <T : Any> registerFactory(type: Class<T>, factory: Supplier<T>): Unit = synchronized(codecLock) {
		FACTORIES[type] = factory
	}

	internal fun create(type: Class<*>): Any = synchronized(codecLock) {
		requireNotNull(FACTORIES[type]) { "No sync factory registered for ${type.name}" }
	}.get()

	@JvmStatic
	fun registerSupplier(type: Class<*>, supplier: Supplier<Codec<*>>): Unit = synchronized(codecLock) {
		REGISTERED_SUPPLIERS.putIfAbsent(type, supplier)
		TYPE_CACHE.clear()
	}

	@JvmStatic
	@Nullable
	fun getContextual(type: Type): ContextualFieldCodec<*>? {
		if (type is WildcardType) return getContextual(type.upperBounds.single())
		val normalized = if (type is Class<*> && type.isPrimitive) PRIMITIVE_TO_BOXED[type] ?: type else type
		return cached(CONTEXTUAL_TYPE_CACHE, normalized, FieldCodecs::generateOrGetContextualCodec)
	}

	@Nullable
	private fun generateOrGetContextualCodec(type: Type): ContextualFieldCodec<*>? {
		var registered = CONTEXTUAL_REGISTERED[type]
		if (registered != null) return registered

		val declaration = TypeDeclaration(type)
		val clazz = declaration.classValue ?: return null
		ownedContextualCodec(clazz)?.let { return it }
		registered = CONTEXTUAL_REGISTERED[clazz]
		if (registered != null) return registered
		if (REGISTERED.containsKey(type)) return null
		if (CollectionCodecs.isContainer(type)) return makeContextualContainerCodec(type)
		if (clazz.isArray) return makeContextualArrayCodec(clazz.componentType)
		for ((key, value) in CONTEXTUAL_REGISTERED) {
			if (key is Class<*> && key.isAssignableFrom(clazz)) {
				return value
			}
		}
		return if (ISyncManaged::class.java.isAssignableFrom(clazz)) ISyncManaged.CODEC else null
	}

	@JvmStatic
	fun registerContextual(type: Type, codec: ContextualFieldCodec<*>): Unit = synchronized(codecLock) {
		CONTEXTUAL_REGISTERED.putIfAbsent(type, codec)
		CONTEXTUAL_TYPE_CACHE.remove(type)
	}

	@Suppress("UNCHECKED_CAST")
	@JvmStatic
	@Nullable
	fun <T> getTyped(type: Type): Codec<T>? = get(type) as Codec<T>?

	@Nullable
	private fun generateOrGetCodec(type: Type): Codec<*>? {
		var registered = REGISTERED[type]
		if (registered != null) return registered

		if (type is ParameterizedType && type.rawType == Optional::class.java) {
			return get(type.actualTypeArguments[0])?.optionalFieldOf("value")?.codec()
		}

		val declaration = TypeDeclaration(type)
		val clazz = declaration.classValue ?: return null

		registered = REGISTERED[clazz]
		if (registered != null) return registered
		(ownedCodec(clazz) as? Codec<*>)?.let { return it }
		if (CollectionCodecs.isContainer(type)) {
			return CollectionCodecs.codec(type, null) { element, _, name ->
				@Suppress("UNCHECKED_CAST") // Element types are resolved from the actual container contract.
				(get(element) ?: throw IllegalArgumentException("Missing collection element Codec for $name: $element")) as Codec<Any?>
			}
		}

		if (clazz.isArray) return makeArrayCodec(clazz.componentType)
		if (clazz.isEnum) return makeEnumCodec(clazz)

		for ((key, value) in REGISTERED_SUPPLIERS) {
			if (key.isAssignableFrom(clazz)) return value.get()
		}
		for ((key, value) in REGISTERED) {
			if (key is Class<*> && key.isAssignableFrom(clazz)) {
				return value
			}
		}
		return null
	}

	@Suppress("UNCHECKED_CAST")
	@Nullable
	private fun makeArrayCodec(componentType: Class<*>): Codec<*>? {
		val elementCodec = get(componentType) ?: return null
		return Codec.list(elementCodec as Codec<Any>).xmap(
			{ values ->
				val array = ReflectArray.newInstance(componentType, values.size)
				for (i in values.indices) {
					ReflectArray.set(array, i, values[i])
				}
				array
			},
			{ array ->
				val values = ArrayList<Any>(ReflectArray.getLength(array))
				for (i in 0 until ReflectArray.getLength(array)) {
					values.add(ReflectArray.get(array, i))
				}
				values
			},
		)
	}

	private fun makeContextualContainerCodec(type: Type): ContextualFieldCodec<Any> = object : ContextualFieldCodec<Any> {
		override fun codec(context: ContextualFieldCodec.Context<Any>): Codec<Any> = requireNotNull(
			CollectionCodecs.codec(type, context.currentValue) { elementType, current, name ->
				elementCodec(elementType, current, context, "${context.fieldName}[$name]")
			},
		)
	}

	@Suppress("UNCHECKED_CAST") // Array element nulls use the same optional slots as other containers.
	private fun makeContextualArrayCodec(componentType: Class<*>): ContextualFieldCodec<*>? {
		if (getContextual(componentType) == null) return null
		return object : ContextualFieldCodec<Any> {
			override fun codec(context: ContextualFieldCodec.Context<Any>): Codec<Any> = sequenceCodec { index ->
				val current = context.currentValue?.takeIf { index < ReflectArray.getLength(it) }?.let { ReflectArray.get(it, index) }
				nullable(elementCodec(componentType, current, context, "${context.fieldName}[$index]") as Codec<Any>)
			}.xmap(
				{ values -> ReflectArray.newInstance(componentType, values.size).also { array -> values.forEachIndexed { index, value -> ReflectArray.set(array, index, value) } } },
				{ array -> (0 until ReflectArray.getLength(array)).map { ReflectArray.get(array, it) } },
			)
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun elementCodec(type: Type, current: Any?, parent: ContextualFieldCodec.Context<*>, fieldName: String): Codec<Any?> {
		val contextual = getContextual(type) as ContextualFieldCodec<Any>?
		val codec = contextual?.codec(nestedContext(parent, type, current, fieldName)) ?: getTyped(type)
			?: throw IllegalArgumentException("Sync: missing element Codec for $fieldName: $type")
		return codec as Codec<Any?>
	}

	@JvmStatic
	fun <T : Any> nullable(codec: Codec<T>): Codec<T?> = codec.optionalFieldOf("value").codec()
		.xmap({ it.orElse(null) }, { Optional.ofNullable(it) })

	private fun <T> nestedContext(parent: ContextualFieldCodec.Context<*>, type: Type, currentValue: T?, fieldName: String): ContextualFieldCodec.Context<T> = ContextualFieldCodec.Context(
		parent.holder, TypeDeclaration(type), currentValue, fieldName, parent.isClientSync, parent.isClientFullSyncUpdate,
		parent.lookup, parent.parseExplicitNull, parent.notifyUnchangedOnFullSync, parent.readOnly,
	)

	private fun <T> sequenceCodec(element: (Int) -> Codec<T>): Codec<List<T>> = object : Codec<List<T>> {
		override fun <D> decode(ops: DynamicOps<D>, input: D): DataResult<Pair<List<T>, D>> = ops.getStream(input).flatMap { stream ->
			val result = ObjectArrayList<T>()
			var decoded: DataResult<List<T>> = DataResult.success(result)
			stream.forEachOrdered { value ->
				val index = result.size
				decoded = decoded.flatMap { list ->
					element(index).parse(ops, value).map {
						result.add(it)
						list
					}
				}
			}
			decoded.map { Pair.of(it, ops.empty()) }
		}

		override fun <D> encode(input: List<T>, ops: DynamicOps<D>, prefix: D): DataResult<D> {
			val builder = ops.listBuilder()
			input.forEachIndexed { index, value -> builder.add(element(index).encodeStart(ops, value)) }
			return builder.build(prefix)
		}
	}

	@JvmStatic
	fun <T> registerStream(type: Type, codec: StreamCodec<RegistryFriendlyByteBuf, T>): Unit = synchronized(codecLock) {
		STREAM_CODECS[type] = codec
	}

	@Suppress("UNCHECKED_CAST") // Native ByteBuf codecs also accept the registry-aware ByteBuf subtype.
	@JvmStatic
	fun getStream(type: Type): StreamCodec<RegistryFriendlyByteBuf, *>? {
		if (type is WildcardType) return getStream(type.upperBounds.single())
		val normalized = if (type is Class<*> && type.isPrimitive) PRIMITIVE_TO_BOXED[type] ?: type else type
		return cached(STREAM_CODECS, normalized) {
			val codec = when (normalized) {
				Boolean::class.javaObjectType -> ByteBufCodecs.BOOL

				Byte::class.javaObjectType -> ByteBufCodecs.BYTE

				Short::class.javaObjectType -> ByteBufCodecs.SHORT

				Int::class.javaObjectType -> ByteBufCodecs.VAR_INT

				Long::class.javaObjectType -> ByteBufCodecs.VAR_LONG

				Float::class.javaObjectType -> ByteBufCodecs.FLOAT

				Double::class.javaObjectType -> ByteBufCodecs.DOUBLE

				String::class.java -> ByteBufCodecs.STRING_UTF8

				UUID::class.java -> UUIDUtil.STREAM_CODEC

				ResourceLocation::class.java -> ResourceLocation.STREAM_CODEC

				BlockPos::class.java -> BlockPos.STREAM_CODEC

				ItemStack::class.java -> ItemStack.OPTIONAL_STREAM_CODEC

				FluidStack::class.java -> FluidStack.OPTIONAL_STREAM_CODEC

				else -> Types.raw(normalized)?.let { ownedValue(it, "STREAM_CODEC") as? StreamCodec<*, *> }
					?: get(normalized)?.let { ByteBufCodecs.fromCodecWithRegistries(it) }
			}
			codec as StreamCodec<RegistryFriendlyByteBuf, *>?
		}
	}

	@Suppress("UNCHECKED_CAST", "rawtypes")
	private fun makeEnumCodec(clazz: Class<*>): Codec<*> {
		if (StringRepresentable::class.java.isAssignableFrom(clazz)) {
			val valuesByName = HashMap<String, Enum<*>>()
			for (value in clazz.enumConstants) {
				valuesByName[(value as StringRepresentable).serializedName] = value as Enum<*>
			}
			return Codec.STRING.xmap(
				{ name ->
					var value = valuesByName[name]
					if (value == null) {
						value = JavaEnum.valueOf(clazz as Class<out Enum<*>>, name)
					}
					value ?: throw IllegalArgumentException("No enum constant ${clazz.name}.$name")
				},
				{ value -> (value as StringRepresentable).serializedName },
			)
		}
		return Codec.STRING.xmap(
			{ name -> JavaEnum.valueOf(clazz as Class<out Enum<*>>, name) },
			{ value -> (value as Enum<*>).name },
		)
	}

	private fun <T> cached(cache: MutableMap<Type, T?>, type: Type, generate: (Type) -> T?): T? = synchronized(codecLock) {
		if (cache.containsKey(type)) return cache[type]
		generate(type).also { cache[type] = it }
	}

	private fun ownedCodec(type: Class<*>): Any? = ownedValue(type, "CODEC")

	private fun ownedValue(type: Class<*>, name: String): Any? = try {
		val field = type.getField(name)
		if (Modifier.isStatic(field.modifiers)) field.get(null) else null
	} catch (_: NoSuchFieldException) {
		null
	}

	private fun ownedContextualCodec(type: Class<*>): ContextualFieldCodec<*>? {
		val handle = try {
			MethodHandles.publicLookup().findStatic(type, "codec", MethodType.methodType(Codec::class.java, ContextualFieldCodec.Context::class.java))
		} catch (_: NoSuchMethodException) {
			return ownedCodec(type) as? ContextualFieldCodec<*>
		}
		return object : ContextualFieldCodec<Any> {
			@Suppress("UNCHECKED_CAST")
			override fun codec(context: ContextualFieldCodec.Context<Any>): Codec<Any> = handle.invoke(context) as Codec<Any>
		}
	}

	init {
		register(Int::class.javaObjectType, Codec.INT)
		register(Long::class.javaObjectType, Codec.LONG)
		register(Float::class.javaObjectType, Codec.FLOAT)
		register(Double::class.javaObjectType, Codec.DOUBLE)
		register(Short::class.javaObjectType, Codec.SHORT)
		register(Byte::class.javaObjectType, Codec.BYTE)
		register(Boolean::class.javaObjectType, Codec.BOOL)
		register(Char::class.javaObjectType, Codec.STRING.xmap({ value -> value[0] }, { value -> value.toString() }))

		register(IntArray::class.java, Codec.INT_STREAM.xmap(IntStream::toArray, Arrays::stream))
		register(LongArray::class.java, Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream))
		register(
			ByteArray::class.java,
			Codec.list(Codec.BYTE).xmap(
				{ values ->
					val array = ByteArray(values.size)
					for (i in values.indices) {
						array[i] = values[i]
					}
					array
				},
				{ array ->
					val values = ArrayList<Byte>(array.size)
					for (value in array) {
						values.add(value)
					}
					values
				},
			),
		)

		register(ResourceLocation::class.java, ResourceLocation.CODEC)
		register(String::class.java, Codec.STRING)
		register(UUID::class.java, UUIDUtil.CODEC)

		register(BlockPos::class.java, BlockPos.CODEC)
		register(BlockState::class.java, BlockState.CODEC)
		register(Component::class.java, ComponentSerialization.CODEC)
		register(ItemStack::class.java, ItemStack.OPTIONAL_CODEC)
		register(FluidStack::class.java, FluidStack.OPTIONAL_CODEC)
	}
}
