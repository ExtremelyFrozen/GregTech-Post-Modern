package com.gregtechceu.gtceu.api.recipe.ui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.SteamTexture;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTDualProgressElement;
import com.gregtechceu.gtceu.api.gui.element.GTFluidSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTProgressBarElement;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.gui.texture.ProgressTexture;
import com.gregtechceu.gtceu.api.gui.texture.ResourceTexture;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.integration.emi.recipe.GTRecipeEMICategory;
import com.gregtechceu.gtceu.integration.jei.GTJEIPlugin;
import com.gregtechceu.gtceu.integration.jei.recipe.GTRecipeJEICategory;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.layout.LayoutProperties;
import com.lowdragmc.lowdraglib2.gui.ui.style.StyleValue;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import com.google.common.collect.Table;
import dev.emi.emi.api.EmiApi;
import dev.vfyjxf.taffy.style.TaffyDimension;
import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

@SuppressWarnings("UnusedReturnValue")
public class GTRecipeTypeUI {

    public static final DoubleSupplier XEI_PROGRESS = () -> Math.abs(System.currentTimeMillis() % 2000) / 2000.0;

    @Getter
    @Setter
    private Byte2ObjectMap<IGuiTexture> slotOverlays = new Byte2ObjectArrayMap<>();

    private final GTRecipeType recipeType;

    @Getter
    @Setter
    private ProgressTexture progressBarTexture = GuiTextures.progressBar(GuiTextures.PROGRESS_BAR_ARROW);
    @Setter
    private SteamTexture steamProgressBarTexture = null;
    @Setter
    private ProgressTexture.FillDirection steamMoveType = ProgressTexture.FillDirection.LEFT_TO_RIGHT;
    @Setter
    @Nullable
    protected RecipeUIBuilder uiBuilder;
    @Setter
    @Getter
    protected int maxTooltips = 3;

    private String customUICache;
    private boolean customUICacheLoaded;

    /**
     * @param recipeType the recipemap corresponding to this ui
     */
    public GTRecipeTypeUI(@NotNull GTRecipeType recipeType) {
        this.recipeType = recipeType;
    }

    public boolean hasCustomUI() {
        return getCustomUIXml() != null;
    }

    /**
     * Returns the active unbound recipe XML template as a fresh DOM document.
     */
    public Document createTemplateDocument() {
        String customXml = getCustomUIXml();
        if (customXml == null) {
            return RecipeUIXmlTemplate.createDocument(recipeType, this);
        }
        return parseCustomUI(customXml);
    }

    /**
     * Returns the active unbound recipe XML template for the LDLib2 text editor.
     */
    public String createTemplateXml() {
        String customXml = getCustomUIXml();
        if (customXml == null) {
            return RecipeUIXmlTemplate.serialize(RecipeUIXmlTemplate.createDocument(recipeType, this));
        }
        parseCustomUI(customXml);
        return customXml;
    }

    public UI createCustomUI() {
        String xml = getCustomUIXml();
        if (xml == null) {
            ResourceLocation location = getCustomUILocation();
            GTCEu.LOGGER.error("Cannot create custom LDLib2 recipe type UI because {} does not exist", location);
            throw new IllegalStateException("Custom LDLib2 recipe type UI does not exist: " + location);
        }

        Document document = parseCustomUI(xml);
        try {
            return UI.of(document);
        } catch (RuntimeException e) {
            GTCEu.LOGGER.error("Failed to create LDLib2 recipe type UI from {}", getCustomUILocation(), e);
            throw new IllegalStateException("Invalid LDLib2 recipe type UI: " + getCustomUILocation(), e);
        }
    }

    private Document parseCustomUI(String xml) {
        try {
            return RecipeUIXmlTemplate.parse(xml);
        } catch (IllegalArgumentException e) {
            GTCEu.LOGGER.error("Failed to parse LDLib2 recipe type UI template from {}",
                    getCustomUILocation(), e);
            throw new IllegalStateException("Invalid LDLib2 recipe type UI template: " +
                    getCustomUILocation(), e);
        }
    }

