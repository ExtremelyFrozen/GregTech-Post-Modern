package com.gregtechceu.gtceu.api.multiblock.structurepredicate

import com.gregtechceu.gtceu.GTCEu
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility
import com.gregtechceu.gtceu.api.multiblock.MultiblockState
import com.gregtechceu.gtceu.config.ConfigHolder

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectList

@JvmRecord
data class AutoMaintenancePredicate(val checkMaintenance: Boolean, val checkMuffler: Boolean, val checkParallel: Boolean) : AlternativePredicate {
	override fun type(): StructurePredicateType<*> = TYPE
	override fun alternatives(state: MultiblockState?): ObjectList<StructurePredicate> = ObjectArrayList<StructurePredicate>().also { result ->
		if (checkMaintenance) result.add(if (ConfigHolder.INSTANCE.machines.enableMaintenance) MAINTENANCE_ENABLED else MAINTENANCE_DISABLED)
		if (checkMuffler) result.add(MUFFLER)
		if (checkParallel) result.add(PARALLEL)
	}

	companion object {
		val TYPE: StructurePredicateType<AutoMaintenancePredicate> by lazy { StructurePredicateType.register(GTCEu.id("auto_maintenance"), CODEC) }
		private val MUFFLER = RestrictedPredicate.builder().base(AbilityPredicate(ObjectList.of(PartAbility.MUFFLER))).exactCount(1).build()
		private val PARALLEL = RestrictedPredicate.builder().base(AbilityPredicate(ObjectList.of(PartAbility.PARALLEL_HATCH))).maxCount(1).previewCount(1).build()
		private val MAINTENANCE_ENABLED = RestrictedPredicate.builder().base(AbilityPredicate(ObjectList.of(PartAbility.MAINTENANCE))).minCount(1).maxCount(1).build()
		private val MAINTENANCE_DISABLED = RestrictedPredicate.builder().base(AbilityPredicate(ObjectList.of(PartAbility.MAINTENANCE))).minCount(0).maxCount(1).build()

		@JvmField
		val CODEC: MapCodec<AutoMaintenancePredicate> = RecordCodecBuilder.mapCodec { instance ->
			instance.group(
				Codec.BOOL.optionalFieldOf("checkMaintenance", true).forGetter(AutoMaintenancePredicate::checkMaintenance),
				Codec.BOOL.optionalFieldOf("checkMuffler", false).forGetter(AutoMaintenancePredicate::checkMuffler),
				Codec.BOOL.optionalFieldOf("checkParallel", true).forGetter(AutoMaintenancePredicate::checkParallel),
			).apply(instance, ::AutoMaintenancePredicate)
		}
	}
}
