package com.gregtechceu.gtceu.api.multiblock.pattern.predicate

import com.gregtechceu.gtceu.api.capability.recipe.IO
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError
import com.gregtechceu.gtceu.data.lang.LangHandler
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.LongOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLists
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.network.chat.Component
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import java.util.function.Predicate
import java.util.function.Supplier

/** One block matching rule, including its limits and build metadata. */
open class PredicateRule {
    @JvmField
    var candidates: Supplier<Array<Block>>? = null

    @JvmField
    var blockInfo: Supplier<MultiblockBlockInfo?> = NULL_BLOCK_INFO

    @JvmField
    var predicate: Predicate<MultiblockState> = Predicate { false }

    @JvmField
    var toolTips: MutableList<Component>? = null

    @JvmField
    var minCount: Int = -1

    @JvmField
    var maxCount: Int = -1

    @JvmField
    var minLayerCount: Int = -1

    @JvmField
    var maxLayerCount: Int = -1

    @JvmField
    var previewCount: Int = -1

    @JvmField
    var disableRenderFormed: Boolean = false

    @JvmField
    var io: IO = IO.BOTH

    @JvmField
    var slotName: String? = null

    constructor()

    constructor(
        predicate: Predicate<MultiblockState>,
        blockInfo: Supplier<out MultiblockBlockInfo?>?,
        candidates: Supplier<Array<Block>>?,
    ) {
        this.predicate = predicate
        this.blockInfo = blockInfo?.let { supplier -> Supplier { supplier.get() } } ?: NULL_BLOCK_INFO
        this.candidates = candidates
    }

    constructor(predicate: Predicate<MultiblockState>, candidates: Supplier<Array<MultiblockBlockInfo>>?) : this(
        predicate,
        candidates?.let { supplier ->
            Supplier {
                val infos = supplier.get()
                if (infos.isEmpty()) MultiblockBlockInfo.EMPTY else infos[0]
            }
        },
        candidates?.let { supplier ->
            Supplier {
                supplier.get().map { it.blockState.block }.toTypedArray()
            }
        },
    )

    open fun buildPredicate(): PredicateRule = this

    @OnlyIn(Dist.CLIENT)
    fun getToolTips(predicates: PatternPredicate?): List<Component> {
        val result = ObjectArrayList<Component>()
        toolTips?.let(result::addAll)
        when {
            minCount == maxCount && maxCount != -1 ->
                result.add(Component.translatable("gtpm.multiblock.pattern.error.limited_exact", minCount))
            minCount != maxCount && minCount != -1 && maxCount != -1 ->
                result.add(Component.translatable("gtpm.multiblock.pattern.error.limited_within", minCount, maxCount))
            else -> {
                if (minCount != -1) {
                    result.add(LangHandler.getFromMultiLang("gtpm.multiblock.pattern.error.limited", 1, minCount))
                }
                if (maxCount != -1) {
                    result.add(LangHandler.getFromMultiLang("gtpm.multiblock.pattern.error.limited", 0, maxCount))
                }
            }
        }
        if (predicates == null) return result
        if (predicates.isSingle()) result.add(Component.translatable("gtpm.multiblock.pattern.single"))
        if (predicates.hasAir()) result.add(Component.translatable("gtpm.multiblock.pattern.replaceable_air"))
        return result
    }

    fun test(blockWorldState: MultiblockState): Boolean =
        predicate.test(blockWorldState) && checkInnerConditions(blockWorldState)

    fun testLimited(blockWorldState: MultiblockState): Boolean =
        testGlobal(blockWorldState) && testLayer(blockWorldState) && checkInnerConditions(blockWorldState)

    private fun checkInnerConditions(blockWorldState: MultiblockState): Boolean {
        if (disableRenderFormed) {
            blockWorldState.getFacts().getOrCreate("renderMask", Supplier { LongOpenHashSet() })
                .add(blockWorldState.pos.asLong())
        }
        if (io != IO.BOTH) {
            if (blockWorldState.io == IO.BOTH) {
                blockWorldState.io = io
            } else if (blockWorldState.io != io) {
                blockWorldState.io = null
            }
        }
        slotName?.let { name ->
            val slots: Long2ObjectMap<MutableSet<String>> = blockWorldState.getFacts().getOrCreate(
                "slots",
                Supplier { Long2ObjectArrayMap<MutableSet<String>>() },
            )
            slots.computeIfAbsent(blockWorldState.pos.asLong()) { ObjectOpenHashSet() }.add(name)
        }
        return true
    }

    fun testGlobal(blockWorldState: MultiblockState): Boolean {
        if (minCount == -1 && maxCount == -1) return true
        val base = predicate.test(blockWorldState)
        val count = blockWorldState.globalCount.mergeInt(this, if (base) 1 else 0, Integer::sum)
        if (maxCount == -1 || count <= maxCount) return base
        blockWorldState.setError(SinglePredicateError(this, 0))
        return false
    }

    fun testLayer(blockWorldState: MultiblockState): Boolean {
        if (minLayerCount == -1 && maxLayerCount == -1) return true
        val base = predicate.test(blockWorldState)
        val count = blockWorldState.layerCount.mergeInt(this, if (base) 1 else 0, Integer::sum)
        if (maxLayerCount == -1 || count <= maxLayerCount) return base
        blockWorldState.setError(SinglePredicateError(this, 2))
        return false
    }

    fun getCandidates(): List<ItemStack> {
        val source = candidates ?: return ObjectLists.emptyList()
        val result = ObjectArrayList<ItemStack>()
        source.get().forEach { block ->
            val item = toItem(block)
            if (item != Items.AIR) result.add(item.defaultInstance)
        }
        return ObjectLists.unmodifiable(result)
    }

    fun addCache(): Boolean = this !== any

    companion object {
        private val NULL_BLOCK_INFO = Supplier<MultiblockBlockInfo?> { null }

        @JvmField
        var any: PredicateRule = PredicateRule({ true }, null, null)

        @JvmField
        var air: PredicateRule = PredicateRule({ state -> state.blockState.isAir }, null, null)

        @JvmStatic
        fun toItem(block: Block): Item = if (block is LiquidBlock) {
            block.fluid.bucket
        } else {
            block.asItem()
        }
    }
}