    @Nullable
    private String getCustomUIXml() {
        if (!this.customUICacheLoaded) {
            ResourceManager resourceManager = getResourceManager();
            if (resourceManager == null) {
                ResourceLocation location = getCustomUILocation();
                GTCEu.LOGGER.error("Cannot load LDLib2 recipe type UI {} without an active resource manager",
                        location);
                throw new IllegalStateException("Cannot load LDLib2 recipe type UI without a resource manager: " +
                        location);
            } else {
                ResourceLocation location = getCustomUILocation();
                var resource = resourceManager.getResource(location);
                if (resource.isEmpty()) {
                    this.customUICache = null;
                } else {
                    try (InputStream inputStream = resource.get().open()) {
                        this.customUICache = decodeUtf8(inputStream.readAllBytes(), location);
                    } catch (Exception e) {
                        GTCEu.LOGGER.error("Failed to load LDLib2 recipe type UI from {}", location, e);
                        throw new IllegalStateException("Failed to load LDLib2 recipe type UI from " + location, e);
                    }
                }
            }
            this.customUICacheLoaded = true;
        }
        return this.customUICache;
    }

    private static String decodeUtf8(byte[] bytes, ResourceLocation location) throws CharacterCodingException {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        String xml = decoder.decode(ByteBuffer.wrap(bytes)).toString();
        if (!xml.isEmpty() && xml.charAt(0) == '\uFEFF') {
            throw new IllegalArgumentException("Recipe type UI XML must be UTF-8 without BOM: " + location);
        }
        return xml;
    }

    private ResourceLocation getCustomUILocation() {
        return ResourceLocation.fromNamespaceAndPath(recipeType.registryName.getNamespace(),
                "ui/recipe_type/%s.xml".formatted(recipeType.registryName.getPath()));
    }

    @Nullable
    private ResourceManager getResourceManager() {
        if (GTCEu.isClientSide()) {
            return Minecraft.getInstance().getResourceManager();
        } else if (GTCEu.getMinecraftServer() != null) {
            return GTCEu.getMinecraftServer().getResourceManager();
        }
        return null;
    }

    public void reloadCustomUI() {
        this.customUICache = null;
        this.customUICacheLoaded = false;
    }

    public record RecipeHolder(DoubleSupplier progressSupplier,
                               Table<IO, RecipeCapability<?>, Object> storages,
                               DataComponentMap data,
                               List<RecipeCondition<?>> conditions,
                               boolean isSteam,
                               boolean isHighPressure) {}

    public record RecipeUISize(int width, int height) {}

    @FunctionalInterface
    public interface RecipeUIBuilder {

        void accept(GTRecipeDefinition recipe, UIElement root, RecipeUISize rootSize);
    }

    public UI createUITemplate(DoubleSupplier progressSupplier,
                               Table<IO, RecipeCapability<?>, Object> storages,
                               DataComponentMap data,
                               List<RecipeCondition<?>> conditions,
                               boolean isSteam,
                               boolean isHighPressure) {
        UI ui = !isSteam && hasCustomUI() ? createCustomUI() :
                createDefaultUI(isSteam, isHighPressure);
        bindRecipeUI(ui.rootElement,
                new RecipeHolder(progressSupplier, storages, data, conditions, isSteam, isHighPressure));
        return ui;
    }

    public UI createUITemplate(DoubleSupplier progressSupplier,
                               Table<IO, RecipeCapability<?>, Object> storages,
                               DataComponentMap data,
                               List<RecipeCondition<?>> conditions) {
        return createUITemplate(progressSupplier, storages, data, conditions, false, false);
    }

    public RecipeUISize getXEIRecipeUISize() {
        RecipeUISize rawSize = getRecipeUISize(false, false);
        return new RecipeUISize(Math.max(rawSize.width(), 150),
                rawSize.height() + 5 + getPropertyHeightShift());
    }

