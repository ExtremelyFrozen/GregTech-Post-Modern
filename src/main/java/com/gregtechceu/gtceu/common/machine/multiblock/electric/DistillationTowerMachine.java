package com.gregtechceu.gtceu.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTComponentPanelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.ActionResult;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeData;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.VoidFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DistillationTowerMachine extends WorkableElectricMultiblockMachine
                                      implements FluidRecipeCapability.ICustomParallel {

    private static final int PAGE_WIDTH = 190;
    private static final int PAGE_HEIGHT = 125;

    @Getter
    private @Nullable List<IFluidHandler> fluidOutputs;
    @Getter
    @Nullable
    private IFluidHandler firstValid = null;
    private final int yOffset;
    /**
     * Keeps the client display supplied from a server-owned snapshot while the structure is formed.
     */
    @Getter(AccessLevel.PACKAGE)
    private final ConditionalSubscriptionHandler displaySnapshotSubscription;
    /**
     * Carries the complete legacy display text without relying on the old Widget sync path.
     */
    @Getter(AccessLevel.PACKAGE)
    @SyncToClient
    private List<Component> displaySnapshot = List.of();

    public DistillationTowerMachine(BlockEntityCreationInfo info) {
        this(info, 1);
    }

    /**
     * Construct DT Machine
     *
     * @param yOffset The Y difference between the controller and the first fluid output
     */
    public DistillationTowerMachine(BlockEntityCreationInfo info, int yOffset) {
        super(info, new DistillationTowerLogic());
        this.yOffset = yOffset;
        this.displaySnapshotSubscription = new ConditionalSubscriptionHandler(this, this::refreshDisplaySnapshot,
                this::isFormed);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            refreshDisplaySnapshot();
            displaySnapshotSubscription.initialize(getLevel());
        }
    }

    @Override
    public DistillationTowerLogic getRecipeLogic() {
        return (DistillationTowerLogic) super.getRecipeLogic();
    }

    @Override
    public void formStructure(String structureName) {
        super.formStructure(structureName);
        if (!DEFAULT_STRUCTURE.equals(structureName)) return;
        final int startY = getBlockPos().getY() + yOffset;
        List<IMultiPart> parts = getParts().stream()
                .filter(part -> PartAbility.EXPORT_FLUIDS.isApplicable(part.self().getBlockState().getBlock()))
                .filter(part -> part.self().getBlockPos().getY() >= startY)
                .toList();

        if (!parts.isEmpty()) {
            // Loop from controller y + offset -> the highest output hatch
            int maxY = parts.get(parts.size() - 1).self().getBlockPos().getY();
            fluidOutputs = new ObjectArrayList<>(maxY - startY);
            int outputIndex = 0;
            for (int y = startY; y <= maxY; ++y) {
                if (parts.size() <= outputIndex) {
                    fluidOutputs.add(VoidFluidHandler.INSTANCE);
                    continue;
                }

                var part = parts.get(outputIndex);
                if (part.self().getBlockPos().getY() == y) {
                    var handler = part.getRecipeHandlers().get(0).getCapability(FluidRecipeCapability.CAP)
                            .stream()
                            .filter(IFluidHandler.class::isInstance)
                            .findFirst()
                            .map(IFluidHandler.class::cast)
                            .orElse(VoidFluidHandler.INSTANCE);
                    addOutput(handler);
                    outputIndex++;
                } else if (part.self().getBlockPos().getY() > y) {
                    fluidOutputs.add(VoidFluidHandler.INSTANCE);
                } else {
                    GTCEu.LOGGER.error(
                            "The Distillation Tower at {} has a fluid export hatch with an unexpected Y position",
                            getBlockPos());
                    invalidateStructure(structureName);
                    return;
                }
            }
            if (!isRemote()) {
                refreshDisplaySnapshot();
                displaySnapshotSubscription.updateSubscription();
            }
        } else invalidateStructure(structureName);
    }

    private void addOutput(IFluidHandler handler) {
        fluidOutputs.add(handler);
        if (firstValid == null && handler != VoidFluidHandler.INSTANCE) firstValid = handler;
    }

    @Override
    public void invalidateStructure(String structureName) {
        if (DEFAULT_STRUCTURE.equals(structureName)) {
            fluidOutputs = null;
            firstValid = null;
        }
        super.invalidateStructure(structureName);
        if (DEFAULT_STRUCTURE.equals(structureName) && !isRemote()) {
            refreshDisplaySnapshot();
            displaySnapshotSubscription.updateSubscription();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        resetDisplaySnapshot();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        resetDisplaySnapshot();
    }

    /**
     * Clears runtime display state when the controller can no longer serve its UI.
     */
    private void resetDisplaySnapshot() {
        displaySnapshot = List.of();
        displaySnapshotSubscription.unsubscribe();
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        textList.addAll(displaySnapshot);
    }

    /**
     * Rebuilds the immutable client snapshot from the server's complete legacy display contract.
     */
    void refreshDisplaySnapshot() {
        List<Component> nextSnapshot = new ArrayList<>();
        collectServerDisplayText(nextSnapshot);
        nextSnapshot = List.copyOf(nextSnapshot);
        if (!displaySnapshot.equals(nextSnapshot)) {
            displaySnapshot = nextSnapshot;
        }
    }

    /**
     * Separates live server-state collection from snapshot publication for direct lifecycle verification.
     */
    protected void collectServerDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
    }

    /**
     * Owns the opening-scoped controller, optional machine-mode, directional, and contextual part pages.
     */

    @Override
    public int limitFluidParallel(GTRecipe recipe, int multiplier, boolean tick) {
        int minMultiplier = 0;
        int maxMultiplier = multiplier;

        var contents = (tick ? recipe.tickInputs : recipe.inputs).get(FluidRecipeCapability.CAP);
        if (contents == null || contents.isEmpty()) return multiplier;

        int maxAmount = contents.stream()
                .map(Content::getContent)
                .map(FluidRecipeCapability.CAP::of)
                .filter(i -> !i.ingredient().hasNoFluids())
                .mapToInt(SizedFluidIngredient::amount)
                .max()
                .orElse(0);

        if (maxAmount == 0) return multiplier;
        if (multiplier > Integer.MAX_VALUE / maxAmount) {
            maxMultiplier = multiplier = Integer.MAX_VALUE / maxAmount;
        }

        while (minMultiplier != maxMultiplier) {
            GTRecipe copy = modifyOutputs(recipe, ContentModifier.multiplier(multiplier));
            boolean filled = getRecipeLogic().applyFluidOutputs(copy, FluidAction.SIMULATE, getVoidingMode());
            int[] bin = ParallelLogic.adjustMultiplier(filled, minMultiplier, multiplier, maxMultiplier);
            minMultiplier = bin[0];
            multiplier = bin[1];
            maxMultiplier = bin[2];
        }
        return multiplier;
    }

    private static GTRecipe modifyOutputs(GTRecipe recipe, ContentModifier cm) {
        return new GTRecipe(recipe.recipeType, recipe.id, recipe.inputs, cm.applyContents(recipe.outputs),
                recipe.tickInputs, cm.applyContents(recipe.tickOutputs), recipe.inputChanceLogics,
                recipe.outputChanceLogics,
                recipe.tickInputChanceLogics, recipe.tickOutputChanceLogics, recipe.conditions,
                RecipeData.copy(recipe.data), recipe.tier, recipe.duration, recipe.recipeCategory, recipe.groupColor);
    }

    public static class DistillationTowerLogic extends RecipeLogic {

        @Nullable
        @SaveField
        @SyncToClient
        GTRecipe workingRecipe = null;

        public DistillationTowerLogic() {
            super();
        }

        @Override
        public DistillationTowerMachine getMachine() {
            return (DistillationTowerMachine) super.getMachine();
        }

        // Copy of lastRecipe with fluid outputs trimmed, for output displays like Jade or GUI text
        @Override
        public @Nullable GTRecipe getLastRecipe() {
            return workingRecipe;
        }

        @Override
        protected ActionResult matchRecipe(GTRecipe recipe) {
            var match = matchDTRecipe(recipe);
            if (!match.isSuccess()) return match;

            return RecipeHelper.matchTickRecipe(getMachine(), recipe);
        }

        @Override
        protected void handleSearchingRecipes(Iterator<GTRecipe> matches) {
            workingRecipe = null;
            super.handleSearchingRecipes(matches);
        }

        private ActionResult matchDTRecipe(GTRecipe recipe) {
            var result = RecipeHelper.handleRecipe(getMachine(), recipe, IO.IN, recipe.inputs,
                    Collections.emptyMap(), false, true);
            if (!result.isSuccess()) return result;

            var items = recipe.getOutputContents(ItemRecipeCapability.CAP);
            if (!items.isEmpty()) {
                Map<RecipeCapability<?>, List<Content>> out = Map.of(ItemRecipeCapability.CAP, items);
                result = RecipeHelper.handleRecipe(getMachine(), recipe, IO.OUT, out, Collections.emptyMap(), false,
                        true);
                if (!result.isSuccess()) return result;
            }

            if (!applyFluidOutputs(recipe, FluidAction.SIMULATE, getMachine().getVoidingMode())) {
                return ActionResult.fail(Component.translatable("gtceu.recipe_logic.insufficient_out")
                        .append(": ")
                        .append(FluidRecipeCapability.CAP.getName()), FluidRecipeCapability.CAP, IO.OUT);
            }

            return ActionResult.SUCCESS;
        }

        private void updateWorkingRecipe(GTRecipe recipe) {
            if (recipe.recipeType == GTRecipeTypes.DISTILLERY_RECIPES) {
                this.workingRecipe = recipe;
                syncDataHolder.markClientSyncFieldDirty("workingRecipe");
                return;
            }

            this.workingRecipe = recipe.copy();
            var contents = recipe.getOutputContents(FluidRecipeCapability.CAP);
            var outputs = getMachine().getFluidOutputs();
            List<Content> trimmed = new ArrayList<>(12);
            for (int i = 0; i < Math.min(contents.size(), outputs.size()); ++i) {
                if (!(outputs.get(i) instanceof VoidFluidHandler)) trimmed.add(contents.get(i));
            }
            this.workingRecipe.outputs.put(FluidRecipeCapability.CAP, trimmed);
            syncDataHolder.markClientSyncFieldDirty("workingRecipe");
        }

        @Override
        protected ActionResult handleRecipeIO(GTRecipe recipe, IO io) {
            if (io != IO.OUT) {
                var handleIO = super.handleRecipeIO(recipe, io);
                if (handleIO.isSuccess()) {
                    updateWorkingRecipe(recipe);
                } else {
                    this.workingRecipe = null;
                }
                return handleIO;
            }

            var items = recipe.getOutputContents(ItemRecipeCapability.CAP);
            if (!items.isEmpty()) {
                Map<RecipeCapability<?>, List<Content>> out = Map.of(ItemRecipeCapability.CAP, items);
                RecipeHelper.handleRecipe(getMachine(), recipe, io, out, chanceCaches, false, false);
            }

            if (applyFluidOutputs(recipe, FluidAction.EXECUTE, getMachine().getVoidingMode())) {
                workingRecipe = null;
                return ActionResult.SUCCESS;
            }

            return ActionResult.fail(Component.translatable("gtpm.recipe_logic.insufficient_out")
                    .append(": ")
                    .append(FluidRecipeCapability.CAP.getName()), FluidRecipeCapability.CAP, IO.OUT);
        }

        private boolean applyFluidOutputs(GTRecipe recipe, FluidAction action, VoidingMode voidMode) {
            var fluids = recipe.getOutputContents(FluidRecipeCapability.CAP)
                    .stream()
                    .map(Content::getContent)
                    .map(FluidRecipeCapability.CAP::of)
                    .toList();

            // Distillery recipes should output to the first non-void handler
            if (recipe.recipeType == GTRecipeTypes.DISTILLERY_RECIPES) {
                var fluid = fluids.getFirst().getFluids()[0];
                var handler = getMachine().getFirstValid();
                if (handler == null) return false;
                int filled = (handler instanceof NotifiableFluidTank nft) ?
                        nft.fillInternal(fluid, action) :
                        handler.fill(fluid, action);
                return filled == fluid.getAmount();
            }

            boolean valid = true;
            var outputs = getMachine().getFluidOutputs();
            for (int i = 0; i < Math.min(fluids.size(), outputs.size()); ++i) {
                var handler = outputs.get(i);
                var fluid = fluids.get(i).getFluids()[0];
                int filled = (handler instanceof NotifiableFluidTank nft) ?
                        nft.fillInternal(fluid, action) :
                        handler.fill(fluid, action);
                if (filled != fluid.getAmount() && !voidMode.canVoid(FluidRecipeCapability.CAP)) valid = false;
                if (action.simulate() && !valid) break;
            }
            return valid;
        }
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        GTScrollerViewElement screen = new GTScrollerViewElement(4, 4, 182, 117);
        screen.style(style -> style.backgroundTexture(getScreenTexture()));
        screen.viewPort(viewPort -> viewPort
                .layout(layout -> layout.paddingAll(0))
                .style(style -> style.backgroundTexture(getScreenTexture())));
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
                DistillationTowerMachine.this::addDisplayText)
                .setMaxWidthLimit(200)
                .clickHandler(DistillationTowerMachine.this::handleDisplayClick));
        root.addChild(screen);
        return root;
    }

    @Override
    public void attachTooltips(TooltipsPanel tooltipsPanel) {
        for (IMultiPart part : getParts()) {
            if (part instanceof IMaintenanceMachine maintenanceMachine) {
                maintenanceMachine.attachMaintenanceTooltips(tooltipsPanel);
            }
        }
    }
}
