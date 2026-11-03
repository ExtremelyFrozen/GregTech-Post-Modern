package com.gregtechceu.gtceu.api.sync_system.rpc

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Collections

/**
 * Registers annotated action methods and shares their parameter schema between sender and receiver.
 * Reflection is confined to this communication boundary; business methods receive decoded values.
 */
object RPCRegistry {

	private val actions = Collections.synchronizedMap(Object2ObjectOpenHashMap<ResourceLocation, Endpoint>())
	private val owners = ReferenceOpenHashSet<Any>()

	/** Registers the public annotated methods of one common-side action object exactly once. */
	@JvmStatic
	fun register(owner: Any): Unit = synchronized(actions) {
		if (owner in owners) return
		for (method in owner.javaClass.declaredMethods) {
			val annotation = method.getAnnotation(RPCAction::class.java) ?: continue
			val id = GTCEu.id(annotation.value)
			val endpoint = Endpoint(owner, method)
			require(actions.putIfAbsent(id, endpoint) == null) { "Duplicate RPC action: $id" }
		}
		owners.add(owner)
	}

	/** Captures ordinary arguments; server context parameters are supplied only at execution. */
	@JvmStatic
	fun create(actionId: ResourceLocation, sequence: Int, vararg args: Any): SyncActionData = SyncActionData(actionId, sequence, args.copyOf())

	/** Decodes each parameter once using the registered method's declared generic type. */
	@JvmStatic
	fun decode(actionId: ResourceLocation, buffer: RegistryFriendlyByteBuf): Array<Any> = endpoint(actionId).decode(buffer)

	/** Encodes arguments using the same schema selected by the action id on the receiver. */
	@JvmStatic
	fun encode(actionId: ResourceLocation, buffer: RegistryFriendlyByteBuf, args: Array<out Any>) {
		endpoint(actionId).encode(buffer, args)
	}

	/** Invokes an action after the packet resolved and authorized its server-owned holder. */
	@JvmStatic
	fun dispatch(context: SyncActionContext): Boolean = try {
		endpoint(context.actionId()).invoke(context)
	} catch (exception: ReflectiveOperationException) {
		val cause = if (exception is InvocationTargetException) exception.targetException else exception
		GTCEu.LOGGER.error("RPC action {} from {} failed", context.actionId(), context.player.gameProfile.name, cause)
		false
	} catch (exception: RuntimeException) {
		GTCEu.LOGGER.error("RPC action {} from {} failed", context.actionId(), context.player.gameProfile.name, exception)
		false
	}

	private fun endpoint(actionId: ResourceLocation): Endpoint = actions[actionId] ?: throw IllegalArgumentException("Unknown RPC action: $actionId")

	private class Endpoint(val owner: Any, val method: Method) {
		init {
			require(method.returnType == Boolean::class.javaPrimitiveType) { "RPC method must return whether it executed: $method" }
		}

		private val parameterTypes = method.parameterTypes
		private val argumentTypes = method.genericParameterTypes.filterIndexed { index, _ ->
			parameterTypes[index] != SyncActionContext::class.java && parameterTypes[index] != ServerPlayer::class.java
		}

		// Registration can precede common codec initialization; the first packet resolves and caches codecs.
		private val codecs by lazy {
			argumentTypes.map { type ->
				// Each value is paired with the codec resolved from the corresponding method parameter.
				@Suppress("UNCHECKED_CAST")
				(FieldCodecs.getStream(type) ?: throw IllegalArgumentException("No RPC codec for $type in $method")) as StreamCodec<RegistryFriendlyByteBuf, Any>
			}
		}

		fun decode(buffer: RegistryFriendlyByteBuf): Array<Any> = Array(codecs.size) { codecs[it].decode(buffer) }

		fun encode(buffer: RegistryFriendlyByteBuf, args: Array<out Any>) {
			codecs.forEachIndexed { index, codec -> codec.encode(buffer, args[index]) }
		}

		fun invoke(context: SyncActionContext): Boolean {
			var argumentIndex = 0
			val arguments = Array(parameterTypes.size) { index ->
				when (parameterTypes[index]) {
					SyncActionContext::class.java -> context
					ServerPlayer::class.java -> context.player
					else -> context.action.payload[argumentIndex++]
				}
			}
			return method.invoke(owner, *arguments) as Boolean
		}
	}
}
