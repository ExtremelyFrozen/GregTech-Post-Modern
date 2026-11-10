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
import net.neoforged.bus.api.EventPriority;
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

/** Anchors independent category and candidate lists beside the caller's page. */
@NullMarked
public final class BlockChoiceList extends UIElement {

    public static final int COLUMN_WIDTH = 48;
    public static final int WIDTH = COLUMN_WIDTH * 2 + 4;

    private final BiConsumer<String, ResourceLocation> select;
    private Object2ObjectMap<String, ObjectList<ResourceLocation>> choices = Object2ObjectMaps.emptyMap();
    private Object2ObjectMap<String, ResourceLocation> selected = Object2ObjectMaps.emptyMap();
    private @Nullable String expandedGroup;
    private boolean interactive = true;

    public BlockChoiceList(BiConsumer<String, ResourceLocation> select) {
        this.select = select;
        setAllowHitTest(false);
    }

    public void resize(int x, int y, int height) {
        // Cover the caller's content so recipe clipping cannot discard the invisible anchor.
        UITemplate.setBounds(this, 0, y, x - 4, height);
    }

    public void setChoices(Object2ObjectMap<String, ObjectList<ResourceLocation>> choices,
                           Object2ObjectMap<String, ResourceLocation> selected) {
        this.choices = choices;
        this.selected = selected;
        if (!choices.containsKey(expandedGroup)) expandedGroup = null;
        if (Panels.opened != null && Panels.opened.owner == this) Panels.opened.dirty = true;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        if (Panels.opened != null && Panels.opened.owner == this && !Panels.opened.dirty) {
            Panels.opened.refreshButtons();
        }
    }

    public void close() {
        expandedGroup = null;
        Panels.close(this);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (choices.isEmpty()) return;
        var panels = Panels.opened;
        if (panels == null || panels.owner != this || panels.screen != Minecraft.getInstance().screen) {
            panels = Panels.open(this);
        }
        panels.ownerRendered = true;
        panels.updatePosition();
    }

    @Override
    protected void onRemoved() {
        close();
        super.onRemoved();
    }

    private static GTImageElement icon(ResourceLocation id, int y) {
        var stack = BuiltInRegistries.BLOCK.get(id).asItem().getDefaultInstance();
        var icon = new GTImageElement(1, y + 1, 16, 16, new ItemStackTexture(stack));
        icon.addEventListener(UIEvents.HOVER_TOOLTIPS,
                event -> event.hoverTooltips = new HoverTooltips(DrawerHelper.getItemToolTip(stack),
                        stack.getTooltipImage().orElse(null), null, stack));
        return icon;
    }

