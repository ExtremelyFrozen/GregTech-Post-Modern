package com.gregtechceu.gtceu.api.sync_system.rpc

/**
 * Exposes an instance method by its Java method name, including inherited and interface declarations.
 * The first RPCSender parameter is injected by the transport; remaining parameters use FieldCodecs.
 * Invocation runs on the receiving game thread. Return values are ignored, as in LDLib2 method RPC.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RPCMethod
