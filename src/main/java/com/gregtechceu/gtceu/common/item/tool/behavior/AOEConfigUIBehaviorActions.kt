package com.gregtechceu.gtceu.common.item.tool.behavior

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.item.IGTTool
import com.gregtechceu.gtceu.api.item.datacomponents.AoESymmetrical
import com.gregtechceu.gtceu.api.item.tool.ToolHelper
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.data.GTDataComponents

import net.minecraft.world.item.ItemStack

/** Owns the wire protocol and server handler for the held-tool AOE configuration UI. */
object AOEConfigUIBehaviorActions {

	private val ACTION = GTCEu.id("set_tool_aoe")

	/** Creates a request using the registered method parameter codecs. */
	@JvmStatic
	fun createSetToolAOEAction(definition: AoESymmetrical): SyncActionData = RPCRegistry.create(ACTION, 0, definition)

	@RPCAction("set_tool_aoe")
	fun apply(context: SyncActionContext, definition: AoESymmetrical): Boolean {
		val stack = context.holder as? ItemStack ?: return false
		val openedStack = context.openedStack ?: return false
		if (!isAOEConfigTool(stack) || !hasConfigurableAOE(stack) || !hasConfigurableAOE(openedStack)) return false
		require(canApplyAOEDefinition(ToolHelper.getAoEDefinition(stack), definition)) { "AOE definition does not match the current tool." }
		stack.set(GTDataComponents.AOE, definition)
		return true
	}

	private fun hasConfigurableAOE(stack: ItemStack): Boolean = !ToolHelper.getAoEDefinition(stack).isZero()

	private fun isAOEConfigTool(stack: ItemStack): Boolean {
		val tool = stack.item as? IGTTool ?: return false
		return tool.toolStats.behaviors.any { behavior -> behavior is AOEConfigUIBehavior }
	}

	private fun isValidAOEDefinition(definition: AoESymmetrical): Boolean = definition.maxColumn() >= 0 &&
		definition.maxRow() >= 0 &&
		definition.maxLayer() >= 0 &&
		definition.column() >= 0 &&
		definition.column() <= definition.maxColumn() &&
		definition.row() >= 0 &&
		definition.row() <= definition.maxRow() &&
		definition.layer() >= 0 &&
		definition.layer() <= definition.maxLayer() &&
		!definition.isZero()

	private fun canApplyAOEDefinition(current: AoESymmetrical, requested: AoESymmetrical): Boolean = isValidAOEDefinition(current) &&
		isValidAOEDefinition(requested) &&
		current.maxColumn() == requested.maxColumn() &&
		current.maxRow() == requested.maxRow() &&
		current.maxLayer() == requested.maxLayer()
}
