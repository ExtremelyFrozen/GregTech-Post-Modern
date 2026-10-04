package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import java.util.function.Predicate
import java.util.function.Supplier

/** Matches blocks contained in a registry tag. */
open class PredicateBlockTag(@JvmField var tag: TagKey<Block>?) : PredicateRule() {
    init {
        buildPredicate()
    }

    open override fun buildPredicate(): PredicateRule {
        val target = tag
        if (target == null) {
            predicate = Predicate { false }
            blockInfo = Supplier { MultiblockBlockInfo.EMPTY }
            candidates = Supplier { arrayOf(Blocks.AIR) }
            return this
        }
        predicate = Predicate { state -> state.blockState.`is`(target) }
        val blocks = ObjectArrayList<Block>()
        BuiltInRegistries.BLOCK.getTag(target).ifPresent { holders ->
            holders.forEach { blocks.add(it.value()) }
        }
        if (blocks.isEmpty()) blocks.add(Blocks.BARRIER)
        val values = blocks.toTypedArray()
        candidates = Supplier { values }
        val info = MultiblockBlockInfo.fromBlock(values[0])
        blockInfo = Supplier { info }
        return this
    }
}
