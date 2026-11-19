package com.gregtechceu.gtceu.api.sync_system.rpc

/** Registers a static RPC during mod discovery. A nonempty modId enables an optional integration only when loaded. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RPCPacket(val value: String, val modId: String = "")
