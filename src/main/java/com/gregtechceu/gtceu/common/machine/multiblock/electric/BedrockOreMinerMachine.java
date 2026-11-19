package com.gregtechceu.gtceu.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.WeightedMaterial;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockDisplayText;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.trait.BedrockOreMinerLogic;
import com.gregtechceu.gtceu.common.machine.trait.FluidDrillLogic;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BedrockOreMinerMachine extends WorkableElectricMultiblockMachine {

    @Getter
    private final int tier;

    public BedrockOreMinerMachine(BlockEntityCreationInfo info, int tier) {
        super(info, new BedrockOreMinerLogic());
        this.tier = tier;
    }

    @NotNull
    @Override
    public BedrockOreMinerLogic getRecipeLogic() {
        return (BedrockOreMinerLogic) super.getRecipeLogic();
    }

    public int getEnergyTier() {
        var energyContainers = this.getCapabilitiesFlat(IO.IN, EURecipeCapability.CAP);
        if (energyContainers.isEmpty()) return this.tier;
        var energyCont = new EnergyContainerList(energyContainers.stream().filter(IEnergyContainer.class::isInstance)
                .map(IEnergyContainer.class::cast).toList());
        return Math.min(this.tier + 1,
                Math.max(this.tier, GTUtil.getFloorTierByVoltage(energyCont.getEffectiveVoltage())));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        boolean formed = isFormed();
        MultiblockDisplayText.builder(textList, formed);
        if (!formed) return;

        int energyTier = getEnergyTier();
        textList.add(Component.translatable("gtpm.multiblock.max_energy_per_tick",
                GTValues.V[energyTier], GTValues.VNF[energyTier]));
        textList.add(Component.translatable("gtpm.multiblock.ore_rig.drilled_ores_list")
                .withStyle(ChatFormatting.GREEN));

        var logic = getRecipeLogic();
        List<WeightedMaterial> veinMaterials = logic.getVeinMaterials();
        if (veinMaterials == null) {
            Component noOre = Component.translatable("gtpm.multiblock.fluid_rig.no_fluid_in_area")
                    .withStyle(ChatFormatting.RED);
            textList.add(Component.translatable("gtpm.multiblock.ore_rig.drilled_ore_entry", noOre)
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        for (WeightedMaterial entry : veinMaterials) {
            Component oreInfo = entry.material().getLocalizedName().withStyle(ChatFormatting.GREEN);
            textList.add(Component.translatable("gtpm.multiblock.ore_rig.drilled_ore_entry", oreInfo)
                    .withStyle(ChatFormatting.GRAY));
        }
        float tickRate = getLevel().tickRateManager().tickrate();
        float orePerSecond = Mth.floor(logic.getOreToProduce() * tickRate / FluidDrillLogic.MAX_PROGRESS);
        Component amountInfo = Component.literal(FormattingUtil.formatNumbers(orePerSecond) + "/s")
                .withStyle(ChatFormatting.BLUE);
        textList.add(Component.translatable("gtpm.multiblock.ore_rig.ore_amount", amountInfo)
                .withStyle(ChatFormatting.GRAY));
    }

    public static int getDepletionChance(int tier) {
        if (tier == GTValues.MV)
            return 1;
        if (tier == GTValues.HV)
            return 2;
        if (tier == GTValues.EV)
            return 8;
        return 1;
    }

    public static int getRigMultiplier(int tier) {
        if (tier == GTValues.MV)
            return 1;
        if (tier == GTValues.HV)
            return 4;
        if (tier == GTValues.EV)
            return 16;
        return 1;
    }

    public static Block getCasingState(int tier) {
        if (tier == GTValues.MV)
            return GTBlocks.CASING_STEEL_SOLID.get();
        if (tier == GTValues.HV)
            return GTBlocks.CASING_TITANIUM_STABLE.get();
        if (tier == GTValues.EV)
            return GTBlocks.CASING_TUNGSTENSTEEL_ROBUST.get();
        return GTBlocks.CASING_STEEL_SOLID.get();
    }

    @SuppressWarnings("DataFlowIssue")
    public static Block getFrameState(int tier) {
        if (tier == GTValues.MV)
            return GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, GTMaterials.Steel).get();
        if (tier == GTValues.HV)
            return GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, GTMaterials.Titanium).get();
        if (tier == GTValues.EV)
            return GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, GTMaterials.TungstenSteel).get();
        return GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, GTMaterials.Steel).get();
    }

    public static ResourceLocation getBaseTexture(int tier) {
        if (tier == GTValues.MV)
            return GTCEu.id("block/casings/solid/machine_casing_solid_steel");
        if (tier == GTValues.HV)
            return GTCEu.id("block/casings/solid/machine_casing_stable_titanium");
        if (tier == GTValues.EV)
            return GTCEu.id("block/casings/solid/machine_casing_robust_tungstensteel");
        return GTCEu.id("block/casings/solid/machine_casing_solid_steel");
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        VoidingModeConfigurator.attachConfigurators(configuratorPanel,
                BedrockOreMinerMachine.this);
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                BedrockOreMinerMachine.this));
    }
}
