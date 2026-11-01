package com.gregtechceu.gtceu.common.item.terminal.ui;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.widget.MultiblockPreviewPanel;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview.Structure;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalStructure;
import com.gregtechceu.gtceu.common.item.terminal.profile.TerminalChange;

import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Shared configuration controls for local XEI previews and server-owned terminal sessions. A callback decides
 * where changes are applied; visibility always belongs to the preview window.
 */
@NullMarked
public final class MultiblockConfigurationPanel extends GTScrollerViewElement {

    private final MultiblockMachineDefinition definition;
    private final MultiblockPreviewPanel preview;
    private final Consumer<TerminalChange> update;
    private final ObjectList<Control> controls = new ObjectArrayList<>();
    private final int contentWidth;
    private final int viewportHeight;
    private TerminalMachine profile;
    private ObjectList<Structure> displayedStructures = ObjectList.of();
    private boolean interactive = true;

    public MultiblockConfigurationPanel(int x, int y, int width, int height,
                                        MultiblockMachineDefinition definition, TerminalMachine profile,
                                        MultiblockPreviewPanel preview, Consumer<TerminalChange> update) {
        super(x, y, width, height);
        this.definition = definition;
        this.profile = profile;
        this.preview = preview;
        this.update = update;
        contentWidth = width - 4;
        viewportHeight = height;
        scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL).verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER).scrollerViewStyle(0));
        viewPort(port -> port.layout(layout -> layout.paddingAll(0)));
        verticalScroller(scroller -> scroller.layout(layout -> layout.width(3)));
        rebuild();
    }

    public void updateProfile(TerminalMachine profile) {
        if (!this.profile.equals(profile) || !displayedStructures.equals(preview.state().snapshot().structures())) {
            this.profile = profile;
            rebuild();
        }
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
        for (Control control : controls) control.button().setActive(interactive && control.enabled());
    }

    private void rebuild() {
        displayedStructures = new ObjectArrayList<>(preview.state().snapshot().structures());
        clearAllScrollViewChildren();
        controls.clear();
        var shared = profile.sharedOptions();
        int y = 0;
        action(0, y, contentWidth, text("replace", onOff(shared.replaceMode())),
                TerminalChange.sharedReplace(!shared.replaceMode()), true);
        y += 18;
        action(0, y, contentWidth, text("no_hatch", onOff(shared.noHatchMode())),
                TerminalChange.sharedNoHatch(!shared.noHatchMode()), true);
        y += 18;
        action(0, y, contentWidth, text("ae", onOff(shared.useME())),
                TerminalChange.sharedME(!shared.useME()), true);
        y += 22;

        long selectedCount = profile.structures().values().stream().filter(TerminalStructure::selected).count();
        BatchPlanner resolver = new BatchPlanner();
        for (String name : definition.getStructureOrder()) {
            TerminalStructure structure = profile.structures().get(name);
            boolean required = profile.structures().entrySet().stream()
                    .anyMatch(entry -> entry.getValue().selected() && entry.getValue().mode() == StructureOperation.BUILD &&
                            definition.getRequiredStructures(entry.getKey()).contains(name));
            boolean selectionLocked = required || structure.selected() && selectedCount == 1;
            GTLabelElement title = new GTLabelElement(0, y, contentWidth, 11, Component.literal(name));
            title.style(style -> style.tooltips(Component.literal(name)));
            addScrollViewChild(title);
            y += 13;
            action(0, y, contentWidth - 44, text(selectionLocked ? "structure_locked" :
                    structure.selected() ? "structure_selected" : "structure_unselected", ""),
                    TerminalChange.structureSelected(name, !structure.selected()), !selectionLocked);
            GTButtonElement visibility = button(contentWidth - 42, y, 42,
                    Component.translatable("gtpm.multiblock.configuration.visible." +
                            preview.state().isStructureVisible(name)));
            visibility.setActive(preview.state().snapshot().structures().stream()
                    .anyMatch(selected -> selected.name().equals(name)));
            visibility
                    .style(style -> style.tooltips(Component.translatable("gtpm.multiblock.configuration.visibility")));
            visibility.setOnClick(event -> {
                boolean visible = !preview.state().isStructureVisible(name);
                preview.state().setStructureVisible(name, visible);
                visibility.setText(Component.translatable("gtpm.multiblock.configuration.visible." + visible));
                event.stopPropagation();
            });
            addScrollViewChild(visibility);
            y += 18;
            action(0, y, contentWidth / 2 - 1, text("mode." + structure.mode().name().toLowerCase(Locale.ROOT)),
                    TerminalChange.structureMode(name,
                            structure.mode() == StructureOperation.BUILD ? StructureOperation.DEMOLISH : StructureOperation.BUILD),
                    !required);
            action(contentWidth / 2 + 1, y, contentWidth / 2 - 1, text("flip", onOff(structure.flipMode())),
                    TerminalChange.structureFlip(name, !structure.flipMode()), definition.isAllowFlip());
            y += 20;

            for (var repetition : definition.getPattern(name).getRepeatDescriptors()) {
                if (repetition.minimum() == repetition.maximum()) continue;
                int index = repetition.index();
                int value = structure.repetitions().getInt(index);
                GTLabelElement label = new GTLabelElement(0, y, contentWidth, 10,
                        Component.translatable("gtpm.multiblock.configuration.repeat_axis",
                                Component.translatable("gtpm.multiblock.configuration.axis." +
                                        repetition.axis().name().toLowerCase(Locale.ROOT)),
                                repetition.id()));
                label.style(style -> style.tooltips(Component.literal(repetition.path())));
                addScrollViewChild(label);
                y += 12;
                action(0, y, 20, Component.literal("-"),
                        TerminalChange.repetition(name, index, value - 1), value > repetition.minimum());
                addScrollViewChild(new GTLabelElement(22, y + 3, contentWidth - 44, 10,
                        Component.literal(value + " / " + repetition.maximum())));
                action(contentWidth - 20, y, 20, Component.literal("+"),
                        TerminalChange.repetition(name, index, value + 1), value < repetition.maximum());
                y += 19;
            }

            for (var choice : definition.getPattern(name).getChoiceDescriptors()) {
                String current = structure.choices().getOrDefault(choice.path(), choice.alternatives().getFirst());
                int index = choice.alternatives().indexOf(current);
                String next = choice.alternatives().get((index + 1) % choice.alternatives().size());
                action(0, y, contentWidth,
                        Component.translatable("gtpm.multiblock.configuration.choice", choice.id(), current),
                        TerminalChange.choice(name, choice.path(), next), choice.alternatives().size() > 1);
                y += 19;
            }

            Object2ObjectMap<String, ObjectList<ResourceLocation>> tierOptions = new Object2ObjectLinkedOpenHashMap<>();
            resolver.activeTierChoiceOptions(definition, new StructureRequest(name, structure.repetitions(),
                    structure.tierChoices(), structure.flipMode(), structure.mode(), structure.choices()))
                    .forEach((group, candidates) -> tierOptions.put(group, new ObjectArrayList<>(candidates)));
            for (var entry : tierOptions.object2ObjectEntrySet()) {
                String group = entry.getKey();
                ObjectList<ResourceLocation> choices = entry.getValue();
                ResourceLocation selected = structure.tierChoices().get(group);
                int selectedIndex = choices.indexOf(selected);
                if (selectedIndex < 0) {
                    throw new IllegalStateException("Unnormalized tier selection for " + name + ": " + group);
                }
                GTLabelElement label = new GTLabelElement(0, y, contentWidth, 10,
                        Component.literal(group));
                label.style(style -> style.tooltips(Component.literal(group)));
                addScrollViewChild(label);
                y += 12;
                ResourceLocation next = choices.get((selectedIndex + 1) % choices.size());
                action(0, y, contentWidth, BuiltInRegistries.BLOCK.get(selected).getName(),
                        TerminalChange.tier(name, group, next), choices.size() > 1)
                        .style(style -> style.tooltips(Component.translatable(
                                "gtpm.multiblock.configuration.tier", group,
                                BuiltInRegistries.BLOCK.get(selected).getName(), selected.toString())));
                y += 19;
            }
            y += 6;
        }
        int contentHeight = Math.max(viewportHeight, y);
        viewContainer.layout(layout -> layout.width(contentWidth).height(contentHeight));
        setInteractive(interactive);
    }

    private GTButtonElement action(int x, int y, int width, Component label, TerminalChange change,
                                   boolean enabled) {
        GTButtonElement button = button(x, y, width, label);
        controls.add(new Control(button, enabled));
        button.setOnClick(event -> {
            if (interactive && enabled) update.accept(change);
            event.stopPropagation();
        });
        addScrollViewChild(button);
        return button;
    }

    private static GTButtonElement button(int x, int y, int width, Component label) {
        GTButtonElement button = new GTButtonElement(x, y, width, 16);
        button.setButtonTexture(GuiTextures.BUTTON);
        button.setText(label);
        button.textStyle(style -> style.textShadow(false));
        return button;
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("gtpm.multiblock.autobuild.terminal." + key, arguments);
    }

    private static Component onOff(boolean enabled) {
        return text(enabled ? "on" : "off");
    }

    private record Control(GTButtonElement button, boolean enabled) {}
}
