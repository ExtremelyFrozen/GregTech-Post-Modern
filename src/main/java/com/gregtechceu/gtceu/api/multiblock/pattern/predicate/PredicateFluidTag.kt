package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.material.Fluid
import java.util.function.Predicate
import java.util.function.Supplier

/** Matches fluid states contained in a registry tag. */
open class PredicateFluidTag(@JvmField var tag: TagKey<Fluid>?) : PredicateRule() {
    init {
        buildPredicate()
    }

    open override fun buildPredicate(): PredicateRule {
        val target = tag
        if (target == null) {
            predicate = Predicate { false }
            blockInfo = Supplier { MultiblockBlockInfo.fromBlock(Blocks.BARRIER) }
            candidates = Supplier { arrayOf(Blocks.BARRIER) }
            return this
        }
        predicate = Predicate { state -> state.blockState.fluidState.`is`(target) }
        val blocks = ObjectArrayList<Block>()
        BuiltInRegistries.FLUID.getTag(target).ifPresent { holders ->
            holders.forEach { holder ->
                blocks.add(holder.value().defaultFluidState().createLegacyBlock().block)
            }
        }
        if (blocks.isEmpty()) blocks.add(Blocks.BARRIER)
        val values = blocks.toTypedArray()
        candidates = Supplier { values }
        val info = MultiblockBlockInfo.fromBlock(values[0])
        blockInfo = Supplier { info }
        return this
    }
}
