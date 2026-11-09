package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.texture.ColorRectTexture;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectList;
import mezz.jei.api.constants.VanillaTypes;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Two compact item columns. The caller owns the choices and applies selections to its own state. */
@NullMarked
public final class BlockChoiceList extends UIElement {

    public static final int COLUMN_WIDTH = 48;
    public static final int WIDTH = COLUMN_WIDTH * 2 + 2;

    private final GTScrollerViewElement groups = scroller();
    private final GTScrollerViewElement candidates = scroller();
    private final BiConsumer<String, ResourceLocation> select;
    private Object2ObjectMap<String, ObjectList<ResourceLocation>> choices = Object2ObjectMaps.emptyMap();
    private Object2ObjectMap<String, ResourceLocation> selected = Object2ObjectMaps.emptyMap();
    private @Nullable String expandedGroup;
    private boolean interactive = true;
    private Consumer<ItemStack> updateJEI = ignored -> {};

    public BlockChoiceList(BiConsumer<String, ResourceLocation> select) {
        this.select = select;
        setOverflowVisible(false);
        addChildren(groups, candidates);
        candidates.setDisplay(false);
    }

    public void resize(int x, int y, int height) {
        UITemplate.setBounds(this, x, y, WIDTH, height);
        UITemplate.setBounds(groups, 0, 0, COLUMN_WIDTH, height);
        UITemplate.setBounds(candidates, COLUMN_WIDTH + 2, 0, COLUMN_WIDTH, height);
    }

    public void setChoices(Object2ObjectMap<String, ObjectList<ResourceLocation>> choices,
                           Object2ObjectMap<String, ResourceLocation> selected) {
        updateJEI.accept(ItemStack.EMPTY);
        this.choices = choices;
        this.selected = selected;
        if (!choices.containsKey(expandedGroup)) expandedGroup = null;
        rebuild();
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        refreshButtons();
    }

    public void close() {
        expandedGroup = null;
        candidates.setDisplay(false);
        updateJEI.accept(ItemStack.EMPTY);
        refreshButtons();
    }

    private void rebuild() {
        groups.clearAllScrollViewChildren();
        int row = 0;
        for (var entry : choices.object2ObjectEntrySet()) {
            String group = entry.getKey();
            var item = icon(entry.getValue().getFirst(), row * 18);
            var expand = button(row * 18, group.equals(expandedGroup) ? "◀" : "▶");
            expand.setId(group);
            expand.getStyle().tooltips(Component.translatable("gtpm.multiblock.category." + group));
            expand.setOnClick(event -> {
                expandedGroup = group.equals(expandedGroup) ? null : group;
                updateJEI.accept(ItemStack.EMPTY);
                candidates.verticalScroller(scroll -> scroll.setNormalizedValue(0));
                rebuildCandidates();
                refreshButtons();
                event.stopPropagation();
            });
            groups.addScrollViewChildren(item, expand);
            row++;
        }
        groups.viewContainer.getLayout().width(44).height(row * 18);
        rebuildCandidates();
        refreshButtons();
    }

    private void rebuildCandidates() {
        candidates.clearAllScrollViewChildren();
        candidates.setDisplay(expandedGroup != null);
        if (expandedGroup == null) return;
        String group = expandedGroup;
        var values = choices.get(group);
        ResourceLocation current = selected.getOrDefault(group, values.getFirst());
        for (int i = 0; i < values.size(); i++) {
            ResourceLocation id = values.get(i);
            var choose = button(i * 18, id.equals(current) ? "✔" : "✘");
            choose.textStyle(style -> style.textColor(id.equals(current) ? 0xff169530 : 0xff404040));
            choose.setActive(interactive);
            choose.getStyle().tooltips(BuiltInRegistries.BLOCK.get(id).getName());
            choose.setOnClick(event -> {
                if (!id.equals(current)) select.accept(group, id);
                event.stopPropagation();
            });
            candidates.addScrollViewChildren(icon(id, i * 18), choose);
        }
        candidates.viewContainer.getLayout().width(44).height(values.size() * 18);
    }

    private void refreshButtons() {
        for (var child : groups.viewContainer.getChildren()) {
            if (child instanceof GTButtonElement button) {
                button.setText(Component.literal(button.getId().equals(expandedGroup) ? "◀" : "▶"));
                String group = button.getId();
                button.textStyle(style -> style.textColor(!choices.get(group).getFirst().equals(selected.get(group)) ?
                        0xff169fb5 : 0xff404040));
            }
        }
        for (var child : candidates.viewContainer.getChildren()) {
            if (child instanceof GTButtonElement button) button.setActive(interactive);
        }
    }

    private static GTItemSlotElement icon(ResourceLocation id, int y) {
        var icon = new GTItemSlotElement();
        UITemplate.setBounds(icon, 4, y, 18, 18);
        icon.setBackgroundTexture(IGuiTexture.EMPTY);
        icon.setItem(BuiltInRegistries.BLOCK.get(id).asItem().getDefaultInstance(), false);
        return icon;
    }

    private static GTButtonElement button(int y, String text) {
        var button = new GTButtonElement(28, y + 3, 12, 12);
        button.setButtonTexture(GuiTextures.VANILLA_BUTTON).setText(Component.literal(text));
        button.getLayout().paddingAll(0);
        button.textStyle(style -> style.fontSize(9).textShadow(false).textColor(0xff404040));
        return button;
    }

    private static GTScrollerViewElement scroller() {
        var scroller = new GTScrollerViewElement();
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(0));
        scroller.viewPort(port -> {
            port.getLayout().paddingAll(0);
            port.getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
            port.setOverflowVisible(false);
        });
        scroller.verticalScroller(scroll -> {
            scroll.getLayout().width(2);
            scroll.getStyle().backgroundTexture(IGuiTexture.EMPTY);
            scroll.headButton.setDisplay(false);
            scroll.tailButton.setDisplay(false);
            var bar = new ColorRectTexture(0xffffffff).setRadius(1);
            scroll.scrollBar.buttonStyle(style -> style.baseTexture(bar).hoverTexture(bar).pressedTexture(bar));
        });
        return scroller;
    }

    /** Recipe widgets are collected once, so keep the binding on this surviving viewport. */
    @OnlyIn(Dist.CLIENT)
    public void enableRecipeLookup() {
        if (GTCEu.MODS.isEMILoaded()) {
            LDLibEMIPlugin.recipeSlot(this, () -> EmiStack.of(getHoveredItem()));
        }
        if (GTCEu.MODS.isJEILoaded()) {
            updateJEI = LDLibJEIPlugin.recipeSlot(this, IngredientIO.NONE, VanillaTypes.ITEM_STACK,
                    Stream::empty, ignored -> {});
        }
    }

    @OnlyIn(Dist.CLIENT)
    private ItemStack getHoveredItem() {
        var ui = getModularUI();
        var hit = ui.ui.rootElement.hitTest(ui.getLastMouseX(), ui.getLastMouseY());
        var hovered = hit == null ? null : hit.getA();
        if (!(hovered instanceof GTItemSlotElement item)) return ItemStack.EMPTY;
        for (UIElement parent = item.getParent(); parent != null; parent = parent.getParent()) {
            if (parent == this) return item.getValue();
        }
        return ItemStack.EMPTY;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawBackgroundAdditional(GUIContext context) {
        updateJEI.accept(getHoveredItem());
        super.drawBackgroundAdditional(context);
    }
}
