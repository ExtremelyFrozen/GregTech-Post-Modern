package com.gregtechceu.gtceu.common.item.terminal.ui;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.gui.texture.ItemStackTexture;
import com.gregtechceu.gtceu.api.gui.widget.MultiblockPreviewPanel;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview.Structure;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalStructure;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalChange;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/** Structure controls and direct category selection shared by the terminal and recipe preview. */
@NullMarked
public final class StructureControls extends UIElement {

    private final MultiblockMachineDefinition definition;
    private final MultiblockPreviewPanel preview;
    private final Consumer<TerminalChange> update;
    private final ObjectList<Control> controls = new ObjectArrayList<>();
    private final boolean showME;
    private final ObjectList<Selection> selections = new ObjectArrayList<>();
    private int contentWidth;
    private TerminalMachine machine;
    private ObjectList<Structure> displayedStructures = ObjectList.of();
    private int selectedStructure;
    private boolean interactive = true;
    private Predicate<String> worldPreviewVisible = name -> false;
    private @Nullable Consumer<String> toggleWorldPreview;
    private @Nullable GTButtonElement visibilityButton;
    private boolean worldVisible;

    public StructureControls(int x, int y, int width, int height, MultiblockMachineDefinition definition,
                             TerminalMachine machine, MultiblockPreviewPanel preview, boolean showME,
                             Consumer<TerminalChange> update) {
        UITemplate.setBounds(this, x, y, width, height);
        getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
        setOverflowVisible(false);
        this.definition = definition;
        this.machine = machine;
        this.preview = preview;
        this.showME = showME;
        this.update = update;
        contentWidth = width - 8;
        rebuild();
    }

    public void resize(int x, int y, int width, int height) {
        UITemplate.setBounds(this, x, y, width, height);
        contentWidth = width - 8;
        rebuild();
    }

    public void setMachine(TerminalMachine machine) {
        if (!this.machine.equals(machine) ||
                !displayedStructures.equals(preview.getState().getPreview().structures())) {
            this.machine = machine;
            rebuild();
        }
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        for (Control control : controls) control.button.setActive(interactive && control.enabled);
    }

    /** Installs the bound terminal's client overlay controls; unbound recipe previews have no world target. */
    public void setWorldPreview(Predicate<String> visible, Consumer<String> toggle) {
        worldPreviewVisible = visible;
        toggleWorldPreview = toggle;
        rebuild();
    }

