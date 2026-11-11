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
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.MultiblockBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.MultiblockPreviewLevel;
import com.gregtechceu.gtceu.api.multiblock.preview.PreviewVisibility;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview.Cell;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.integration.xei.GTXEIHelper;

import com.lowdragmc.lowdraglib2.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib2.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvent;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
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
import dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Setter;
import mezz.jei.api.constants.VanillaTypes;
import org.joml.Vector3f;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Client-owned scene, focus/layer controls and paged materials for one preview window.
 */
@OnlyIn(Dist.CLIENT)
@NullMarked
public final class ClientMultiblockPreviewElement extends UIElement {

    private static final BlockPos SCENE_ORIGIN = new BlockPos(0, 64, 0);
    private static final int MATERIAL_BAR_HEIGHT = 24;
    private static final boolean FTB_LIBRARY_LOADED = GTCEu.isModLoaded("ftblibrary");

    private final PreviewVisibility state;
    private MultiblockPreviewLevel previewLevel;
    @Nullable
    private Level sourceLevel;
    private final GTSceneElement scene;
    private final UIElement materialViewport;
    private final UIElement predicateViewport = new UIElement();
    private final Object2ObjectMap<GTItemSlotElement, List<ItemStack>> predicateItems = new Object2ObjectLinkedOpenHashMap<>();
    private @Nullable Consumer<ItemStack> updateJEIMaterial;
    private @Nullable Consumer<ItemStack> updateJEIPredicate;
    private final GTButtonElement previousMaterials;
    private final GTButtonElement nextMaterials;
    private final ObjectList<GTItemSlotElement> materialSlots = new ObjectArrayList<>();
    private ObjectList<List<ItemStack>> materialCandidates = ObjectList.of();
    private final GTButtonElement focusButton;
    private final GTButtonElement layerButton;
    private final GTButtonElement hatchButton;
    private final GTLabelElement diagnosticLabel;
    private final boolean showAvailability;
    @Setter
    private @Nullable Consumer<Cell> onSelected;
    private int materialViewportWidth;
    private float sceneAspect;
    private final Object2ObjectMap<BlockPos, StructurePreview.Cell> sceneCells = new Object2ObjectLinkedOpenHashMap<>();
    private final ObjectList<BlockPos> loadedPositions = new ObjectArrayList<>();
    private final ObjectList<BlockPos> hatchPositions = new ObjectArrayList<>();
    private int materialPage;
    private long renderedViewRevision;
    private @Nullable BlockState cameraControllerState;
    private boolean removed;
    private boolean fullscreen;

    public ClientMultiblockPreviewElement(int width, int height, PreviewVisibility state,
                                          boolean showAvailability) {
        this.state = state;
        this.showAvailability = showAvailability;
        getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
        materialViewportWidth = width - 36;
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

        int sceneHeight = height - MATERIAL_BAR_HEIGHT;
        sceneAspect = (float) width / sceneHeight;
        scene = new GTSceneElement(0, 0, width, sceneHeight);
        scene.setAllowXEILookup(false);
        scene.setRenderFacing(false);
        scene.setRenderSelect(false);
        scene.setAfterWorldRender(ignored -> renderHatchOutlines());
        scene.setOnSelected((pos, facing) -> selectCell(pos));
        scene.addEventListener(UIEvents.HOVER_TOOLTIPS, this::addCellTooltips);
        addChild(scene);

        materialViewport = new UIElement();
        materialViewport.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(18).top(height - MATERIAL_BAR_HEIGHT + 3).width(materialViewportWidth).height(18));
        materialViewport.setOverflowVisible(false);
        previousMaterials = navigationButton(2, height - MATERIAL_BAR_HEIGHT + 3,
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
            EMISupport.recipeSlot(materialViewport, () -> getHoveredItem(materialViewport));
            EMISupport.recipeSlot(predicateViewport, () -> getHoveredItem(predicateViewport));
        }
        if (GTCEu.MODS.isJEILoaded()) {
            updateJEIMaterial = JEISupport.recipeSlot(materialViewport, () -> getHoveredItem(materialViewport));
            updateJEIPredicate = JEISupport.recipeSlot(predicateViewport, () -> getHoveredItem(predicateViewport));
        }
        addChildren(materialViewport, previousMaterials, nextMaterials);

