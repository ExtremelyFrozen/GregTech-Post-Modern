package com.gregtechceu.gtceu.common.machine.storage

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.GTValues
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry

import org.jetbrains.annotations.ApiStatus

/** Exposes only the creative energy mutations that Kotlin server action handlers may invoke. */
@ApiStatus.Internal
interface CreativeEnergyActionTarget {

	/** Applies one validated voltage and updates its derived tier and client sync state. */
	fun setCreativeEnergyVoltage(voltage: Long)

	/** Applies one validated tier and updates its derived voltage and client sync state. */
	fun setCreativeEnergyTier(tier: Int)
}

/** Owns the wire protocol and server handlers for creative energy voltage and tier actions. */
object CreativeEnergyContainerMachineActions {

	@JvmStatic
	fun createSetVoltageAction(voltage: Long): SyncActionData = RpcRegistry.create(GTCEu.id("set_creative_energy_voltage"), voltage.hashCode(), voltage)

	@JvmStatic
	fun createSetTierAction(tier: Int): SyncActionData = RpcRegistry.create(GTCEu.id("set_creative_energy_tier"), tier, tier)

	@RpcAction("set_creative_energy_voltage")
	fun setVoltage(context: SyncActionContext, voltage: Long): Boolean {
		val target = context.holder as? CreativeEnergyActionTarget ?: return false
		require(voltage >= 0) { "Creative energy voltage must be non-negative: $voltage" }
		target.setCreativeEnergyVoltage(voltage)
		return true
	}

	@RpcAction("set_creative_energy_tier")
	fun setTier(context: SyncActionContext, tier: Int): Boolean {
		val target = context.holder as? CreativeEnergyActionTarget ?: return false
		require(tier in GTValues.VNF.indices) { "Creative energy tier is out of range: $tier" }
		target.setCreativeEnergyTier(tier)
		return true
	}
}