    public RecipeUISize getRecipeUISize(boolean isSteam, boolean isHighPressure) {
        Document document = isSteam ? RecipeUIXmlTemplate.createDocument(recipeType, this) :
                createTemplateDocument();
        return getRecipeUISize(document);
    }

    /**
     * Reads the fixed pixel dimensions declared by a recipe XML document.
     */
    public static RecipeUISize getRecipeUISize(Document document) {
        Element root = getRoot(document);
        var properties = Stylesheet.parseStyleValues(root.getAttribute("style"));
        return new RecipeUISize(
                fixedRecipeUISize(getDimension(properties.get(LayoutProperties.WIDTH)), "width"),
                fixedRecipeUISize(getDimension(properties.get(LayoutProperties.HEIGHT)), "height"));
    }

    @Nullable
    private static TaffyDimension getDimension(@Nullable StyleValue<?> styleValue) {
        if (styleValue != null && styleValue.compute() instanceof TaffyDimension dimension) {
            return dimension;
        }
        return null;
    }

    private static Element getRoot(Document document) {
        Element documentRoot = document.getDocumentElement();
        if (documentRoot == null) {
            throw new IllegalArgumentException("LDLib2 recipe UI XML does not contain a document root");
        }
        Element root = null;
        for (Node child = documentRoot.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && element.getTagName().equals("root")) {
                if (root != null) {
                    throw new IllegalArgumentException(
                            "LDLib2 recipe UI XML contains more than one direct root element");
                }
                root = element;
            }
        }
        if (root == null) {
            throw new IllegalArgumentException("LDLib2 recipe UI XML does not contain a direct root element");
        }
        return root;
    }

    private static int fixedRecipeUISize(TaffyDimension dimension, String axis) {
        if (dimension == null || !dimension.isLength()) {
            GTCEu.LOGGER.error("LDLib2 recipe UI {} must use a fixed pixel size, got {}", axis, dimension);
            throw new IllegalArgumentException("LDLib2 recipe UI " + axis + " must use a fixed pixel size");
        }
        return Math.round(dimension.getValue());
    }

    private UI createDefaultUI(boolean isSteam, boolean isHighPressure) {
        var inputs = addInventorySlotGroup(false, isSteam, isHighPressure);
        var outputs = addInventorySlotGroup(true, isSteam, isHighPressure);
        var maxWidth = Math.max(inputs.width(), outputs.width());
        var width = 2 * maxWidth + 40;
        var height = Math.max(inputs.height(), outputs.height());
        var root = new UIElement();
        root.layout(layout -> {
            layout.width(width);
            layout.height(height);
            layout.positionType(TaffyPosition.ABSOLUTE);
        });

        inputs.element().layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left((maxWidth - inputs.width()) / 2f);
            layout.top((height - inputs.height()) / 2f);
        });
        outputs.element().layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(maxWidth + 40 + (maxWidth - outputs.width()) / 2f);
            layout.top((height - outputs.height()) / 2f);
        });
        root.addChildren(inputs.element(), outputs.element());

        var progress = new GTProgressBarElement();
        progress.setId("progress");
        progress.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(maxWidth + 10);
            layout.top(height / 2f - 10);
            layout.width(20);
            layout.height(20);
        });
        configureProgressTexture(progress, isSteam, isHighPressure);
        root.addChild(progress);
        return UI.of(root);
    }

    /**
     * Binds progress and capability storage to an already parsed recipe XML tree.
     */
    public void bindRecipeUI(UIElement root, RecipeHolder recipeHolder) {
        var isXEI = recipeHolder.progressSupplier == XEI_PROGRESS;
        List<UIElement> progressElements = new ArrayList<>();
        root.selectId("progress", GTProgressBarElement.class)
                .forEach(progress -> {
                    progress.setProgressSupplier(recipeHolder.progressSupplier);
                    progressElements.add(progress);
                });
        root.selectId("progress", GTDualProgressElement.class)
                .forEach(progress -> {
                    progress.setProgressSupplier(recipeHolder.progressSupplier);
                    progressElements.add(progress);
                });
        if (progressElements.isEmpty()) {
            GTCEu.LOGGER.error("LDLib2 recipe UI for {} has no bindable progress element", recipeType.registryName);
            throw new IllegalStateException("LDLib2 recipe UI has no bindable progress element: " +
                    recipeType.registryName);
        }

        if (!isXEI && GTCEu.MODS.isAnyRecipeViewerLoaded()) {
            progressElements.forEach(this::addRecipeViewerButton);
        }

        for (var capabilityEntry : recipeHolder.storages.rowMap().entrySet()) {
            IO io = capabilityEntry.getKey();
            for (var storagesEntry : capabilityEntry.getValue().entrySet()) {
                RecipeCapability<?> cap = storagesEntry.getKey();
                Object storage = storagesEntry.getValue();
                var elementClass = cap.getElementClass();
                if (elementClass != null) {
                    root.selectRegex("^%s_[0-9]+$".formatted(cap.slotName(io)), elementClass)
                            .forEach(element -> {
                                var index = elementIdIndex(element);
                                cap.applyElementInfo(element, index, isXEI, io, recipeHolder, recipeType, null,
                                        null, storage, 0, 0);
                            });
                }
            }
        }
    }

    private void addRecipeViewerButton(UIElement progress) {
        GTButtonElement button = new GTButtonElement();
        button.noText();
        button.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(0);
            layout.top(0);
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        button.buttonStyle(style -> style
                .baseTexture(IGuiTexture.EMPTY)
                .hoverTexture(IGuiTexture.EMPTY)
                .pressedTexture(IGuiTexture.EMPTY));
        button.setOnClick(event -> showRecipeViewerCategory());
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> event.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gtpm.recipe_type.show_recipes")), null, null, null));
        progress.addChild(button);
    }

    private void showRecipeViewerCategory() {
        if (GTCEu.MODS.isEMILoaded()) {
            EmiApi.displayRecipeCategory(GTRecipeEMICategory.machineCategory(recipeType.getCategory()));
        } else if (GTCEu.MODS.isJEILoaded()) {
            GTJEIPlugin.jeiRuntime.getRecipesGui().showTypes(
                    recipeType.getCategories().stream()
                            .filter(GTRecipeCategory::isXEIVisible)
                            .map(GTRecipeJEICategory::machineType)
                            .collect(Collectors.toList()));
        }
    }

    public void applyRecipeContent(UI ui,
                                   Table<IO, RecipeCapability<?>, List<Content>> contentTable,
                                   GTRecipeDefinition recipe,
                                   int recipeTier,
                                   int chanceTier) {
        applyRecipeContent(ui.rootElement, contentTable, recipe, recipeTier, chanceTier);
    }

    public void applyRecipeContent(UIElement root,
                                   Table<IO, RecipeCapability<?>, List<Content>> contentTable,
                                   GTRecipeDefinition recipe,
                                   int recipeTier,
                                   int chanceTier) {
        for (var capabilityEntry : contentTable.rowMap().entrySet()) {
            IO io = capabilityEntry.getKey();
            for (var contentsEntry : capabilityEntry.getValue().entrySet()) {
                RecipeCapability<?> cap = contentsEntry.getKey();
                var elementClass = cap.getElementClass();
                if (elementClass == null) {
                    continue;
                }
                List<Content> contents = contentsEntry.getValue();
                int nonTickCount = (io == IO.IN ? recipe.getInputContents(cap) : recipe.getOutputContents(cap))
                        .size();
                root.selectRegex("^%s_[0-9]+$".formatted(cap.slotName(io)), elementClass)
                        .forEach(element -> {
                            int index = elementIdIndex(element);
                            if (index >= 0 && index < contents.size()) {
                                Content content = contents.get(index);
                                cap.applyElementInfo(element, index, true, io, null, recipe.getType(), recipe,
                                        content, null, recipeTier, chanceTier);
                                setContentOverlay(element,
                                        content.createOverlay(index >= nonTickCount, recipeTier, chanceTier,
                                                recipe.getType().getChanceFunction()));
                            }
                        });
            }
        }
    }

    private ElementGroup addInventorySlotGroup(boolean isOutputs, boolean isSteam,
                                               boolean isHighPressure) {
        int maxCount = 0;
        int totalR = 0;
        Object2IntSortedMap<RecipeCapability<?>> map = new Object2IntAVLTreeMap<>(RecipeCapability.COMPARATOR);
        if (isOutputs) {
            for (var value : recipeType.maxOutputs.object2IntEntrySet()) {
                if (value.getKey().doRenderSlot) {
                    int val = value.getIntValue();
                    if (val > maxCount) {
                        maxCount = Math.min(val, 3);
                    }
                    totalR += (val + 2) / 3;
                    map.put(value.getKey(), val);
                }
            }
        } else {
            for (var value : recipeType.maxInputs.object2IntEntrySet()) {
                if (value.getKey().doRenderSlot) {
                    int val = value.getIntValue();
                    if (val > maxCount) {
                        maxCount = Math.min(val, 3);
                    }
                    totalR += (val + 2) / 3;
                    map.put(value.getKey(), val);
                }
            }
        }
        int width = maxCount * 18 + 8;
        int height = totalR * 18 + 8;
        UIElement group = new UIElement();
        group.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.width(width);
            layout.height(height);
        });
        int index = 0;
        for (var entry : map.object2IntEntrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            int capCount = entry.getIntValue();
            for (int slotIndex = 0; slotIndex < capCount; slotIndex++) {
                var slot = cap.createElement();
                if (slot == null) {
                    IO io = isOutputs ? IO.OUT : IO.IN;
                    GTCEu.LOGGER.error("Recipe capability '{}' declares a rendered {} slot without a LDLib2 element",
                            cap.name, io);
                    throw new IllegalStateException("Missing LDLib2 recipe element for capability " + cap.name +
                            " " + io);
                }
                int slotX = (index % 3) * 18 + 4;
                int slotY = (index / 3) * 18 + 4;
                slot.layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.left(slotX);
                    layout.top(slotY);
                    layout.width(18);
                    layout.height(18);
                });
                setSlotBackground(slot,
                        getOverlaysForSlot(isOutputs, cap, slotIndex == capCount - 1, isSteam, isHighPressure));
                slot.setId(cap.slotName(isOutputs ? IO.OUT : IO.IN, slotIndex));
                group.addChild(slot);
                index++;
            }
            index += (3 - (index % 3)) % 3;
        }
        return new ElementGroup(group, width, height);
    }

    private void configureProgressTexture(GTProgressBarElement progress, boolean isSteam,
                                          boolean isHighPressure) {
        ProgressTexture texture = getProgressTexture(isSteam, isHighPressure);
        progress.setProgressTexture(texture.getEmptyBarArea(), texture.getFilledBarArea())
                .setFillDirection(toFillDirection(texture.getFillDirection()));
    }

    private ProgressTexture getProgressTexture(boolean isSteam, boolean isHighPressure) {
        if (!isSteam || steamProgressBarTexture == null) {
            return progressBarTexture;
        }
        IGuiTexture steamTexture = steamProgressBarTexture.get(isHighPressure);
        if (steamTexture instanceof ResourceTexture resourceTexture) {
            return GuiTextures.progressBar(resourceTexture, steamMoveType);
        }
        GTCEu.LOGGER.error("Steam progress bar texture must be a resource texture, got {}",
                steamTexture.getClass().getName());
        throw new IllegalArgumentException("Unsupported steam progress bar texture: " +
                steamTexture.getClass().getName());
    }

    private static FillDirection toFillDirection(ProgressTexture.FillDirection fillDirection) {
        return switch (fillDirection) {
            case RIGHT_TO_LEFT -> FillDirection.RIGHT_TO_LEFT;
            case UP_TO_DOWN -> FillDirection.UP_TO_DOWN;
            case DOWN_TO_UP -> FillDirection.DOWN_TO_UP;
            case LEFT_TO_RIGHT, ALWAYS_FULL -> FillDirection.LEFT_TO_RIGHT;
        };
    }

    private static void setSlotBackground(UIElement element, IGuiTexture texture) {
        if (element instanceof GTItemSlotElement slot) {
            slot.setBackgroundTexture(texture);
        } else if (element instanceof GTFluidSlotElement tank) {
            tank.setBackgroundTexture(texture);
        } else {
            element.getStyle().backgroundTexture(texture);
        }
    }

    private static void setContentOverlay(UIElement element, IGuiTexture texture) {
        if (element instanceof GTItemSlotElement slot) {
            slot.setContentOverlay(texture);
        } else if (element instanceof GTFluidSlotElement tank) {
            tank.setContentOverlay(texture);
        } else {
            GTCEu.LOGGER.error("LDLib2 recipe element {} does not support content overlays",
                    element.getClass().getName());
            throw new IllegalArgumentException("Unsupported LDLib2 recipe element: " + element.getClass().getName());
        }
    }

    private static int elementIdIndex(UIElement element) {
        var id = element.getId();
        var separator = id.lastIndexOf('_');
        if (separator < 0 || separator == id.length() - 1) {
            GTCEu.LOGGER.error("Invalid LDLib2 recipe element id '{}'", id);
            throw new IllegalArgumentException("Invalid LDLib2 recipe element id: " + id);
        }
        try {
            return Integer.parseInt(id.substring(separator + 1));
        } catch (NumberFormatException e) {
            GTCEu.LOGGER.error("Invalid LDLib2 recipe element id '{}'", id, e);
            throw e;
        }
    }

    private record ElementGroup(UIElement element, int width, int height) {}

    protected IGuiTexture getOverlaysForSlot(boolean isOutput, RecipeCapability<?> capability, boolean isLast,
                                             boolean isSteam, boolean isHighPressure) {
        IGuiTexture base = capability == FluidRecipeCapability.CAP ? GuiTextures.FLUID_SLOT :
                (isSteam ? GuiTextures.SLOT_STEAM.get(isHighPressure) : GuiTextures.SLOT);
        byte overlayKey = (byte) ((isOutput ? 2 : 0) + (capability == FluidRecipeCapability.CAP ? 1 : 0) +
                (isLast ? 4 : 0));
        if (slotOverlays.containsKey(overlayKey)) {
            return GuiTextures.group(base, slotOverlays.get(overlayKey));
        }
        return base;
    }

    /**
     * @return the height used to determine size of background texture in JEI
     */
    public int getPropertyHeightShift() {
        int maxPropertyCount = maxTooltips + recipeType.getDataInfos().size() + recipeType.getMinRecipeConditions();
        return maxPropertyCount * 10; // GTRecipeXEIHelper#LINE_HEIGHT
    }

    public void appendXEIUI(GTRecipeDefinition recipe, UIElement root) {
        appendXEIUI(recipe, root, getXEIRecipeUISize());
    }

    public void appendXEIUI(GTRecipeDefinition recipe, UIElement root, RecipeUISize rootSize) {
        if (uiBuilder != null) {
            uiBuilder.accept(recipe, root, rootSize);
        }
    }

    public GTRecipeTypeUI setSlotOverlay(boolean isOutput, boolean isFluid, IGuiTexture slotOverlay) {
        return this.setSlotOverlay(isOutput, isFluid, false, slotOverlay).setSlotOverlay(isOutput, isFluid, true,
                slotOverlay);
    }

    public GTRecipeTypeUI setSlotOverlay(boolean isOutput, boolean isFluid, boolean isLast, IGuiTexture slotOverlay) {
        this.slotOverlays.put((byte) ((isOutput ? 2 : 0) + (isFluid ? 1 : 0) + (isLast ? 4 : 0)), slotOverlay);
        return this;
    }

    public GTRecipeTypeUI setProgressBar(ProgressTexture progressBar) {
        this.progressBarTexture = progressBar;
        return this;
    }
}
