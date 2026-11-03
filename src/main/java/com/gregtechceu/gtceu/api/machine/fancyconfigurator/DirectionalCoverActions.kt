package com.gregtechceu.gtceu.api.machine.fancyconfigurator

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.capability.ICoverable
import com.gregtechceu.gtceu.api.gui.factory.CoverUIHelper
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer
import com.gregtechceu.gtceu.api.machine.MetaMachine
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine
import com.gregtechceu.gtceu.api.sync_system.SyncActionContext
import com.gregtechceu.gtceu.api.sync_system.SyncActionData
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCAction
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCRegistry
import com.gregtechceu.gtceu.common.item.behavior.CoverPlaceBehavior

import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack

/**
 * Creates and handles GT machine actions for covers selected from an LDLib2 directional page.
 *
 * The server resolves the cover definition exclusively from the player's current carried stack. Client payloads only
 * identify a machine side and therefore cannot select or synthesize a cover item.
 */
object DirectionalCoverActions {

	@JvmStatic
	fun createPlaceCoverAction(side: Direction): SyncActionData = RPCRegistry.create(GTCEu.id("place_directional_cover"), side.get3DDataValue(), side)

	@JvmStatic
	fun createRemoveCoverAction(side: Direction): SyncActionData = RPCRegistry.create(GTCEu.id("remove_directional_cover"), side.get3DDataValue(), side)

	@JvmStatic
	fun createOpenCoverAction(side: Direction): SyncActionData = RPCRegistry.create(GTCEu.id("open_directional_cover"), side.get3DDataValue(), side)

	@RPCAction("place_directional_cover")
	fun placeCover(context: SyncActionContext, side: Direction): Boolean {
		val coverable = coverable(context) ?: return false
		val player = context.player
		val carried = player.containerMenu.carried
		val definition = CoverPlaceBehavior.findCoverDefinition(carried) ?: return false
		val previousCover = coverable.getCoverAtSide(side)
		if (previousCover == null) {
			check(coverable.placeCoverOnSide(side, carried, definition, player)) { "Directional cover placement was rejected." }
			if (!player.isCreative) carried.shrink(1)
			player.containerMenu.broadcastChanges()
			return true
		}
		if (carried.count != 1 || ItemStack.isSameItemSameComponents(carried, previousCover.pickItem)) return false
		val replacementCover = definition.createCoverBehavior(coverable, side)
		check(coverable.canPlaceCoverOnSide(definition, side) && replacementCover.canAttach()) { "Directional cover replacement failed its placement rules." }
		val previousAttachItem = previousCover.pickItem.copy()
		check(coverable.replaceCoverOnSide(side, previousCover, replacementCover, carried, player)) { "Directional cover replacement detected changed state." }
		player.containerMenu.setCarried(previousAttachItem)
		player.containerMenu.broadcastChanges()
		return true
	}

	@RPCAction("remove_directional_cover")
	fun removeCover(context: SyncActionContext, side: Direction): Boolean {
		val coverable = coverable(context) ?: return false
		val player = context.player
		if (!player.containerMenu.carried.isEmpty) return false
		val cover = coverable.getCoverAtSide(side) ?: return false
		val attachItem = cover.pickItem.copy()
		check(coverable.removeCover(false, side, player)) { "Directional cover removal was rejected." }
		player.containerMenu.setCarried(attachItem)
		player.containerMenu.broadcastChanges()
		return true
	}

	@RPCAction("open_directional_cover")
	fun openCover(context: SyncActionContext, side: Direction): Boolean {
		val coverable = coverable(context) ?: return false
		val cover = coverable.getCoverAtSide(side) ?: return false
		if (!CoverUIHelper.canOpen(cover, context.player)) return false
		check(CoverUIHelper.open(cover, context.player)) { "Directional cover UI failed to open." }
		return true
	}

	private fun coverable(context: SyncActionContext): ICoverable? {
		val machine = context.holder
		if (machine !is MetaMachine || machine !is FancyActionMachine) return null
		return machine.getTrait(MachineCoverContainer.TYPE)
	}
}
