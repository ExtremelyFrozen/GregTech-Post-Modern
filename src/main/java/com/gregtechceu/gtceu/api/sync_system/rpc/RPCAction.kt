package com.gregtechceu.gtceu.api.sync_system.rpc

/**
 * Declares a server action whose ordinary parameters use the shared type codecs.
 *
 * Register the owning object on both physical sides. [value] is a GT action path; method
 * names are not part of the wire format. SyncActionContext and ServerPlayer parameters are
 * supplied by the server and never accepted from the client. Methods run on the server thread
 * after the holder packet has validated its interaction and session boundaries. Return `true`
 * when the request was handled, or `false` when the current holder or business state rejects it.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RPCAction(val value: String)
