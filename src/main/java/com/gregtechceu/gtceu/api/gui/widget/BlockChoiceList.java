package com.gregtechceu.gtceu.api.gui.widget;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.texture.ColorRectTexture;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.gui.texture.ItemStackTexture;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiConsumer;

/** Category entries open an independent candidate panel; the caller owns and applies selections. */
@NullMarked
public final class BlockChoiceList extends UIElement {

    public static final int COLUMN_WIDTH = 48;
    public static final int WIDTH = COLUMN_WIDTH * 2 + 4;

    private final GTScrollerViewElement groups = scroller();
    private final BiConsumer<String, ResourceLocation> select;
    private Object2ObjectMap<String, ObjectList<ResourceLocation>> choices = Object2ObjectMaps.emptyMap();
    private Object2ObjectMap<String, ResourceLocation> selected = Object2ObjectMaps.emptyMap();
    private @Nullable String expandedGroup;
    private boolean interactive = true;

    public BlockChoiceList(BiConsumer<String, ResourceLocation> select) {
        this.select = select;
        setOverflowVisible(false);
        addChild(groups);
    }

    public void resize(int x, int y, int height) {
        UITemplate.setBounds(this, x, y, COLUMN_WIDTH, height);
        UITemplate.setBounds(groups, 0, 0, COLUMN_WIDTH, height);
    }

    public void setChoices(Object2ObjectMap<String, ObjectList<ResourceLocation>> choices,
                           Object2ObjectMap<String, ResourceLocation> selected) {
        this.choices = choices;
        this.selected = selected;
        if (!choices.containsKey(expandedGroup)) close();
        rebuild();
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        refreshButtons();
        if (Candidates.opened != null && Candidates.opened.owner == this) Candidates.opened.refreshSelection();
    }