    private void rebuild() {
        displayedStructures = preview.getState().getPreview().structures();
        clearAllChildren();
        controls.clear();
        selections.clear();
        visibilityButton = null;
        addChild(label(5, 4, contentWidth, text("title")));
        var shared = machine.sharedOptions();
        int toggleWidth = (contentWidth - (showME ? 4 : 2)) / (showME ? 3 : 2);
        toggle(4, 17, toggleWidth, text("replace"), shared.replaceMode(),
                TerminalChange.sharedReplace(!shared.replaceMode()));
        toggle(6 + toggleWidth, 17, toggleWidth, text("casings"), shared.noHatchMode(),
                TerminalChange.sharedNoHatch(!shared.noHatchMode()));
        if (showME) {
            toggle(8 + toggleWidth * 2, 17, toggleWidth, text("me"), shared.useME(),
                    TerminalChange.sharedME(!shared.useME()));
        }

        var order = definition.getStructureOrder();
        String name = order.get(selectedStructure);
        var previous = button(4, 37, 14, Component.literal("<"));
        previous.setActive(order.size() > 1);
        previous.setOnClick(event -> {
            selectedStructure = (selectedStructure + order.size() - 1) % order.size();
            rebuild();
            event.stopPropagation();
        });
        addChild(previous);
        addChild(label(20, 40, contentWidth - 32, structureName(name)));
        var next = button(contentWidth - 10, 37, 14, Component.literal(">"));
        next.setActive(order.size() > 1);
        next.setOnClick(event -> {
            selectedStructure = (selectedStructure + 1) % order.size();
            rebuild();
            event.stopPropagation();
        });
        addChild(next);

        TerminalStructure structure = machine.structures().get(name);
        boolean required = machine.structures().entrySet().stream()
                .anyMatch(entry -> entry.getValue().selected() && entry.getValue().mode() == StructureOperation.BUILD &&
                        definition.getRequiredStructures(entry.getKey()).contains(name));
        boolean locked = required || structure.selected() &&
                machine.structures().values().stream().filter(TerminalStructure::selected).count() == 1;
        int half = (contentWidth - 2) / 2;
        action(this, 4, 55, toggleWorldPreview == null ? contentWidth : half,
                text(locked ? "required" : structure.selected() ? "selected" : "unselected"),
                TerminalChange.structureSelected(name, !structure.selected()), !locked);
        Consumer<String> toggle = toggleWorldPreview;
        if (toggle != null) {
            worldVisible = worldPreviewVisible.test(name);
            visibilityButton = button(6 + half, 55, half, text(worldVisible ? "visible" : "hidden"));
            visibilityButton.getStyle().tooltips(text("world_preview"));
            controls.add(new Control(visibilityButton,
                    displayedStructures.stream().anyMatch(current -> current.name().equals(name))));
            visibilityButton.setOnClick(event -> {
                toggle.accept(name);
                event.stopPropagation();
            });
            addChild(visibilityButton);
        }
        action(this, 4, 77, half, text("mode." + structure.mode().name().toLowerCase(Locale.ROOT)),
                TerminalChange.structureMode(name, structure.mode() == StructureOperation.BUILD ?
                        StructureOperation.DEMOLISH : StructureOperation.BUILD),
                !required);
        action(this, 6 + half, 77, half, text("flip", onOff(structure.flipMode())),
                TerminalChange.structureFlip(name, !structure.flipMode()), definition.isAllowFlip());
        for (var repetition : definition.getPattern(name).getRepeatDescriptors()) {
            if (repetition.minimum() == repetition.maximum()) continue;
            int index = repetition.index();
            int value = structure.repetitions().getInt(index);
            selections.add(new Selection(text("axis." + repetition.axis().name().toLowerCase(Locale.ROOT)),
                    Component.literal(Integer.toString(value)),
                    () -> IntStream.rangeClosed(repetition.minimum(), repetition.maximum())
                            .mapToObj(amount -> new Value(Component.literal(Integer.toString(amount)), ItemStack.EMPTY,
                                    TerminalChange.repetition(name, index, amount), amount == value))
                            .collect(ObjectArrayList.toList())));
        }
        for (var choice : definition.getPattern(name).getChoiceDescriptors()) {
            String current = structure.choices().getOrDefault(choice.path(), choice.alternatives().getFirst());
            int index = choice.alternatives().indexOf(current);
            selections.add(new Selection(text("variant"),
                    text("variant_index", index + 1, choice.alternatives().size()),
                    () -> IntStream.range(0, choice.alternatives().size())
                            .mapToObj(alternative -> new Value(text("variant_index", alternative + 1,
                                    choice.alternatives().size()), ItemStack.EMPTY,
                                    TerminalChange.choice(name, choice.path(), choice.alternatives().get(alternative)),
                                    alternative == index))
                            .collect(ObjectArrayList.toList())));
        }
        var tiers = new BatchPlanner().activeTierChoiceOptions(definition, new StructureRequest(name,
                structure.repetitions(), structure.tierChoices(), structure.flipMode(), structure.mode(),
                structure.choices()));
        for (var entry : tiers.object2ObjectEntrySet()) {
            String group = entry.getKey();
            var choices = entry.getValue();
            ResourceLocation selected = structure.tierChoices().getOrDefault(group, choices.getFirst());
            selections.add(new Selection(Component.translatable(group.startsWith("ability/") ?
                    "gtpm.multiblock.ability." + group.substring("ability/".length()) :
                    "gtpm.multiblock.category." + group), BuiltInRegistries.BLOCK.get(selected).getName(),
                    () -> choices.stream().map(id -> new Value(BuiltInRegistries.BLOCK.get(id).getName(),
                            BuiltInRegistries.BLOCK.get(id).asItem().getDefaultInstance(),
                            TerminalChange.tier(name, group, id), id.equals(selected)))
                            .collect(ObjectArrayList.toList())));
        }
        var select = button(4, 99, contentWidth, text("select"));
        controls.add(new Control(select, !selections.isEmpty()));
        select.setOnClick(event -> {
            openSelection(0);
            event.stopPropagation();
        });
        addChild(select);
        setInteractive(interactive);
    }

    @Override
    public void screenTick() {
        super.screenTick();
        if (visibilityButton == null) return;
        boolean visible = worldPreviewVisible.test(definition.getStructureOrder().get(selectedStructure));
        if (worldVisible != visible) {
            worldVisible = visible;
            visibilityButton.setText(text(visible ? "visible" : "hidden"));
        }
    }

