package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.texture.ColorRectTexture;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.gui.texture.ItemStackTexture;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;

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

    private static GTImageElement icon(ResourceLocation id, int y) {
        var stack = BuiltInRegistries.BLOCK.get(id).asItem().getDefaultInstance();
        var icon = new GTImageElement(5, y + 1, 16, 16, new ItemStackTexture(stack));
        icon.addEventListener(UIEvents.HOVER_TOOLTIPS,
                event -> event.hoverTooltips = new HoverTooltips(DrawerHelper.getItemToolTip(stack),
                        stack.getTooltipImage().orElse(null), null, stack));
        return icon;
    }

    private static GTButtonElement button(int y, String text) {
        var button = new GTButtonElement(28, y + 3, 12, 12);
        button.setButtonTexture(GuiTextures.VANILLA_BUTTON).setText(Component.literal(text));
        button.getLayout().paddingAll(0);
        button.text.getLayout().widthPercent(100).heightPercent(100).marginAll(0).minWidth(0);
        button.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false)
                .fontSize(9).textShadow(false).textColor(0xff404040));
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
}