    public void close() {
        expandedGroup = null;
        Candidates.close(this);
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
                if (group.equals(expandedGroup)) close();
                else {
                    close();
                    expandedGroup = group;
                    Candidates.open(this, group);
                }
                refreshButtons();
                event.stopPropagation();
            });
            groups.addScrollViewChildren(item, expand);
            row++;
        }
        groups.viewContainer.getLayout().width(44).height(row * 18);
        if (Candidates.opened != null && Candidates.opened.owner == this) Candidates.opened.rebuild();
        refreshButtons();
    }

    private void refreshButtons() {
        for (var child : groups.viewContainer.getChildren()) {
            if (child instanceof GTButtonElement button) {
                button.setText(Component.literal(button.getId().equals(expandedGroup) ? "◀" : "▶"));
                String group = button.getId();
                var first = choices.get(group).getFirst();
                button.textStyle(style -> style.textColor(!first.equals(selected.getOrDefault(group, first)) ?
                        0xff169fb5 : 0xff404040));
            }
        }
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (Candidates.opened != null && Candidates.opened.owner == this) Candidates.opened.ownerRendered = true;
    }

    @Override
    protected void onRemoved() {
        close();
        super.onRemoved();
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

    /** Uses the screen's overlay/input events so candidates also work outside a recipe viewer. */
    @OnlyIn(Dist.CLIENT)
    @EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
    public static final class Candidates {

        private static @Nullable Candidates opened;
        private final BlockChoiceList owner;
        private final String group;
        private final Screen screen = Minecraft.getInstance().screen;
        private final GTScrollerViewElement root = new GTScrollerViewElement();
        private final ModularUI ui = ModularUI.of(UI.of(root));
        private final Object2ObjectMap<ResourceLocation, GTButtonElement> buttons = new Object2ObjectLinkedOpenHashMap<>();
        private ResourceLocation current;
        private boolean captured;
        private boolean ownerRendered;

        private Candidates(BlockChoiceList owner, String group) {
            this.owner = owner;
            this.group = group;
            current = owner.selected.getOrDefault(group, owner.choices.get(group).getFirst());
            root.scrollerStyle(style -> style.mode(ScrollerMode.HORIZONTAL)
                    .verticalScrollDisplay(ScrollDisplay.NEVER).horizontalScrollDisplay(ScrollDisplay.AUTO)
                    .scrollerViewStyle(0));
            root.viewPort(port -> {
                port.getLayout().paddingAll(0);
                port.setOverflowVisible(false);
            });
            root.horizontalScroller(scroll -> {
                scroll.getLayout().height(2);
                scroll.getStyle().backgroundTexture(IGuiTexture.EMPTY);
                scroll.headButton.setDisplay(false);
                scroll.tailButton.setDisplay(false);
                var bar = new ColorRectTexture(0xffffffff).setRadius(1);
                scroll.scrollBar.buttonStyle(style -> style.baseTexture(bar).hoverTexture(bar).pressedTexture(bar));
            });
        }

        private static void open(BlockChoiceList owner, String group) {
            if (opened != null) opened.owner.close();
            opened = new Candidates(owner, group);
            opened.rebuild();
        }

        private static void close(BlockChoiceList owner) {
            if (opened != null && opened.owner == owner) {
                opened.ui.onRemoved();
                opened = null;
            }
        }

        private void rebuild() {
            root.clearAllScrollViewChildren();
            buttons.clear();
            var values = owner.choices.get(group);
            current = owner.selected.getOrDefault(group, values.getFirst());
            var anchor = owner.getWorldMouse(owner.getContentX() + COLUMN_WIDTH + 4, owner.getContentY());
            var parent = owner.getParent();
            var bottom = parent.getWorldMouse(parent.getContentX(),
                    parent.getContentY() + parent.getContentHeight() - 4);
            int rows = Math.max(1, ((int) (bottom.y - anchor.y) - 10) / 18);
            int columns = (values.size() + rows - 1) / rows;
            int contentWidth = columns * (COLUMN_WIDTH + 4) - 4;
            int height = rows * 18 + 10;
            int x = Math.min(Math.round(anchor.x), screen.width - COLUMN_WIDTH - 4);
            UITemplate.setBounds(root, x, Math.round(anchor.y), Math.min(contentWidth, screen.width - x - 4), height);
            root.getLayout().positionType(TaffyPosition.ABSOLUTE);
            for (int column = 0; column < columns; column++) {
                var panel = UITemplate.setBounds(new UIElement(), column * (COLUMN_WIDTH + 4), 0, COLUMN_WIDTH,
                        height - 2);
                panel.getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
                root.addScrollViewChild(panel);
                for (int row = 0; row < rows && column * rows + row < values.size(); row++) {
                    ResourceLocation id = values.get(column * rows + row);
                    var choose = button(row * 18 + 4, "");
                    choose.getStyle().tooltips(BuiltInRegistries.BLOCK.get(id).getName());
                    choose.setOnClick(event -> {
                        if (!id.equals(current)) owner.select.accept(group, id);
                        current = id;
                        refreshSelection();
                        event.stopPropagation();
                    });
                    panel.addChildren(icon(id, row * 18 + 4), choose);
                    buttons.put(id, choose);
                }
            }
            root.viewContainer.getLayout().width(contentWidth).height(height - 2);
            refreshSelection();
            ui.setScreenAndInit(screen);
        }

        private void refreshSelection() {
            for (var entry : buttons.object2ObjectEntrySet()) {
                boolean selected = entry.getKey().equals(current);
                var button = entry.getValue();
                button.setText(Component.literal(selected ? "✔" : "✘"));
                button.textStyle(style -> style.textColor(selected ? 0xff169530 : 0xff404040));
                button.setActive(owner.interactive);
            }
        }

        @SubscribeEvent
        public static void beforeRender(ScreenEvent.Render.Pre event) {
            if (opened != null) opened.ownerRendered = false;
        }

        @SubscribeEvent
        public static void render(ScreenEvent.Render.Post event) {
            if (opened == null) return;
            if (opened.screen != event.getScreen() || !opened.ownerRendered) {
                opened.owner.close();
                return;
            }
            opened.ui.getWidget().render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(),
                    event.getPartialTick());
        }

        @SubscribeEvent
        public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
            if (opened == null || opened.screen != event.getScreen()) return;
            var panel = opened;
            if (panel.ui.hitTestAtScreen((float) event.getMouseX(), (float) event.getMouseY()) != null) {
                panel.ui.getWidget().mouseMoved(event.getMouseX(), event.getMouseY());
                panel.ui.getWidget().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
                panel.captured = true;
                event.setCanceled(true);
            } else if (!panel.owner.isAncestorOf(
                    panel.owner.getModularUI().hitTestAtScreen((float) event.getMouseX(), (float) event.getMouseY()))) {
                        panel.owner.close();
                    }
        }

        @SubscribeEvent
        public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
            if (opened == null || !opened.captured || opened.screen != event.getScreen()) return;
            opened.ui.getWidget().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
            opened.captured = false;
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
            if (opened != null && opened.captured && opened.screen == event.getScreen()) {
                opened.ui.getWidget().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(),
                        event.getDragX(), event.getDragY());
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
            if (opened != null && opened.screen == event.getScreen() &&
                    opened.ui.hitTestAtScreen((float) event.getMouseX(), (float) event.getMouseY()) != null) {
                opened.ui.getWidget().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(),
                        event.getScrollDeltaY());
                event.setCanceled(true);
            }
        }

        @SubscribeEvent
        public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
            if (opened != null && opened.screen == event.getScreen() && event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
                opened.owner.close();
                event.setCanceled(true);
            }
        }
    }
}