        focusButton = navigationButton(width - 22, 4, "gtpm.multiblock.preview.tooltip.focus");
        focusButton.setOnClick(event -> {
            state.focusNext();
            rebuildRenderedCore(false);
            updateNavigationText();
            event.stopPropagation();
        });
        addChild(focusButton);

        layerButton = navigationButton(width - 22, 24, "gtpm.multiblock.preview.tooltip.layer");
        layerButton.setOnClick(event -> {
            state.layerNext();
            rebuildRenderedCore(false);
            updateNavigationText();
            event.stopPropagation();
        });
        addChild(layerButton);

        hatchButton = navigationButton(width - 22, 44, "gtpm.multiblock.preview.tooltip.hatches");
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

        predicateViewport.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE)
                .left(4).top(4).width(18).height(sceneHeight - 8));
        predicateViewport.setOverflowVisible(false);
        predicateViewport.setAllowHitTest(false);
        predicateViewport.setDisplay(false);
        addChild(predicateViewport);

        rebuildPreview(true);
    }

    public void resize(int width, int height) {
        getLayout().width(width).height(height);
        materialViewportWidth = width - 36;
        int sceneHeight = height - (fullscreen ? 0 : MATERIAL_BAR_HEIGHT);
        sceneAspect = (float) width / sceneHeight;
        scene.getLayout().width(width).height(sceneHeight);
        materialViewport.getLayout().top(height - MATERIAL_BAR_HEIGHT + 3).width(materialViewportWidth);
        previousMaterials.getLayout().top(height - MATERIAL_BAR_HEIGHT + 3);
        nextMaterials.getLayout().left(width - 16).top(height - MATERIAL_BAR_HEIGHT + 3);
        focusButton.getLayout().left(width - (fullscreen ? 108 : 22)).top(fullscreen ? 8 : 4);
        layerButton.getLayout().left(width - (fullscreen ? 88 : 22)).top(fullscreen ? 8 : 24);
        hatchButton.getLayout().left(width - (fullscreen ? 68 : 22)).top(fullscreen ? 8 : 44);
        diagnosticLabel.getLayout().top((float) sceneHeight / 2 - 10).width(width - 16);
        layoutCandidates(height);
        rebuildRenderedCore(true);
        rebuildMaterials();
    }

    public void setFullscreen(boolean fullscreen) {
        if (this.fullscreen && !fullscreen && FTB_LIBRARY_LOADED) FTBSupport.setSidebarVisible(true);
        this.fullscreen = fullscreen;
        getStyle().backgroundTexture(fullscreen ? IGuiTexture.EMPTY : GuiTextures.BACKGROUND_INVERSE);
    }

    public void setPreview(StructurePreview preview) {
        clearCandidates();
        state.setPreview(preview);
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
        if (resetCamera && !rendered.isEmpty()) {
            var center = scene.getCenter();
            double radiusSquared = 0;
            for (BlockPos pos : rendered) {
                double x = Math.abs(pos.getX() + 0.5 - center.x) + 0.5;
                double y = Math.abs(pos.getY() + 0.5 - center.y) + 0.5;
                double z = Math.abs(pos.getZ() + 0.5 - center.z) + 0.5;
                radiusSquared = Math.max(radiusSquared, x * x + y * y + z * z);
            }
            double halfFov = Math.atan(Math.tan(Math.toRadians(scene.getRenderer().getFov() / 2)) *
                    Math.min(1, sceneAspect));
            scene.setZoom((float) (Math.sqrt(radiusSquared) / Math.sin(halfFov) * 1.1));
        }
        renderedViewRevision = state.getViewRevision();
    }

    private ItemStack getHoveredItem(UIElement viewport) {
        var ui = getModularUI();
        var hit = ui.ui.rootElement.hitTest(ui.getLastMouseX(), ui.getLastMouseY());
        var hovered = hit == null ? null : hit.getA();
        return hovered instanceof GTItemSlotElement slot && viewport.isAncestorOf(slot) ?
                slot.getValue() : ItemStack.EMPTY;
    }

    public void clearCandidates() {
        if (updateJEIPredicate != null) updateJEIPredicate.accept(ItemStack.EMPTY);
        predicateViewport.clearAllChildren();
        predicateItems.clear();
        predicateViewport.setDisplay(false);
    }

    public void addCandidates(List<ItemStack> items, List<Component> tooltips) {
        if (items.isEmpty()) return;
        var slot = new GTItemSlotElement();
        slot.layout(layout -> layout.positionType(TaffyPosition.ABSOLUTE).width(18).height(18));
        slot.setCanPutItems(false).setCanTakeItems(false).setBackgroundTexture(GuiTextures.SLOT);
        slot.setOnAddedTooltips((element, tips) -> tips.addAll(tooltips));
        slot.setItem(items.getFirst(), false);
        predicateItems.put(slot, items);
        predicateViewport.addChild(slot);
        layoutCandidates((int) getSizeHeight());
        predicateViewport.setDisplay(true);
    }

    private void layoutCandidates(int previewHeight) {
        int rows = Math.max(1, (previewHeight - MATERIAL_BAR_HEIGHT - (fullscreen ? 38 : 8)) / 18);
        int index = 0;
        for (var slot : predicateItems.keySet()) {
            slot.getLayout().left(index / rows * 18).top(index % rows * 18);
            index++;
        }
        predicateViewport.getLayout().left(fullscreen ? 8 : 4).top(fullscreen ? 34 : 4)
                .width(Math.ceilDiv(index, rows) * 18).height(Math.min(index, rows) * 18);
    }

    private void rebuildMaterials() {
        if (updateJEIMaterial != null) updateJEIMaterial.accept(ItemStack.EMPTY);
        materialViewport.clearAllChildren();
        materialSlots.clear();
        materialCandidates = state.getPreview().materials().stream()
                .<List<ItemStack>>map(material -> material.candidates().stream()
                        .map(candidate -> candidate
                                .copyWithCount((int) Math.min(material.required(), Integer.MAX_VALUE)))
                        .collect(ObjectImmutableList.toList()))
                .collect(ObjectImmutableList.toList());
        if (materialCandidates.isEmpty()) {
            updateMaterialPage();
            return;
        }
        for (int index = 0; index < materialCandidates.size(); index++) {
            GTItemSlotElement slot = getSlot(index);
            materialViewport.addChild(slot);
            materialSlots.add(slot);
        }
        updateMaterialIcons();
        updateMaterialPage();
    }

    private GTItemSlotElement getSlot(int slotIndex) {
        GTItemSlotElement slot = new GTItemSlotElement();
        slot.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(slotIndex * 18);
            layout.top(0);
            layout.width(18);
            layout.height(18);
        });
        slot.setIngredientIO(GTXEIHelper.input());
        slot.setBackgroundTexture(GuiTextures.SLOT);
        slot.setItemCountDecorationXOffset(ClientMultiblockPreviewElement::countDecorationOffset);
        slot.setOnAddedTooltips((element, tooltips) -> addMaterialTooltips(
                state.getPreview().materials().get(slotIndex), tooltips));
        slot.xeiRecipeIngredient();
        return slot;
    }

    private void updateMaterialPage() {
        if (updateJEIMaterial != null) updateJEIMaterial.accept(ItemStack.EMPTY);
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

    private void updateMaterialIcons() {
        long cycle = System.currentTimeMillis() / 1000;
        for (int i = 0; i < materialSlots.size(); i++) {
            var values = materialCandidates.get(i);
            materialSlots.get(i).setItem(values.isEmpty() ? ItemStack.EMPTY : values.get((int) (cycle % values.size())),
                    false);
        }
    }

    private void addMaterialTooltips(StructurePreview.Material material, List<Component> tooltips) {
        tooltips.add(Component.translatable("gtpm.multiblock.preview.material.required", material.required()));
        for (StructurePreview.StructureAmount structure : material.structures()) {
            tooltips.add(Component.translatable("gtpm.multiblock.preview.material.structure",
                    structureName(structure.structureName()), structure.required()));
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
                    structureName(contribution.structureName()),
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
        if (onSelected != null) onSelected.accept(cell);
    }

    private void updateNavigationText() {
        @Nullable
        String focus = state.getFocusedStructure();
        focusButton.setText(focus == null ?
                Component.translatable("gtpm.multiblock.preview.button.structure_all") :
                Component.translatable("gtpm.multiblock.preview.button.structure"));
        focusButton.getStyle().tooltips(focus == null ?
                Component.translatable("gtpm.multiblock.preview.tooltip.focus") :
                structureName(focus));
        @Nullable
        Integer layer = state.getVisibleLayer();
        layerButton.setText(layer == null ?
                Component.translatable("gtpm.multiblock.preview.button.layer_all") :
                Component.translatable("gtpm.multiblock.preview.button.layer", layer));
        hatchButton.textStyle(style -> style.textColor(state.isHighlightHatches() ? 0xff167129 : 0xff404040));
    }

    private static Component structureName(String name) {
        return Component.translatable(name.equals(MultiblockControllerMachine.DEFAULT_STRUCTURE) ?
                "gtpm.multiblock.configuration.main" : name);
    }

    private void updateDiagnosticText() {
        ObjectList<StructurePreview.Diagnostic> diagnostics = state.getPreview().diagnostics();
        diagnosticLabel.setText(diagnostics.isEmpty() ? Component.empty() :
                Component.translatable("gtpm.multiblock.preview.diagnostic",
                        diagnosticDescription(diagnostics.getFirst())));
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
        updateMaterialIcons();
        long cycle = System.currentTimeMillis() / 1000;
        for (var entry : predicateItems.object2ObjectEntrySet()) {
            var items = entry.getValue();
            entry.getKey().setItem(items.get((int) (cycle % items.size())), false);
        }
        @Nullable
        Level currentLevel = Minecraft.getInstance().level;
        if (currentLevel != sourceLevel) {
            rebuildForLevel(currentLevel);
        } else if (renderedViewRevision != state.getViewRevision()) {
            rebuildRenderedCore(false);
            updateNavigationText();
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        if (fullscreen && FTB_LIBRARY_LOADED) FTBSupport.setSidebarVisible(false);
        if (updateJEIMaterial != null) updateJEIMaterial.accept(getHoveredItem(materialViewport));
        if (updateJEIPredicate != null) updateJEIPredicate.accept(getHoveredItem(predicateViewport));
        super.drawBackgroundAdditional(context);
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
        if (fullscreen && FTB_LIBRARY_LOADED) FTBSupport.setSidebarVisible(true);
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

    private static final class FTBSupport {

        private static void setSidebarVisible(boolean visible) {
            var screen = Minecraft.getInstance().screen;
            if (screen == null) return;
            for (var child : screen.children()) {
                if (child instanceof SidebarGroupGuiButton sidebar) {
                    sidebar.visible = visible;
                    sidebar.active = visible;
                }
            }
        }
    }

    private static final class EMISupport {

        private static void recipeSlot(UIElement viewport, Supplier<ItemStack> hovered) {
            LDLibEMIPlugin.recipeSlot(viewport, () -> EmiStack.of(hovered.get()));
        }
    }

    private static final class JEISupport {

        private static Consumer<ItemStack> recipeSlot(UIElement viewport, Supplier<ItemStack> hovered) {
            return LDLibJEIPlugin.recipeSlot(viewport, IngredientIO.INPUT,
                    VanillaTypes.ITEM_STACK, () -> Stream.of(hovered.get()), ignored -> {});
        }
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