    private void openSelection(int selectedCategory) {
        var dialog = new Dialog().setClickOutsideClose(true);
        dialog.overlay.getLayout().width(280).height(178).paddingAll(4);
        dialog.overlay.getStyle().backgroundTexture(GuiTextures.BACKGROUND);
        dialog.titleBar.setDisplay(false);
        dialog.buttonContainer.setDisplay(false);
        UITemplate.setBounds(dialog.contentContainer, 4, 4, 272, 170);
        dialog.contentContainer.getLayout().paddingAll(0);
        dialog.contentContainer.getStyle().backgroundTexture(IGuiTexture.EMPTY);
        var categories = scroller(0, 18, 106, 152);
        var values = scroller(110, 18, 162, 152);
        dialog.contentContainer.addChildren(categories, values);
        dialog.contentContainer.addChild(label(2, 3, 236, text("select")));
        var close = button(254, 0, 18, Component.literal("×"));
        close.setOnClick(event -> {
            dialog.close();
            event.stopPropagation();
        });
        dialog.contentContainer.addChild(close);
        ObjectList<GTButtonElement> categoryButtons = new ObjectArrayList<>();
        IntConsumer selectCategory = index -> {
            for (int i = 0; i < categoryButtons.size(); i++) {
                int color = i == index ? 0xff167129 : 0xff404040;
                categoryButtons.get(i).textStyle(style -> style.textColor(color));
            }
            values.clearAllScrollViewChildren();
            var candidates = selections.get(index).values().get();
            for (int i = 0; i < candidates.size(); i++) {
                Value candidate = candidates.get(i);
                var choice = button(0, i * 20, 156, candidate.name());
                choice.getLayout().height(18);
                choice.textStyle(style -> style.textColor(candidate.selected() ? 0xff167129 : 0xff404040));
                if (!candidate.icon().isEmpty()) {
                    choice.getLayout().paddingLeft(20);
                    var icon = new UIElement();
                    UITemplate.setBounds(icon, 1, 1, 16, 16);
                    icon.getStyle().backgroundTexture(new ItemStackTexture(candidate.icon()));
                    icon.setAllowHitTest(false);
                    choice.addChild(icon);
                }
                choice.setOnClick(event -> {
                    dialog.close();
                    if (!candidate.selected()) update.accept(candidate.change());
                    event.stopPropagation();
                });
                values.addScrollViewChild(choice);
            }
            values.viewContainer.getLayout().width(156).height(candidates.size() * 20);
        };
        for (int i = 0; i < selections.size(); i++) {
            int index = i;
            var category = button(0, i * 20, 100, selections.get(i).name());
            category.getStyle().tooltips(selections.get(i).name(), selections.get(i).selected());
            category.setOnClick(event -> {
                selectCategory.accept(index);
                event.stopPropagation();
            });
            categoryButtons.add(category);
            categories.addScrollViewChild(category);
        }
        categories.viewContainer.getLayout().width(100).height(selections.size() * 20);
        selectCategory.accept(selectedCategory);
        dialog.show(getModularUI().ui.rootElement);
    }

    private static GTScrollerViewElement scroller(int x, int y, int width, int height) {
        var scroller = new GTScrollerViewElement(x, y, width, height);
        scroller.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER)
                .scrollerViewStyle(0));
        scroller.viewPort(port -> {
            port.getLayout().paddingAll(0);
            port.getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
        });
        scroller.verticalScroller(scroll -> {
            scroll.getLayout().width(3);
            scroll.getStyle().backgroundTexture(GuiTextures.SLIDER_BACKGROUND_VERTICAL);
            scroll.headButton.setDisplay(false);
            scroll.tailButton.setDisplay(false);
            scroll.scrollBar.buttonStyle(style -> style.baseTexture(GuiTextures.BUTTON)
                    .hoverTexture(GuiTextures.BUTTON).pressedTexture(GuiTextures.BUTTON));
        });
        return scroller;
    }

    private void toggle(int x, int y, int width, Component label, boolean enabled, TerminalChange change) {
        var button = action(this, x, y, width, label, change, true);
        button.textStyle(style -> style.textColor(enabled ? 0xff167129 : 0xff404040));
        button.getStyle().tooltips(text("toggle", label, onOff(enabled)));
    }

    private GTButtonElement action(UIElement parent, int x, int y, int width, Component label,
                                   TerminalChange change, boolean enabled) {
        var button = button(x, y, width, label);
        controls.add(new Control(button, enabled));
        button.setOnClick(event -> {
            update.accept(change);
            event.stopPropagation();
        });
        parent.addChild(button);
        return button;
    }

    private static GTButtonElement button(int x, int y, int width, Component label) {
        var button = new GTButtonElement(x, y, width, 16);
        button.setButtonTexture(GuiTextures.BUTTON);
        button.getLayout().paddingAll(1);
        button.text.getLayout().marginAll(0).widthPercent(100).minWidth(0);
        button.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false)
                .fontSize(8).textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        button.setOverflowVisible(false);
        button.setText(label);
        button.getStyle().tooltips(label);
        return button;
    }

    private static GTLabelElement label(int x, int y, int width, Component text) {
        var label = new GTLabelElement(x, y, width, 10, text);
        label.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(9)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        label.setOverflowVisible(false);
        label.getStyle().tooltips(text);
        return label;
    }

    public static Component structureName(String name) {
        return name.equals(MultiblockControllerMachine.DEFAULT_STRUCTURE) ? text("main") : Component.translatable(name);
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("gtpm.multiblock.configuration." + key, arguments);
    }

    private static Component onOff(boolean enabled) {
        return text(enabled ? "on" : "off");
    }

    private record Control(GTButtonElement button, boolean enabled) {}

    private record Selection(Component name, Component selected, Supplier<ObjectList<Value>> values) {}

    private record Value(Component name, ItemStack icon, TerminalChange change, boolean selected) {}
}