    private static GTButtonElement button(int y) {
        var button = new GTButtonElement(24, y + 3, 12, 12);
        button.setButtonTexture(GuiTextures.VANILLA_BUTTON);
        button.getLayout().paddingAll(0);
        button.text.getLayout().widthPercent(100).heightPercent(100).marginAll(0).minWidth(0);
        button.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false)
                .fontSize(9).textShadow(false).textColor(0xff404040));
        return button;
    }

    private static GTScrollerViewElement list() {
        var list = new GTScrollerViewElement();
        list.getLayout().paddingAll(4);
        list.getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
        list.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(0));
        list.viewPort(port -> {
            port.getLayout().paddingAll(0);
            port.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        });
        list.verticalScroller(scroll -> {
            scroll.getLayout().width(2);
            scroll.getStyle().backgroundTexture(IGuiTexture.EMPTY);
            scroll.headButton.setDisplay(false);
            scroll.tailButton.setDisplay(false);
            var bar = new ColorRectTexture(0xffffffff).setRadius(1);
            scroll.scrollBar.buttonStyle(style -> style.baseTexture(bar).hoverTexture(bar).pressedTexture(bar));
        });
        return list;
    }

    /** Both levels share a screen-sized input surface; only their visible columns receive input. */
    @OnlyIn(Dist.CLIENT)
    @EventBusSubscriber(modid = GTCEu.MOD_ID, value = Dist.CLIENT)
    public static final class Panels {

        private static @Nullable Panels opened;
        private final BlockChoiceList owner;
        private final Screen screen = Minecraft.getInstance().screen;
        private final UIElement root = new UIElement();
        private final ModularUI ui = ModularUI.of(UI.of(root, size -> size));
        private final GTScrollerViewElement groupList = list();
        private final GTScrollerViewElement candidateList = list();
        private final Object2ObjectMap<String, GTButtonElement> groups = new Object2ObjectLinkedOpenHashMap<>();
        private final Object2ObjectMap<ResourceLocation, GTButtonElement> candidates = new Object2ObjectLinkedOpenHashMap<>();
        private @Nullable ResourceLocation current;
        private int x;
        private int y;
        private int height;
        private boolean dirty = true;
        private boolean captured;
        private boolean ownerRendered;

        private Panels(BlockChoiceList owner) {
            this.owner = owner;
            root.getLayout().positionType(TaffyPosition.ABSOLUTE);
            root.setAllowHitTest(false);
            root.addChildren(groupList, candidateList);
            ui.setScreenAndInit(screen);
        }

        private static Panels open(BlockChoiceList owner) {
            if (opened != null) opened.owner.close();
            return opened = new Panels(owner);
        }

        private static void close(BlockChoiceList owner) {
            if (opened != null && opened.owner == owner) {
                opened.ui.onRemoved();
                opened = null;
            }
        }

        private void updatePosition() {
            var anchor = owner.getWorldMouse(owner.getContentX() + owner.getContentWidth() + 4, owner.getContentY());
            var parent = owner.getParent();
            var bottom = parent.getWorldMouse(parent.getContentX(),
                    parent.getContentY() + parent.getContentHeight() - 4);
            int x = Math.round(anchor.x);
            int y = Math.round(anchor.y);
            int height = Math.max(26, Math.round(bottom.y) - y);
            if (this.x != x || this.y != y || this.height != height) {
                this.x = x;
                this.y = y;
                this.height = height;
                dirty = true;
            }
            if (ui.getScreenWidth() != screen.width || ui.getScreenHeight() != screen.height) {
                ui.init(screen.width, screen.height);
                dirty = true;
            }
        }

        private void rebuild() {
            dirty = false;
            groupList.clearAllScrollViewChildren();
            candidateList.clearAllScrollViewChildren();
            groups.clear();
            candidates.clear();
            var expanded = owner.expandedGroup;
            var values = expanded == null ? ObjectList.<ResourceLocation>of() : owner.choices.get(expanded);
            current = values.isEmpty() ? null : owner.selected.getOrDefault(expanded, values.getFirst());
            int columns = expanded == null ? 1 : 2;
            int left = Math.max(4, Math.min(x, screen.width - columns * (COLUMN_WIDTH + 4)));
            UITemplate.setBounds(groupList, left, y, COLUMN_WIDTH, height);
            UITemplate.setBounds(candidateList, left + COLUMN_WIDTH + 4, y, COLUMN_WIDTH, height);
            candidateList.setDisplay(expanded != null);
            int index = 0;
            for (var entry : owner.choices.object2ObjectEntrySet()) {
                String group = entry.getKey();
                int top = index * 18;
                var expand = button(top);
                expand.getStyle().tooltips(Component.translatable("gtpm.multiblock.category." + group));
                expand.setOnClick(event -> {
                    owner.expandedGroup = group.equals(owner.expandedGroup) ? null : group;
                    candidateList.verticalScroller.setValue(0f);
                    dirty = true;
                    event.stopPropagation();
                });
                groupList.addScrollViewChildren(icon(entry.getValue().getFirst(), top), expand);
                groups.put(group, expand);
                index++;
            }
            for (int i = 0; i < values.size(); i++) {
                ResourceLocation id = values.get(i);
                int top = i * 18;
                var choose = button(top);
                choose.getStyle().tooltips(BuiltInRegistries.BLOCK.get(id).getName());
                choose.setOnClick(event -> {
                    if (!id.equals(current)) owner.select.accept(expanded, id);
                    current = id;
                    refreshButtons();
                    event.stopPropagation();
                });
                candidateList.addScrollViewChildren(icon(id, top), choose);
                candidates.put(id, choose);
            }
            groupList.viewContainer.getLayout().width(38).height(owner.choices.size() * 18);
            candidateList.viewContainer.getLayout().width(38).height(values.size() * 18);
            refreshButtons();
        }

        private void refreshButtons() {
            for (var entry : groups.object2ObjectEntrySet()) {
                String group = entry.getKey();
                var button = entry.getValue();
                var first = owner.choices.get(group).getFirst();
                button.setText(Component.literal(group.equals(owner.expandedGroup) ? "◀" : "▶"));
                button.textStyle(style -> style.textColor(!first.equals(owner.selected.getOrDefault(group, first)) ?
                        0xff169fb5 : 0xff404040));
            }
            for (var entry : candidates.object2ObjectEntrySet()) {
                boolean selected = entry.getKey().equals(current);
                var button = entry.getValue();
                button.setText(Component.literal(selected ? "✔" : "✘"));
                button.textStyle(style -> style.textColor(selected ? 0xff169530 : 0xff404040));
                button.setActive(owner.interactive);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void beforeRender(ScreenEvent.Render.Pre event) {
            if (opened != null) opened.ownerRendered = false;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void render(ScreenEvent.Render.Post event) {
            if (opened == null) return;
            if (opened.screen != event.getScreen() || !opened.ownerRendered) {
                opened.owner.close();
                return;
            }
            if (opened.dirty) opened.rebuild();
            var graphics = event.getGuiGraphics();
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            opened.ui.getWidget().render(graphics, event.getMouseX(), event.getMouseY(), event.getPartialTick());
            graphics.pose().popPose();
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
            if (opened == null || opened.screen != event.getScreen()) return;
            var panel = opened;
            if (panel.ui.hitTestAtScreen((float) event.getMouseX(), (float) event.getMouseY()) != null) {
                panel.ui.getWidget().mouseMoved(event.getMouseX(), event.getMouseY());
                panel.ui.getWidget().mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton());
                panel.captured = true;
                event.setCanceled(true);
            } else if (panel.owner.expandedGroup != null) {
                panel.owner.expandedGroup = null;
                panel.dirty = true;
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
            if (opened == null || !opened.captured || opened.screen != event.getScreen()) return;
            opened.ui.getWidget().mouseReleased(event.getMouseX(), event.getMouseY(), event.getButton());
            opened.captured = false;
            event.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mouseDragged(ScreenEvent.MouseDragged.Pre event) {
            if (opened != null && opened.captured && opened.screen == event.getScreen()) {
                opened.ui.getWidget().mouseDragged(event.getMouseX(), event.getMouseY(), event.getMouseButton(),
                        event.getDragX(), event.getDragY());
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void mouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
            if (opened != null && opened.screen == event.getScreen() &&
                    opened.ui.hitTestAtScreen((float) event.getMouseX(), (float) event.getMouseY()) != null) {
                opened.ui.getWidget().mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaX(),
                        event.getScrollDeltaY());
                event.setCanceled(true);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void keyPressed(ScreenEvent.KeyPressed.Pre event) {
            if (opened != null && opened.screen == event.getScreen() &&
                    opened.owner.expandedGroup != null && event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
                opened.owner.expandedGroup = null;
                opened.dirty = true;
                event.setCanceled(true);
            }
        }
    }
}
