package com.gregtechceu.gtceu.api.sync_system.rpc

/**
 * Declares a server action whose ordinary parameters use the shared type codecs.
 *
 * Register the owning class on both physical sides. [value] is a GT action path; method
 * names are not part of the wire format. SyncActionContext and ServerPlayer parameters are
 * supplied by the server and never accepted from the client. Methods run on the server thread
 * after the holder packet has validated its interaction and session boundaries. Instance methods
 * run on the resolved holder; static methods can use the supplied context for item or feature
 * operations. A void method completes the request; boolean methods can reject its current state.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RPCAction(val value: String)
