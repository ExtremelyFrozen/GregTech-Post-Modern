package com.gregtechceu.gtceu.client.gui;

import com.gregtechceu.gtceu.api.gui.ColorPattern;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTSceneElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockPreviewLevel;
import com.gregtechceu.gtceu.api.multiblock.preview.MultiblockPreviewSnapshot;
import com.gregtechceu.gtceu.api.multiblock.preview.MultiblockPreviewSnapshot.Cell;
import com.gregtechceu.gtceu.api.multiblock.preview.MultiblockPreviewState;
import com.gregtechceu.gtceu.integration.xei.GTXEIHelper;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;

import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.utils.data.BlockPosFace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.vfyjxf.taffy.style.TaffyPosition;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Client-owned scene, focus/layer controls and complete material scroller for one preview window.
 */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class ClientMultiblockPreviewElement extends UIElement {

    private static final BlockPos SCENE_ORIGIN = new BlockPos(0, 64, 0);
    private static final int MATERIAL_BAR_HEIGHT = 24;
    private static final int MATERIAL_DETAIL_HEIGHT = 64;

    private final MultiblockPreviewState state;
    private MultiblockPreviewLevel previewLevel;
    @Nullable
    private Level sourceLevel;
    private final GTSceneElement scene;
    private final GTScrollerViewElement materialScroller;
    @Nullable
    private final GTScrollerViewElement materialDetails;
    private final GTButtonElement focusButton;
    private final GTButtonElement layerButton;
    private final GTLabelElement diagnosticLabel;
    private final GTLabelElement demolitionLabel;
    private final boolean showAvailability;
    private final int materialViewportWidth;
    private final int materialDetailViewportWidth;
    private final Map<BlockPos, MultiblockPreviewSnapshot.Cell> sceneCells = new LinkedHashMap<>();
    private final Map<String, Integer> structureColors = new HashMap<>();
    private final List<BlockPos> loadedPositions = new ArrayList<>();
    private int selectedMaterialIndex;
    private boolean removed;

    public ClientMultiblockPreviewElement(int width, int height, MultiblockPreviewState state,
                                          boolean showAvailability) {
        this.state = state;
        this.showAvailability = showAvailability;
        if (showAvailability && height < 160) {
            throw new IllegalArgumentException(
                    "A material allocation preview requires at least 160 pixels of height");
        }
        materialViewportWidth = width - 44;
        materialDetailViewportWidth = width - 6;
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
        scene.createScene(previewLevel);
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.setOnSelected((pos, facing) -> selectCell(pos));
        scene.addEventListener(UIEvents.HOVER_TOOLTIPS, this::addCellTooltips);
        addChild(scene);

        if (showAvailability) {
            GTScrollerViewElement details = new GTScrollerViewElement(2, sceneHeight,
                    width - 4, detailHeight);
            details.scrollerStyle(style -> style
                    .mode(ScrollerMode.VERTICAL)
                    .verticalScrollDisplay(ScrollDisplay.AUTO)
                    .horizontalScrollDisplay(ScrollDisplay.NEVER)
                    .scrollerViewStyle(0));
            details.viewPort(viewPort -> {
                viewPort.layout(layout -> layout.paddingAll(0));
                viewPort.style(style -> style.backgroundTexture(ColorPattern.T_GRAY.rectTexture()));
            });
            details.verticalScroller(scroller -> {
                scroller.layout(layout -> layout.width(2));
                scroller.getStyle().backgroundTexture(ColorPattern.T_WHITE.rectTexture().setRadius(1));
            });
            materialDetails = details;
            addChild(details);
        } else {
            materialDetails = null;
        }

        demolitionLabel = new GTLabelElement(2, height - MATERIAL_BAR_HEIGHT + 5, 38, 10, Component.empty());
        demolitionLabel.setTextColor(0xffff6060);
        demolitionLabel.setTextShadow(true);
        demolitionLabel.style(style -> style.tooltips(
                Component.translatable("gtpm.multiblock.preview.tooltip.demolition_count", 0)));
        addChild(demolitionLabel);

        materialScroller = new GTScrollerViewElement(42, height - MATERIAL_BAR_HEIGHT + 1,
                materialViewportWidth, MATERIAL_BAR_HEIGHT - 2);
        materialScroller.scrollerStyle(style -> style
                .mode(ScrollerMode.HORIZONTAL)
                .horizontalScrollDisplay(ScrollDisplay.AUTO)
                .verticalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(0));
        materialScroller.viewPort(viewPort -> {
            viewPort.layout(layout -> layout.paddingAll(0));
            viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        });
        materialScroller.horizontalScroller(scroller -> {
            scroller.layout(layout -> layout.height(4));
            scroller.getStyle().backgroundTexture(GuiTextures.SLIDER_BACKGROUND);
        });
        addChild(materialScroller);

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

        diagnosticLabel = new GTLabelElement(8, Math.max(42, sceneHeight / 2 - 10),
                width - 16, 24, Component.empty());
        diagnosticLabel.setTextColor(0xffff6060);
        diagnosticLabel.setTextShadow(true);
        diagnosticLabel.setTextWrap(TextWrap.WRAP);
        addChild(diagnosticLabel);

        rebuildSnapshot(true);
    }

    /**
     * Called by the side-safe parent after its shared state accepted a new snapshot.
     */
    public void snapshotChanged(MultiblockPreviewSnapshot snapshot) {
        if (state.snapshot() != snapshot) {
            throw new IllegalStateException("Client preview state was not updated before rebuilding its scene");
        }
        rebuildSnapshot(true);
    }

    private void rebuildSnapshot(boolean resetCamera) {
        runOnRenderThread(() -> {
            if (removed) return;
            releaseLoadedBlocks();
            if (sourceLevel == null) {
                sceneCells.clear();
                scene.setRenderedCore(List.of(), new PreviewCellRenderHook(), resetCamera);
                rebuildMaterials();
                updateDemolitionText();
                updateNavigationText();
                updateDiagnosticText();
                return;
            }
            structureColors.clear();
            for (MultiblockPreviewSnapshot.Structure structure : state.snapshot().structures()) {
                structureColors.put(structure.name(), stableStructureColor(
                        state.snapshot().definitionId(), structure.name()));
            }
            sceneCells.clear();
            for (MultiblockPreviewSnapshot.Cell cell : state.snapshot().cells()) {
                if (cell.renderState().isAir()) continue;
                BlockPos scenePos = SCENE_ORIGIN.offset(cell.relativePos());
                MultiblockBlockInfo blockInfo = cell.renderInfo().createBlockInfo();
                previewLevel.addBlock(scenePos, blockInfo);
                @Nullable
                BlockEntity blockEntity = blockInfo.getBlockEntity(
                        previewLevel.registryAccess(), previewLevel, scenePos);
                if (blockEntity != null) previewLevel.setInnerBlockEntity(blockEntity);
                loadedPositions.add(scenePos);
                sceneCells.put(scenePos, cell);
            }
            rebuildRenderedCore(resetCamera);
            rebuildMaterials();
            updateDemolitionText();
            updateNavigationText();
            updateDiagnosticText();
        });
    }

    private void rebuildRenderedCore(boolean resetCamera) {
        List<BlockPos> rendered = state.visibleCells().stream()
                .filter(cell -> !cell.renderState().isAir())
                .map(cell -> SCENE_ORIGIN.offset(cell.relativePos()))
                .toList();
        scene.setRenderedCore(rendered, new PreviewCellRenderHook(), resetCamera);
    }

    private void rebuildMaterials() {
        materialScroller.clearAllScrollViewChildren();
        List<List<ItemStack>> displayStacks = state.snapshot().materials().stream()
                .map(material -> material.candidates().stream()
                        .map(candidate -> candidate
                                .copyWithCount((int) Math.min(material.required(), Integer.MAX_VALUE)))
                        .toList())
                .toList();
        if (displayStacks.isEmpty()) {
            selectedMaterialIndex = 0;
            materialScroller.viewContainer.layout(layout -> layout.width(1).height(18));
            rebuildMaterialDetails();
            return;
        }
        selectedMaterialIndex = Math.min(selectedMaterialIndex, displayStacks.size() - 1);
        CycleItemEntryHandler itemHandler = CycleItemEntryHandler.createFromStacks(displayStacks);
        for (int index = 0; index < displayStacks.size(); index++) {
            int slotIndex = index;
            GTItemSlotElement slot = new GTItemSlotElement(itemHandler, index);
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
            slot.setBackgroundTexture(ColorPattern.T_GRAY.rectTexture());
            slot.setItemCountDecorationXOffset(ClientMultiblockPreviewElement::countDecorationOffset);
            slot.setOnAddedTooltips((element, tooltips) -> addMaterialTooltips(
                    state.snapshot().materials().get(slotIndex), tooltips));
            slot.addEventListener(UIEvents.MOUSE_DOWN, event -> {
                selectedMaterialIndex = slotIndex;
                rebuildMaterialDetails();
            });
            slot.xeiRecipeIngredient();
            slot.xeiRecipeSlot();
            materialScroller.addScrollViewChild(slot);
        }
        materialScroller.viewContainer.layout(layout -> layout
                .width(Math.max(materialViewportWidth, displayStacks.size() * 18))
                .height(18));
        rebuildMaterialDetails();
    }

    private void rebuildMaterialDetails() {
        @Nullable
        GTScrollerViewElement details = materialDetails;
        if (details == null) return;
        details.clearAllScrollViewChildren();
        List<MultiblockPreviewSnapshot.Material> materials = state.snapshot().materials();
        List<Component> lines = new ArrayList<>();
        if (materials.isEmpty()) {
            lines.add(Component.translatable("gtpm.multiblock.preview.material.none"));
        } else {
            selectedMaterialIndex = Math.min(selectedMaterialIndex, materials.size() - 1);
            MultiblockPreviewSnapshot.Material material = materials.get(selectedMaterialIndex);
            lines.add(Component.translatable("gtpm.multiblock.preview.material.selected",
                    material.candidates().getFirst().getHoverName()));
            lines.add(Component.translatable("gtpm.multiblock.preview.material.required", material.required()));
            for (MultiblockPreviewSnapshot.StructureAmount structure : material.structures()) {
                lines.add(Component.translatable("gtpm.multiblock.preview.material.structure",
                        structure.structureName(), structure.required()));
            }
            if (state.snapshot().availabilityKnown()) {
                lines.add(Component.translatable("gtpm.multiblock.preview.material.me",
                        material.meAvailable(), material.meAllocated()));
                lines.add(Component.translatable("gtpm.multiblock.preview.material.player",
                        material.playerAvailable(), material.playerAllocated()));
                lines.add(Component.translatable("gtpm.multiblock.preview.material.missing", material.missing()));
                if (material.unlimited()) {
                    lines.add(Component.translatable("gtpm.multiblock.preview.material.unlimited"));
                }
            } else {
                lines.add(Component.translatable("gtpm.multiblock.preview.material.availability_unknown"));
            }
        }
        for (int index = 0; index < lines.size(); index++) {
            GTLabelElement label = new GTLabelElement(2, index * 10, materialDetailViewportWidth, 10,
                    lines.get(index));
            label.setTextColor(-1);
            label.setTextShadow(true);
            details.addScrollViewChild(label);
        }
        details.viewContainer.layout(layout -> layout
                .width(materialDetailViewportWidth)
                .height(Math.max(MATERIAL_DETAIL_HEIGHT, lines.size() * 10)));
    }

    private void addMaterialTooltips(MultiblockPreviewSnapshot.Material material, List<Component> tooltips) {
        tooltips.add(Component.translatable("gtpm.multiblock.preview.material.required", material.required()));
        for (MultiblockPreviewSnapshot.StructureAmount structure : material.structures()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.structure",
                    structure.structureName(), structure.required()));
        }
        if (showAvailability && state.snapshot().availabilityKnown()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.me",
                    material.meAvailable(), material.meAllocated()));
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.player",
                    material.playerAvailable(), material.playerAllocated()));
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.missing", material.missing()));
            if (material.unlimited()) {
                tooltips.add(Component.translatable("gtpm.multiblock.preview.material.unlimited"));
            }
        } else if (!state.snapshot().availabilityKnown()) {
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
        List<Component> tooltips = new ArrayList<>();
        tooltips.add(cell.renderState().getBlock().getName());
        tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.position",
                cell.relativePos().getX(), cell.relativePos().getY(), cell.relativePos().getZ()));
        for (MultiblockPreviewSnapshot.Contribution contribution : cell.contributions()) {
            MultiblockPreviewSnapshot.CellKey key = contribution.cellKey();
            tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.contribution",
                    contribution.structureName(), contribution.targetState().getBlock().getName(),
                    Component.translatable("gtpm.multiblock.preview.cell.action." +
                            contribution.action().name().toLowerCase(Locale.ROOT)),
                    key.unitIndex(), key.repetition(), key.innerSlice(), key.row(), key.column()));
            @Nullable
            Direction requiredDirection = contribution.requiredDirection();
            if (requiredDirection != null) {
                tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.direction",
                        Component.translatable("direction.minecraft." + requiredDirection.getName())));
            }
        }
        if (cell.conflict()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.conflict"));
        }
        if (cell.demolitionTarget()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.cell.demolition"));
        }
        event.hoverTooltips = new HoverTooltips(tooltips, null, null, ItemStack.EMPTY);
    }

    private void selectCell(BlockPos scenePos) {
        @Nullable
        Cell cell = sceneCells.get(scenePos);
        if (cell == null || cell.contributions().isEmpty()) return;
        state.focus(cell.representative().structureName());
        rebuildRenderedCore(false);
        updateNavigationText();
    }

    private void updateNavigationText() {
        @Nullable
        String focus = state.focusedStructure();
        focusButton.setText(focus == null ?
                Component.translatable("gtpm.multiblock.preview.button.structure_all") :
                Component.translatable("gtpm.multiblock.preview.button.structure", abbreviate(focus)));
        @Nullable
        Integer layer = state.visibleLayer();
        layerButton.setText(layer == null ?
                Component.translatable("gtpm.multiblock.preview.button.layer_all") :
                Component.translatable("gtpm.multiblock.preview.button.layer", layer));
    }

    private void updateDiagnosticText() {
        List<MultiblockPreviewSnapshot.Diagnostic> diagnostics = state.snapshot().diagnostics();
        diagnosticLabel.setText(diagnostics.isEmpty() ? Component.empty() :
                Component.translatable("gtpm.multiblock.preview.diagnostic",
                        diagnosticDescription(diagnostics.getFirst())));
    }

    private void updateDemolitionText() {
        long count = state.snapshot().cells().stream()
                .filter(MultiblockPreviewSnapshot.Cell::demolitionTarget)
                .count();
        demolitionLabel.setText(Component.translatable(
                "gtpm.multiblock.preview.demolition_count", count));
        demolitionLabel.style(style -> style.tooltips(Component.translatable(
                "gtpm.multiblock.preview.tooltip.demolition_count", count)));
    }

    private static Component diagnosticDescription(MultiblockPreviewSnapshot.Diagnostic diagnostic) {
        return Component.translatable("gtpm.multiblock.preview.diagnostic." +
                diagnostic.code().toLowerCase(Locale.ROOT));
    }

    private void releaseLoadedBlocks() {
        for (BlockPos pos : loadedPositions) {
            @Nullable
            BlockEntity blockEntity = previewLevel.getBlockEntity(pos);
            if (blockEntity != null && !blockEntity.isRemoved()) blockEntity.setRemoved();
            previewLevel.removeBlock(pos, false);
        }
        loadedPositions.clear();
    }

    @Override
    public void screenTick() {
        super.screenTick();
        @Nullable
        Level currentLevel = Minecraft.getInstance().level;
        if (currentLevel != sourceLevel) {
            rebuildForLevel(currentLevel);
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
            scene.createScene(previewLevel);
            scene.setRenderFacing(false);
            scene.setRenderSelect(false);
            if (currentLevel == null) {
                scene.setRenderedCore(List.of(), new PreviewCellRenderHook(), true);
            } else {
                rebuildSnapshot(true);
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
        structureColors.clear();
        super.onRemoved();
    }

    private static GTButtonElement navigationButton(int x, int y, String tooltipKey) {
        GTButtonElement button = new GTButtonElement(x, y, 18, 18);
        button.textStyle(style -> {
            style.textColor(-1);
            style.textShadow(false);
        });
        button.setButtonTextures(ColorPattern.T_GRAY.rectTexture(),
                GuiTextures.group(ColorPattern.T_GRAY.rectTexture(), GuiTextures.colorRect(0x4fffffff)),
                ColorPattern.T_GRAY.rectTexture());
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

    private static String abbreviate(String value) {
        return value.length() <= 4 ? value : value.substring(0, 4);
    }

    private static int stableStructureColor(ResourceLocation definitionId, String structureName) {
        int hash = 31 * definitionId.hashCode() + structureName.hashCode();
        float hue = Integer.toUnsignedLong(hash) / (float) (1L << 32);
        float saturation = 0.58f + ((hash >>> 24) & 0x0f) / 100.0f;
        float value = 0.9f + ((hash >>> 28) & 0x03) / 100.0f;
        return Mth.hsvToRgb(hue, saturation, value);
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
            } else if (cell.controller()) {
                red = green = blue = 1.0f;
            } else {
                int color = structureColors.getOrDefault(cell.representative().structureName(),
                        stableStructureColor(ClientMultiblockPreviewElement.this.state.snapshot().definitionId(),
                                cell.representative().structureName()));
                red = ((color >>> 16) & 0xff) / 255.0f;
                green = ((color >>> 8) & 0xff) / 255.0f;
                blue = (color & 0xff) / 255.0f;
            }
            if (!ClientMultiblockPreviewElement.this.state.isFocused(cell)) {
                red *= 0.3f;
                green *= 0.3f;
                blue *= 0.3f;
            }
            wrapper.setColorMultiplier(Mth.clamp(red, 0, 1), Mth.clamp(green, 0, 1),
                    Mth.clamp(blue, 0, 1), 1);
        }
    }
}
