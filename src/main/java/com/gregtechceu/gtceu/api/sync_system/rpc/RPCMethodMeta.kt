package com.gregtechceu.gtceu.api.sync_system.rpc

import com.gregtechceu.gtceu.api.sync_system.FieldCodecs

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Cached communication schema. No managed instance is retained by class metadata. */
class RPCMethodMeta(private val method: Method) {
	private val hasSender = method.parameterTypes.firstOrNull() == RPCSender::class.java
	private val argumentTypes = method.genericParameterTypes.drop(if (hasSender) 1 else 0)
	private val codecs by lazy {
		argumentTypes.map { type ->
			// FieldCodecs returns the schema belonging to this exact declared parameter type.
			@Suppress("UNCHECKED_CAST")
			(FieldCodecs.getStream(type) ?: error("No RPC codec for $type in $method")) as StreamCodec<RegistryFriendlyByteBuf, Any>
		}
	}

	init {
		method.trySetAccessible()
	}

	fun serializeArgs(buffer: RegistryFriendlyByteBuf, args: Array<out Any>) {
		codecs.forEachIndexed { index, codec -> codec.encode(buffer, args[index]) }
	}

	fun invoke(instance: Any?, sender: RPCSender, buffer: RegistryFriendlyByteBuf) {
		val values = Array(codecs.size) { codecs[it].decode(buffer) }
		method.invoke(instance, *(if (hasSender) arrayOf(sender, *values) else values))
	}

	companion object {
		private val CACHE = object : ClassValue<Map<String, RPCMethodMeta>>() {
			override fun computeValue(type: Class<*>): Map<String, RPCMethodMeta> {
				val methods = Object2ObjectOpenHashMap<String, RPCMethodMeta>()
				type.superclass?.let { methods.putAll(get(it)) }
				for (contract in type.interfaces) methods.putAll(get(contract))
				for (method in type.declaredMethods) {
					if (!Modifier.isStatic(method.modifiers) && method.isAnnotationPresent(RPCMethod::class.java)) methods[method.name] = RPCMethodMeta(method)
				}
				return methods
			}
		}

		@JvmStatic
		fun getMethods(type: Class<*>): Map<String, RPCMethodMeta> = CACHE.get(type)
	}
}
