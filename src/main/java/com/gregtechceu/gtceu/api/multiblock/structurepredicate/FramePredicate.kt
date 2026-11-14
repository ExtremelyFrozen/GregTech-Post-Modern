package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.data.chemical.material.Material
import com.gregtechceu.gtceu.api.data.tag.TagPrefix
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.api.pipenet.IPipeNode
import com.gregtechceu.gtceu.api.registry.GTRegistries
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks
import com.gregtechceu.gtceu.common.registry.GTRegistration.REGISTRATE

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.common.util.Lazy

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectList

/** Resolves frame blocks lazily and also accepts pipe nodes carrying one of the same material frames. */
class FramePredicate(val materialIds: ObjectList<ResourceLocation>) : StructurePredicate {
	private val resolvedMaterials = Lazy.of { materialIds.stream().map(::resolveMaterial).collect(ObjectImmutableList.toList()) }
	private val resolvedFrameBlocks = Lazy.of { materials.stream().map(::resolveFrameBlock).collect(ObjectImmutableList.toList()) }
	val materials: ObjectList<Material> get() = resolvedMaterials.get()
	val frameBlocks: ObjectList<Block> get() = resolvedFrameBlocks.get()

	override fun type(): StructurePredicateType<*> = TYPE
	override fun observeCandidate(candidateState: BlockState): PatternObservation = PatternObservation.of(frameBlocks.contains(candidateState.block), ObjectList.of("frame"))
	override fun observePosition(state: MultiblockState): PatternObservation = PatternObservation.of(
		frameBlocks.contains(state.blockState.block) || (state.blockEntity as? IPipeNode<*, *>)?.let { materials.contains(it.frameMaterial) } == true,
		ObjectList.of("frame"),
	)
	override fun test(multiblockState: MultiblockState, mutateCount: Boolean): Boolean = observePosition(multiblockState).matched
	override fun candidates(): ObjectList<MultiblockBlockInfo> = frameBlocks.stream().map(MultiblockBlockInfo::fromBlock).collect(ObjectImmutableList.toList())
	override fun blockCandidates(): ObjectList<Block> = frameBlocks

	private fun resolveMaterial(id: ResourceLocation): Material {
		val material = GTRegistries.MATERIALS.getMaterial(id)
		if (material.isNull) throw IllegalStateException("Unknown frame material: $id")
		return material
	}
	private fun resolveFrameBlock(material: Material): Block {
		val entry = GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, material)
			?: throw IllegalStateException("No frameGt block registered for material ${material.resourceLocation}")
		if (!entry.isBound) throw IllegalStateException("FrameGt block is not bound for material ${material.resourceLocation}")
		return entry.get()
	}

	companion object {
		val TYPE: StructurePredicateType<FramePredicate> by lazy { REGISTRATE.structurePredicate("frame", CODEC) }

		@JvmField
		val CODEC: MapCodec<FramePredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(PredicateCodecs.oneOrMore(GTCEu.GTCEU_ID).fieldOf("materials").forGetter(FramePredicate::materialIds))
				.apply(instance, ::FramePredicate)
		}
	}
}
