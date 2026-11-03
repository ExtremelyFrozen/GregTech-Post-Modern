package com.gregtechceu.gtceu.client.gui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.block.property.GTBlockStateProperties;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTSceneElement;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockPreviewLevel;
import com.gregtechceu.gtceu.api.multiblock.preview.PreviewVisibility;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview.Cell;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.common.item.terminal.ui.StructureControls;
import com.gregtechceu.gtceu.integration.xei.GTXEIHelper;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;

import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.api.stack.EmiStack;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import mezz.jei.api.constants.VanillaTypes;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Client-owned scene, focus/layer controls and paged materials for one preview window.
 */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class ClientMultiblockPreviewElement extends UIElement {

    private static final BlockPos SCENE_ORIGIN = new BlockPos(0, 64, 0);
    private static final int MATERIAL_BAR_HEIGHT = 24;
    private static final int MATERIAL_DETAIL_HEIGHT = 34;

    private final PreviewVisibility state;
    private MultiblockPreviewLevel previewLevel;
    @Nullable
    private Level sourceLevel;
    private final GTSceneElement scene;
    private final UIElement materialViewport;
    private @Nullable Consumer<ItemStack> updateJEIMaterial;
    private final GTButtonElement previousMaterials;
    private final GTButtonElement nextMaterials;
    private final ObjectList<GTItemSlotElement> materialSlots = new ObjectArrayList<>();
    @Nullable
    private final UIElement materialDetails;
    private final GTButtonElement focusButton;
    private final GTButtonElement layerButton;
    private final GTButtonElement hatchButton;
    private final GTLabelElement diagnosticLabel;
    private final GTLabelElement demolitionLabel;
    private final boolean showAvailability;
    private int materialViewportWidth;
    private int materialDetailViewportWidth;
    private final Object2ObjectMap<BlockPos, StructurePreview.Cell> sceneCells = new Object2ObjectLinkedOpenHashMap<>();
    private final ObjectList<BlockPos> loadedPositions = new ObjectArrayList<>();
    private final ObjectList<BlockPos> hatchPositions = new ObjectArrayList<>();
    private int selectedMaterialIndex;
    private int materialPage;
    private long renderedViewRevision;
    private @Nullable BlockState cameraControllerState;
    private boolean removed;

    public ClientMultiblockPreviewElement(int width, int height, PreviewVisibility state,
                                          boolean showAvailability) {
        this.state = state;
        this.showAvailability = showAvailability;
        materialViewportWidth = width - 74;
        materialDetailViewportWidth = width - 8;
        layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.width(width);
            layout.height(height);
        });
        setOverflowVisible(false);
        @Nullable
        Level clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            throw new IllegalStateException("Cannot create a multiblock preview before the client level is loaded");
        }
        sourceLevel = clientLevel;
        previewLevel = new MultiblockPreviewLevel(clientLevel);

        int detailHeight = showAvailability ? MATERIAL_DETAIL_HEIGHT : 0;
        int sceneHeight = height - MATERIAL_BAR_HEIGHT - detailHeight;
        scene = new GTSceneElement(0, 0, width, sceneHeight);
        scene.setAllowXEILookup(false);
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.setAfterWorldRender(ignored -> renderHatchOutlines());
        scene.setOnSelected((pos, facing) -> selectCell(pos));
        scene.addEventListener(UIEvents.HOVER_TOOLTIPS, this::addCellTooltips);
        addChild(scene);

        if (showAvailability) {
            materialDetails = new UIElement();
            materialDetails.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                    .left(2).top(sceneHeight).width(width - 4).height(detailHeight));
            materialDetails.getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
            materialDetails.setOverflowVisible(false);
            addChild(materialDetails);
        } else {
            materialDetails = null;
        }

        demolitionLabel = new GTLabelElement(2, height - MATERIAL_BAR_HEIGHT + 5, 38, 10, Component.empty());
        demolitionLabel.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(8)
                .textColor(0xffa02020).textShadow(false).textWrap(TextWrap.HIDE));
        demolitionLabel.style(style -> style.tooltips(
                Component.translatable("gtpm.multiblock.preview.tooltip.demolition_count", 0)));
        addChild(demolitionLabel);

        materialViewport = new UIElement();
        materialViewport.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(56).top(height - MATERIAL_BAR_HEIGHT + 3).width(materialViewportWidth).height(18));
        materialViewport.setOverflowVisible(false);
        previousMaterials = navigationButton(40, height - MATERIAL_BAR_HEIGHT + 3,
                "gtpm.multiblock.preview.tooltip.previous_materials");
        previousMaterials.getLayout().width(14);
        previousMaterials.setText(Component.literal("<"));
        previousMaterials.setOnClick(event -> {
            materialPage--;
            updateMaterialPage();
            event.stopPropagation();
        });
        nextMaterials = navigationButton(width - 16, height - MATERIAL_BAR_HEIGHT + 3,
                "gtpm.multiblock.preview.tooltip.next_materials");
        nextMaterials.getLayout().width(14);
        nextMaterials.setText(Component.literal(">"));
        nextMaterials.setOnClick(event -> {
            materialPage++;
            updateMaterialPage();
            event.stopPropagation();
        });
        // The container survives material changes; per-slot recipe widgets would retain removed slots.
        if (GTCEu.MODS.isEMILoaded()) {
            LDLibEMIPlugin.recipeSlot(materialViewport, () -> EmiStack.of(getHoveredMaterial()));
        }
        if (GTCEu.MODS.isJEILoaded()) {
            updateJEIMaterial = LDLibJEIPlugin.recipeSlot(materialViewport, IngredientIO.INPUT,
                    VanillaTypes.ITEM_STACK, () -> Stream.of(getHoveredMaterial()), ignored -> {});
        }
        addChildren(materialViewport, previousMaterials, nextMaterials);

        focusButton = navigationButton(width - 20, 2, "gtpm.multiblock.preview.tooltip.focus");
        focusButton.setOnClick(event -> {
            state.focusNext();
            rebuildRenderedCore(false);
            updateNavigationText();
            event.stopPropagation();
        });
        addChild(focusButton);

        layerButton = navigationButton(width - 20, 22, "gtpm.multiblock.preview.tooltip.layer");
        layerButton.setOnClick(event -> {
            state.layerNext();
            rebuildRenderedCore(false);
            updateNavigationText();
            event.stopPropagation();
        });
        addChild(layerButton);

        hatchButton = navigationButton(width - 20, 42, "gtpm.multiblock.preview.tooltip.hatches");
        hatchButton.setText(Component.translatable("gtpm.multiblock.preview.button.hatches"));
        hatchButton.setOnClick(event -> {
            state.setHighlightHatches(!state.isHighlightHatches());
            updateNavigationText();
            event.stopPropagation();
        });
        addChild(hatchButton);

        diagnosticLabel = new GTLabelElement(8, Math.max(42, sceneHeight / 2 - 10),
                width - 16, 24, Component.empty());
        diagnosticLabel.setTextColor(0xffff6060);
        diagnosticLabel.setTextShadow(true);
        diagnosticLabel.setTextWrap(TextWrap.WRAP);
        diagnosticLabel.setAllowHitTest(false);
        addChild(diagnosticLabel);

        rebuildPreview(true);
    }

    public void resize(int width, int height) {
        getLayout().width(width).height(height);
        materialViewportWidth = width - 74;
        materialDetailViewportWidth = width - 8;
        int sceneHeight = height - MATERIAL_BAR_HEIGHT - (showAvailability ? MATERIAL_DETAIL_HEIGHT : 0);
        scene.getLayout().width(width).height(sceneHeight);
        if (materialDetails != null) materialDetails.getLayout().top(sceneHeight).width(width - 4);
        materialViewport.getLayout().top(height - MATERIAL_BAR_HEIGHT + 3).width(materialViewportWidth);
        previousMaterials.getLayout().top(height - MATERIAL_BAR_HEIGHT + 3);
        nextMaterials.getLayout().left(width - 16).top(height - MATERIAL_BAR_HEIGHT + 3);
        demolitionLabel.getLayout().top(height - MATERIAL_BAR_HEIGHT + 5);
        focusButton.getLayout().left(width - 20);
        layerButton.getLayout().left(width - 20);
        hatchButton.getLayout().left(width - 20);
        diagnosticLabel.getLayout().top((float) sceneHeight / 2 - 10).width(width - 16);
        rebuildMaterials();
    }

    /**
     * Called by the side-safe parent after its shared state accepted a new preview.
     */
    public void previewChanged(StructurePreview preview) {
        if (state.getPreview() != preview) {
            throw new IllegalStateException("Client preview state was not updated before rebuilding its scene");
        }
        rebuildPreview(true);
    }

    private void rebuildPreview(boolean resetCamera) {
        runOnRenderThread(() -> {
            if (removed) return;
            releaseLoadedBlocks();
            if (sourceLevel == null) {
                sceneCells.clear();
                scene.createScene(previewLevel);
                scene.setRenderedCore(ObjectList.of(), new PreviewCellRenderHook(), resetCamera);
                rebuildMaterials();
                updateDemolitionText();
                updateNavigationText();
                updateDiagnosticText();
                return;
            }
            sceneCells.clear();
            BlockState controllerState = null;
            for (StructurePreview.Cell cell : state.getPreview().cells()) {
                if (cell.renderState().isAir()) continue;
                BlockPos scenePos = SCENE_ORIGIN.offset(cell.relativePos());
                MultiblockBlockInfo blockInfo = cell.renderInfo().createBlockInfo();
                previewLevel.addBlock(scenePos, blockInfo);
                if (cell.worldPos() != null) {
                    previewLevel.copyMachineAppearance(sourceLevel, cell.worldPos(), scenePos);
                }
                loadedPositions.add(scenePos);
                sceneCells.put(scenePos, cell);
                if (cell.controller()) controllerState = cell.renderState();
            }
            // Scene installs a core filter: attach it only after every block initializer has run.
            scene.createScene(previewLevel);
            if (controllerState != null && controllerState != cameraControllerState) {
                cameraControllerState = controllerState;
                orientCamera(controllerState);
            }
            rebuildRenderedCore(resetCamera);
            rebuildMaterials();
            updateDemolitionText();
            updateNavigationText();
            updateDiagnosticText();
        });
    }

    private void orientCamera(BlockState controllerState) {
        if (!(controllerState.getBlock() instanceof MetaMachineBlock block)) return;
        var definition = block.getDefinition();
        var rotation = definition.getRotationState();
        Direction front = rotation == RotationState.NONE ? Direction.NORTH :
                controllerState.getValue(rotation.property);
        Direction upwards = controllerState.hasProperty(GTBlockStateProperties.UPWARDS_FACING) ?
                controllerState.getValue(GTBlockStateProperties.UPWARDS_FACING) : Direction.NORTH;
        Direction up = RelativeDirection.UP.getRelative(front, upwards, false);
        Direction right = RelativeDirection.RIGHT.getRelative(front, upwards, false);
        // Look at the controller's front, right and top in the same coordinate frame as the resolved structure.
        var eye = new Vector3f(front.getStepX(), front.getStepY(), front.getStepZ()).mul(2)
                .add(right.getStepX(), right.getStepY(), right.getStepZ())
                .add(up.getStepX(), up.getStepY(), up.getStepZ());
        scene.setCameraYawAndPitch((float) Math.toDegrees(Math.atan2(eye.z, eye.x)),
                (float) Math.toDegrees(Math.atan2(eye.y, Math.hypot(eye.x, eye.z))));
    }

    private void rebuildRenderedCore(boolean resetCamera) {
        var visible = state.visibleCells();
        hatchPositions.clear();
        for (var cell : visible) {
            if (!cell.renderState().isAir() && cell.contributions().stream()
                    .anyMatch(StructurePreview.Contribution::hatchReplaceable)) {
                hatchPositions.add(SCENE_ORIGIN.offset(cell.relativePos()));
            }
        }
        ObjectList<BlockPos> rendered = visible.stream()
                .filter(cell -> !cell.renderState().isAir())
                .map(cell -> SCENE_ORIGIN.offset(cell.relativePos()))
                .collect(ObjectImmutableList.toList());
        scene.setRenderedCore(rendered, new PreviewCellRenderHook(), resetCamera);
        renderedViewRevision = state.getViewRevision();
    }

    private ItemStack getHoveredMaterial() {
        var hovered = getModularUI().getLastHoveredElement();
        return hovered instanceof GTItemSlotElement slot && slot.getParent() == materialViewport ?
                slot.getValue() : ItemStack.EMPTY;
    }

    private void rebuildMaterials() {
        materialViewport.clearAllChildren();
        materialSlots.clear();
        ObjectList<List<ItemStack>> displayStacks = state.getPreview().materials().stream()
                .<List<ItemStack>>map(material -> material.candidates().stream()
                        .map(candidate -> candidate
                                .copyWithCount((int) Math.min(material.required(), Integer.MAX_VALUE)))
                        .collect(ObjectImmutableList.toList()))
                .collect(ObjectImmutableList.toList());
        if (displayStacks.isEmpty()) {
            selectedMaterialIndex = 0;
            updateMaterialPage();
            rebuildMaterialDetails();
            return;
        }
        selectedMaterialIndex = Math.min(selectedMaterialIndex, displayStacks.size() - 1);
        CycleItemEntryHandler itemHandler = CycleItemEntryHandler.createFromStacks(displayStacks);
        for (int index = 0; index < displayStacks.size(); index++) {
            GTItemSlotElement slot = getSlot(index, itemHandler);
            materialViewport.addChild(slot);
            materialSlots.add(slot);
        }
        updateMaterialPage();
        rebuildMaterialDetails();
    }

    private GTItemSlotElement getSlot(int slotIndex, CycleItemEntryHandler itemHandler) {
        GTItemSlotElement slot = new GTItemSlotElement(itemHandler, slotIndex);
        slot.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(slotIndex * 18);
            layout.top(0);
            layout.width(18);
            layout.height(18);
        });
        slot.setCanTakeItems(false);
        slot.setCanPutItems(false);
        slot.setIngredientIO(GTXEIHelper.input());
        slot.setBackgroundTexture(GuiTextures.SLOT);
        slot.setItemCountDecorationXOffset(ClientMultiblockPreviewElement::countDecorationOffset);
        slot.setOnAddedTooltips((element, tooltips) -> addMaterialTooltips(
                state.getPreview().materials().get(slotIndex), tooltips));
        slot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
            selectedMaterialIndex = slotIndex;
            rebuildMaterialDetails();
        });
        slot.xeiRecipeIngredient();
        return slot;
    }

    private void updateMaterialPage() {
        int perPage = Math.max(1, materialViewportWidth / 18);
        int lastPage = Math.max(0, (materialSlots.size() - 1) / perPage);
        materialPage = Math.clamp(materialPage, 0, lastPage);
        int first = materialPage * perPage;
        // Keep every ingredient in the XEI tree; the viewport clips slots outside the current page.
        for (int index = 0; index < materialSlots.size(); index++) {
            materialSlots.get(index).getLayout().left((index - first) * 18);
        }
        materialViewport.getLayout().width(perPage * 18);
        previousMaterials.setActive(materialPage > 0);
        nextMaterials.setActive(materialPage < lastPage);
    }

    private void rebuildMaterialDetails() {
        @Nullable
        UIElement details = materialDetails;
        if (details == null) return;
        details.clearAllChildren();
        ObjectList<StructurePreview.Material> materials = state.getPreview().materials();
        ObjectList<Component> lines = new ObjectArrayList<>();
        if (materials.isEmpty()) {
            lines.add(Component.translatable("gtpm.multiblock.preview.material.none"));
        } else {
            selectedMaterialIndex = Math.min(selectedMaterialIndex, materials.size() - 1);
            StructurePreview.Material material = materials.get(selectedMaterialIndex);
            lines.add(Component.translatable("gtpm.multiblock.preview.material.selected",
                    material.candidates().getFirst().getHoverName()));
            lines.add(Component.translatable("gtpm.multiblock.preview.material.required", material.required()));
            if (state.getPreview().availabilityKnown()) {
                lines.add(Component.translatable(material.unlimited() ?
                        "gtpm.multiblock.preview.material.unlimited" :
                        "gtpm.multiblock.preview.material.missing", material.missing()));
            } else {
                lines.add(Component.translatable("gtpm.multiblock.preview.material.availability_unknown"));
            }
        }
        for (int index = 0; index < lines.size(); index++) {
            GTLabelElement label = new GTLabelElement(2, 2 + index * 10, materialDetailViewportWidth, 10,
                    lines.get(index));
            label.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(8)
                    .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
            label.getStyle().tooltips(lines.get(index));
            details.addChild(label);
        }
        ObjectList<Component> tooltips = new ObjectArrayList<>();
        if (!materials.isEmpty()) addMaterialTooltips(materials.get(selectedMaterialIndex), tooltips);
        details.getStyle().tooltips(tooltips.toArray(Component[]::new));
    }

    private void addMaterialTooltips(StructurePreview.Material material, List<Component> tooltips) {
        tooltips.add(Component.translatable("gtpm.multiblock.preview.material.required", material.required()));
        for (StructurePreview.StructureAmount structure : material.structures()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.structure",
                    StructureControls.structureName(structure.structureName()), structure.required()));
        }
        if (showAvailability && state.getPreview().availabilityKnown()) {
            if (GTCEu.MODS.AE2.isLoaded()) {
                tooltips.add(Component.translatable("gtpm.multiblock.preview.material.me",
                        material.meAvailable(), material.meAllocated()));
            }
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.player",
                    material.playerAvailable(), material.playerAllocated()));
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.missing", material.missing()));
            if (material.unlimited()) {
                tooltips.add(Component.translatable("gtpm.multiblock.preview.material.unlimited"));
            }
        } else if (!state.getPreview().availabilityKnown()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.availability_unknown"));
        }
    }

    private void addCellTooltips(UIEvent event) {
        @Nullable
        BlockPosFace hovered = scene.getLastHoverPosFace();
        if (hovered == null) return;
        @Nullable
        Cell cell = sceneCells.get(hovered.pos());
        if (cell == null) return;
        ObjectList<Component> tooltips = new ObjectArrayList<>();
        tooltips.add(cell.renderState().getBlock().getName());
        tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.position",
                cell.relativePos().getX(), cell.relativePos().getY(), cell.relativePos().getZ()));
        for (StructurePreview.Contribution contribution : cell.contributions()) {
            StructurePreview.CellKey key = contribution.cellKey();
            tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.contribution",
                    StructureControls.structureName(contribution.structureName()),
                    contribution.targetState().getBlock().getName(),
                    Component.translatable("gtpm.multiblock.preview.cell.action." +
                            contribution.action().name().toLowerCase(Locale.ROOT)),
                    key.unitIndex(), key.repetition(), key.innerSlice(), key.row(), key.column()));
            @Nullable
            Direction requiredDirection = contribution.requiredDirection();
            if (requiredDirection != null) {
                tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.direction",
                        Component.translatable("gtpm.multiblock.preview.direction." + requiredDirection.getName())));
            }
        }
        if (cell.conflict()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.conflict"));
        }
        if (cell.demolitionTarget()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.demolition"));
        }
        if (cell.contributions().stream().anyMatch(StructurePreview.Contribution::hatchReplaceable)) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.hatch_replaceable"));
        }
        event.hoverTooltips = new HoverTooltips(tooltips, null, null, ItemStack.EMPTY);
    }

    private void selectCell(BlockPos scenePos) {
        @Nullable
        Cell cell = sceneCells.get(scenePos);
        if (cell == null || cell.contributions().isEmpty()) return;
        state.setFocusedStructure(cell.representative().structureName());
        rebuildRenderedCore(false);
        updateNavigationText();
    }

    private void updateNavigationText() {
        @Nullable
        String focus = state.getFocusedStructure();
        focusButton.setText(focus == null ?
                Component.translatable("gtpm.multiblock.preview.button.structure_all") :
                Component.translatable("gtpm.multiblock.preview.button.structure"));
        focusButton.getStyle().tooltips(focus == null ?
                Component.translatable("gtpm.multiblock.preview.tooltip.focus") :
                StructureControls.structureName(focus));
        @Nullable
        Integer layer = state.getVisibleLayer();
        layerButton.setText(layer == null ?
                Component.translatable("gtpm.multiblock.preview.button.layer_all") :
                Component.translatable("gtpm.multiblock.preview.button.layer", layer));
        hatchButton.textStyle(style -> style.textColor(state.isHighlightHatches() ? 0xff167129 : 0xff404040));
    }

    private void updateDiagnosticText() {
        ObjectList<StructurePreview.Diagnostic> diagnostics = state.getPreview().diagnostics();
        diagnosticLabel.setText(diagnostics.isEmpty() ? Component.empty() :
                Component.translatable("gtpm.multiblock.preview.diagnostic",
                        diagnosticDescription(diagnostics.getFirst())));
    }

    private void updateDemolitionText() {
        long count = state.getPreview().cells().stream()
                .filter(StructurePreview.Cell::demolitionTarget)
                .count();
        demolitionLabel.setText(Component.translatable(
                "gtpm.multiblock.preview.demolition_count", count));
        demolitionLabel.style(style -> style.tooltips(Component.translatable(
                "gtpm.multiblock.preview.tooltip.demolition_count", count)));
    }

    private static Component diagnosticDescription(StructurePreview.Diagnostic diagnostic) {
        return Component.translatable("gtpm.multiblock.preview.diagnostic." +
                diagnostic.code().toLowerCase(Locale.ROOT));
    }

    private void releaseLoadedBlocks() {
        // Hidden cells still own block entities that must be initialized and released with the whole preview.
        previewLevel.setBlockFilter(null);
        for (BlockPos pos : loadedPositions) {
            @Nullable
            BlockEntity blockEntity = previewLevel.getBlockEntity(pos);
            if (blockEntity != null && !blockEntity.isRemoved()) blockEntity.setRemoved();
            previewLevel.removeBlock(pos, false);
        }
        loadedPositions.clear();
        hatchPositions.clear();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (updateJEIMaterial != null) updateJEIMaterial.accept(getHoveredMaterial());
        @Nullable
        Level currentLevel = Minecraft.getInstance().level;
        if (currentLevel != sourceLevel) {
            rebuildForLevel(currentLevel);
        } else if (renderedViewRevision != state.getViewRevision()) {
            rebuildRenderedCore(false);
            updateNavigationText();
        }
    }

    private void rebuildForLevel(@Nullable Level currentLevel) {
        runOnRenderThread(() -> {
            if (removed || sourceLevel == currentLevel) return;
            releaseLoadedBlocks();
            sceneCells.clear();
            sourceLevel = currentLevel;
            previewLevel = currentLevel == null ?
                    new MultiblockPreviewLevel() : new MultiblockPreviewLevel(currentLevel);
            if (currentLevel == null) {
                scene.createScene(previewLevel);
                scene.setRenderedCore(ObjectList.of(), new PreviewCellRenderHook(), true);
            } else {
                rebuildPreview(true);
            }
        });
    }

    @Override
    protected void onRemoved() {
        removed = true;
        releaseLoadedBlocks();
        sourceLevel = null;
        previewLevel = new MultiblockPreviewLevel();
        sceneCells.clear();
        super.onRemoved();
    }

    private static GTButtonElement navigationButton(int x, int y, String tooltipKey) {
        GTButtonElement button = new GTButtonElement(x, y, 18, 18);
        button.getLayout().paddingAll(1);
        button.text.getLayout().marginAll(0).widthPercent(100).minWidth(0);
        button.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(8)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        button.setButtonTexture(GuiTextures.BUTTON);
        button.style(style -> style.tooltips(Component.translatable(tooltipKey)));
        return button;
    }

    private static int countDecorationOffset(ItemStack stack) {
        int count = stack.getCount();
        if (count >= 100_000) return 9;
        if (count >= 10_000) return 6;
        if (count >= 1_000) return 3;
        return 0;
    }

    private void renderHatchOutlines() {
        if (!state.isHighlightHatches() || hatchPositions.isEmpty()) return;
        var buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        var pose = new PoseStack();
        for (BlockPos pos : hatchPositions) {
            LevelRenderer.renderLineBox(pose, lines, new AABB(pos).inflate(0.002), 0.2f, 0.85f, 1.0f, 1.0f);
        }
        buffers.endBatch(RenderType.lines());
    }

    private static void runOnRenderThread(Runnable action) {
        if (RenderSystem.isOnRenderThread()) action.run();
        else RenderSystem.recordRenderCall(action::run);
    }

    private final class PreviewCellRenderHook implements ISceneBlockRenderHook {

        @Override
        public void applyVertexConsumerWrapper(Level world, BlockPos pos, BlockState state,
                                               WorldSceneRenderer.VertexConsumerWrapper wrapper,
                                               RenderType layer, float partialTicks) {
            @Nullable
            Cell cell = sceneCells.get(pos);
            if (cell == null) return;
            float red;
            float green;
            float blue;
            if (cell.conflict()) {
                red = 1.0f;
                green = 0.18f;
                blue = 0.92f;
            } else if (cell.demolitionTarget()) {
                red = 1.0f;
                green = 0.2f;
                blue = 0.16f;
            } else {
                red = green = blue = 1.0f;
            }
            if (!ClientMultiblockPreviewElement.this.state.isFocused(cell)) {
                red *= 0.3f;
                green *= 0.3f;
                blue *= 0.3f;
            }
            wrapper.setColorMultiplier(red, green, blue, 1);
        }
    }
}
