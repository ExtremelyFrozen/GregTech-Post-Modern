package com.gregtechceu.gtceu.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.WeightedMaterial;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.VoidingModeConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.machine.trait.BedrockOreMinerLogic;
import com.gregtechceu.gtceu.common.machine.trait.FluidDrillLogic;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;

import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BedrockOreMinerMachine extends WorkableElectricMultiblockMachine {

    private static final int PAGE_WIDTH = 190;
    private static final int PAGE_HEIGHT = 125;

    @Getter
    private final int tier;
    @Getter(AccessLevel.PACKAGE)
    private final ConditionalSubscriptionHandler displaySubscription;
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> displayText = List.of();

    public BedrockOreMinerMachine(BlockEntityCreationInfo info, int tier) {
        super(info, new BedrockOreMinerLogic());
        this.tier = tier;
        this.displaySubscription = new ConditionalSubscriptionHandler(this, this::refreshDisplayText,
                this::isFormed);
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
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            refreshDisplayText();
            displaySubscription.initialize(getLevel());
        }
    }

    @Override
    public void formStructure(String structureName) {
        super.formStructure(structureName);
        if (DEFAULT_STRUCTURE.equals(structureName) && !isRemote()) {
            refreshDisplayText();
            displaySubscription.updateSubscription();
        }
    }

    @Override
    public void invalidateStructure(String structureName) {
        super.invalidateStructure(structureName);
        if (DEFAULT_STRUCTURE.equals(structureName) && !isRemote()) {
            refreshDisplayText();
            displaySubscription.updateSubscription();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        clearDisplayRuntimeState();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        clearDisplayRuntimeState();
    }

    private void clearDisplayRuntimeState() {
        displayText = List.of();
        displaySubscription.unsubscribe();
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.addAll(displayText);
    }

    void refreshDisplayText() {
        List<Component> nextText = createDisplayText(captureDisplayState());
        if (!displayText.equals(nextText)) {
            displayText = nextText;
        }
    }

    protected DisplayState captureDisplayState() {
        if (!isFormed()) {
            return captureDisplayState(false, tier, null, 0, 0);
        }
        int energyTier = getEnergyTier();
        List<WeightedMaterial> veinMaterials = getRecipeLogic().getVeinMaterials();
        if (veinMaterials == null) {
            return captureDisplayState(true, energyTier, null, 0, 0);
        }
        return captureDisplayState(true, energyTier, veinMaterials, getRecipeLogic().getOreToProduce(),
                getLevel().tickRateManager().tickrate());
    }

    static DisplayState captureDisplayState(boolean formed, int energyTier,
                                            @Nullable List<WeightedMaterial> veinMaterials,
                                            int oreToProduce, float tickRate) {
        float orePerSecond = 0;
        if (formed && veinMaterials != null) {
            orePerSecond = oreToProduce * tickRate;
            orePerSecond = Mth.floor(orePerSecond / FluidDrillLogic.MAX_PROGRESS);
        }
        return new DisplayState(formed, energyTier, veinMaterials, orePerSecond);
    }

    static List<Component> createDisplayText(DisplayState state) {
        List<Component> text = new ArrayList<>();
        if (state.formed()) {
            long maxVoltage = GTValues.V[state.energyTier()];
            String voltageName = GTValues.VNF[state.energyTier()];
            text.add(Component.translatable("gtpm.multiblock.max_energy_per_tick", maxVoltage, voltageName));
            text.add(Component.translatable("gtpm.multiblock.ore_rig.drilled_ores_list")
                    .withStyle(ChatFormatting.GREEN));

            if (state.veinMaterials() != null) {
                for (WeightedMaterial entry : state.veinMaterials()) {
                    Component oreInfo = entry.material().getLocalizedName().withStyle(ChatFormatting.GREEN);
                    text.add(Component.translatable("gtpm.multiblock.ore_rig.drilled_ore_entry", oreInfo)
                            .withStyle(ChatFormatting.GRAY));
                }
                Component amountInfo = Component.literal(FormattingUtil.formatNumbers(state.orePerSecond()) + "/s")
                        .withStyle(ChatFormatting.BLUE);
                text.add(Component.translatable("gtpm.multiblock.ore_rig.ore_amount", amountInfo)
                        .withStyle(ChatFormatting.GRAY));
            } else {
                Component noOre = Component.translatable("gtpm.multiblock.fluid_rig.no_fluid_in_area")
                        .withStyle(ChatFormatting.RED);
                text.add(Component.translatable("gtpm.multiblock.ore_rig.drilled_ore_entry", noOre)
                        .withStyle(ChatFormatting.GRAY));
            }
        } else {
            Component tooltip = Component.translatable("gtpm.multiblock.invalid_structure.tooltip")
                    .withStyle(ChatFormatting.GRAY);
            text.add(Component.translatable("gtpm.multiblock.invalid_structure")
                    .withStyle(Style.EMPTY.withColor(ChatFormatting.RED)
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, tooltip))));
        }
        return List.copyOf(text);
    }

    record DisplayState(boolean formed, int energyTier, @Nullable List<WeightedMaterial> veinMaterials,
                        float orePerSecond) {

        DisplayState {
            if (veinMaterials != null) {
                veinMaterials = List.copyOf(veinMaterials);
            }
        }
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
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        UITemplate.setBackgroundTexture(root, GuiTextures.BACKGROUND_INVERSE);

        GTScrollerViewElement screen = new GTScrollerViewElement(4, 4, 182, 117);
        UITemplate.setBackgroundTexture(screen, getScreenTexture());
        screen.viewPort.layout(layout -> layout.paddingAll(0));
        UITemplate.setBackgroundTexture(screen.viewPort, getScreenTexture());
        screen.scrollerStyle(style -> style
                .mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));

        GTLabelElement title = new GTLabelElement(4, 5, 174, 10,
                getBlockState().getBlock().getDescriptionId(), true);
        title.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        screen.addScrollViewChild(title);
        screen.addScrollViewChild(new GTComponentPanelElement(4, 17,
                BedrockOreMinerMachine.this::addDisplayText)
                .setMaxWidthLimit(200)
                .clickHandler(BedrockOreMinerMachine.this::handleDisplayClick));
        root.addChild(screen);
        return root;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        VoidingModeConfigurator.attachConfigurators(configuratorPanel,
                BedrockOreMinerMachine.this);
        configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                BedrockOreMinerMachine.this));
    }
}
