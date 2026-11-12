package com.gregtechceu.gtceu.api.gui.multiblock;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisBasis;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisIssue;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisMirror;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisReport;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisSelection;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.DiagnosisStatus;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.StructureDiagnosis;
import com.gregtechceu.gtceu.api.multiblock.diagnosis.network.DiagnosisReportCodec;
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternLayout;
import com.gregtechceu.gtceu.client.renderer.StructureIssueOverlay;

import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEmitter;
import com.lowdragmc.lowdraglib2.gui.sync.rpc.RPCEventBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;

/** Menu-owned diagnosis with typed requests, bounded report transport and client-only issue overlays. */
public final class StructureDiagnosisPanel extends UIElement {

    private static final int MAX_OPTIONS = 4096;
    private final MultiblockControllerMachine controller;
    private final Player player;
    private final BooleanSupplier validTarget;
    private final Function<String, DiagnosisSelection> selection;
    private final boolean manualControls;
    private final GTScrollerViewElement rows = new GTScrollerViewElement(4, 54, 292, 140);
    private final GTLabelElement status = new GTLabelElement(4, 30, 292, 20, "", false);
    private final GTButtonElement structure = new GTButtonElement(4, 4, 94, 20);
    private final RPCEmitter startEvent;
    private final RPCEmitter cancelEvent;
    private final RPCEmitter reportEvent;
    private final RPCEmitter optionsEvent;
    private final Object2ObjectMap<String, String> manualChoices = new Object2ObjectLinkedOpenHashMap<>();
    private final Object2IntMap<String> manualMeasurements = new Object2IntLinkedOpenHashMap<>();
    private int @Nullable [] manualRepetitions;
    private DiagnosisMirror manualMirror = DiagnosisMirror.AUTOMATIC;
    private @Nullable DiagnosisSelection serverOptions;
    private @Nullable String serverOptionsName;
    private @Nullable StructureDiagnosis diagnosis;
    private @Nullable DiagnosisReport lastProgress;
    private long request;
    private long cancelledRequest = -1;
    private int structureIndex;
    private boolean requestRunning;
    private boolean editing;
    private boolean removed;

    public StructureDiagnosisPanel(MultiblockControllerMachine controller, Player player,
                                   BooleanSupplier validTarget, Function<String, DiagnosisSelection> selection) {
        this(controller, player, validTarget, selection, false);
    }

    public StructureDiagnosisPanel(MultiblockControllerMachine controller, Player player,
                                   BooleanSupplier validTarget, Function<String, DiagnosisSelection> selection,
                                   boolean manualControls) {
        this.controller = controller;
        this.player = player;
        this.validTarget = validTarget;
        this.selection = selection;
        this.manualControls = manualControls;
        // Both logical sides register these events unconditionally and in exactly this order.
        startEvent = addRPCEvent(RPCEventBuilder.simple(String.class, Long.class, this::startRequest));
        cancelEvent = addRPCEvent(RPCEventBuilder.simple(Long.class, this::cancelRequest));
        reportEvent = addRPCEvent(RPCEventBuilder.simple(byte[].class, Long.class, this::receiveReport));
        optionsEvent = addRPCEvent(RPCEventBuilder.simple(String.class, int[].class, Integer.class,
                String[].class, String[].class, String[].class, int[].class, this::receiveOptions));
        UITemplate.setBounds(this, 0, 0, 300, 200);
        style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));
        structure.setText(currentName()).setOnClick(event -> {
            if (!client()) return;
            cancelDiagnosis();
            structureIndex = (structureIndex + 1) % controller.getDefinition().getStructureOrder().size();
            structure.setText(currentName());
            resetOptions();
            rows.clearAllScrollViewChildren();
            if (editing) showOptions();
        });
        GTButtonElement start = new GTButtonElement(102, 4, 62, 20);
        start.setText(Component.translatable("gtpm.multiblock.diagnosis.start")).setOnClick(event -> {
            if (!client()) return;
            editing = false;
            if (manualControls) sendOptions();
            requestRunning = true;
            startEvent.send(currentName(), ++request);
        });
        GTButtonElement cancel = new GTButtonElement(168, 4, 62, 20);
        cancel.setText(Component.translatable("gtpm.multiblock.diagnosis.cancel"))
                .setOnClick(event -> cancelDiagnosis());
        GTButtonElement options = new GTButtonElement(234, 4, 62, 20);
        options.setText(Component.translatable("gtpm.multiblock.diagnosis.options"))
                .setOnClick(event -> {
                    if (!client()) return;
                    cancelDiagnosis();
                    editing = true;
                    showOptions();
                });
        options.setVisible(manualControls);
        options.setActive(manualControls);
        addChildren(structure, start, cancel, options, status, rows);
    }

    private boolean client() {
        return player.level().isClientSide();
    }

    private boolean validMenu() {
        var ui = getModularUI();
        return !removed && !player.isRemoved() && !controller.isRemoved() && ui != null && ui.player == player &&
                ui.getMenu() != null && player.containerMenu == ui.getMenu() && ui.getMenu().stillValid(player) &&
                player.level() == controller.getLevel() && validTarget.getAsBoolean();
    }

    private void startRequest(String name, long identifier) {
        if (!(player instanceof ServerPlayer) || identifier <= request || identifier <= 0 || !validMenu() ||
                !controller.getDefinition().getStructureNames().contains(name))
            return;
        closeRequest();
        request = identifier;
        DiagnosisSelection options = serverOptions == null || !name.equals(serverOptionsName) ? selection.apply(name) :
                serverOptions;
        serverOptions = null;
        serverOptionsName = null;
        diagnosis = new StructureDiagnosis(controller, name, options.getRepetitions(), options.getChoices(),
                options.getMirror(), options.getMeasurements());
        lastProgress = null;
    }

    private void cancelRequest(long identifier) {
        if (!(player instanceof ServerPlayer) || identifier != request || !validMenu()) return;
        closeRequest();
        sendReport(new DiagnosisReport(DiagnosisStatus.CANCELLED, currentName(), 0, 0, false, false, null,
                ObjectLists.emptyList()));
    }

    /** Cancels the request from a hidden page, the cancel button or its owning terminal. */
    public void cancelDiagnosis() {
        if (client()) {
            if (requestRunning) cancelEvent.send(request);
            cancelledRequest = request;
            requestRunning = false;
            StructureIssueOverlay.clear();
        } else closeRequest();
    }

    private void receiveReport(byte[] encoded, long identifier) {
        if (!client() || removed || identifier != request) return;
        try {
            DiagnosisReport report = DiagnosisReportCodec.decode(encoded);
            if (identifier == cancelledRequest && report.getStatus() != DiagnosisStatus.CANCELLED) return;
            if (!requestRunning && report.getStatus() == DiagnosisStatus.RUNNING) return;
            requestRunning = report.getStatus() == DiagnosisStatus.RUNNING;
            if (!editing) showReport(report);
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Invalid structure diagnosis report for {} at {}", player.getUUID(),
                    controller.getBlockPos(), exception);
            requestRunning = false;
        }
    }

    private void sendOptions() {
        String[] keys = manualChoices.keySet().toArray(String[]::new);
        String[] values = Arrays.stream(keys).map(manualChoices::get).toArray(String[]::new);
        String[] measurements = manualMeasurements.keySet().toArray(String[]::new);
        int[] distances = new int[measurements.length];
        for (int index = 0; index < distances.length; index++)
            distances[index] = manualMeasurements.getInt(measurements[index]);
        optionsEvent.send(currentName(), manualRepetitions == null ? new int[0] : manualRepetitions.clone(),
                manualMirror.ordinal(), keys, values, measurements, distances);
    }

    private void receiveOptions(String name, int[] counts, int mirror, String[] keys, String[] values,
                                String[] measurements, int[] distances) {
        serverOptions = null;
        serverOptionsName = null;
        if (!manualControls || !(player instanceof ServerPlayer) || !validMenu() ||
                !controller.getDefinition().getStructureNames().contains(name) || counts.length > MAX_OPTIONS ||
                keys.length > MAX_OPTIONS || measurements.length > MAX_OPTIONS || keys.length != values.length ||
                measurements.length != distances.length || mirror < 0 || mirror >= DiagnosisMirror.getEntries().size())
            return;
        var pattern = controller.getPattern(name);
        var repeats = pattern.getRepeatDescriptors();
        if (counts.length != 0 && counts.length != repeats.size()) return;
        for (int index = 0; index < counts.length; index++) {
            var descriptor = repeats.get(index);
            if (counts[index] < descriptor.minimum() || counts[index] > descriptor.maximum()) return;
        }
        var choices = new Object2ObjectLinkedOpenHashMap<String, String>();
        for (int index = 0; index < keys.length; index++) {
            if (keys[index].length() > MAX_OPTIONS || values[index].length() > MAX_OPTIONS) return;
            choices.put(keys[index], values[index]);
        }
        var measured = new Object2IntLinkedOpenHashMap<String>();
        for (int index = 0; index < measurements.length; index++) {
            String parameter = measurements[index];
            var descriptor = pattern.getMeasurements().stream()
                    .filter(entry -> entry.getParameter().equals(parameter)).findFirst().orElse(null);
            if (descriptor == null || distances[index] < descriptor.getMinimum() ||
                    distances[index] > descriptor.getMaximum())
                return;
            measured.put(parameter, distances[index]);
        }
        // Layout expansion validates branch/module keys and values before the next start request consumes them.
        int[] resolved = counts.length == 0 ? defaults(name) : counts;
        Object2IntMap<String> previewMeasurements = defaultMeasurements(name, measured);
        try {
            PatternLayout layout = pattern.expandLayout(resolved, choices, Object2IntMaps.emptyMap(),
                    previewMeasurements);
            for (var entry : choices.entrySet()) {
                boolean branch = layout.getChoices().stream()
                        .anyMatch(choice -> (choice.path().equals(entry.getKey()) ||
                                choice.descriptor().id().equals(entry.getKey())) &&
                                choice.descriptor().alternatives().contains(entry.getValue()));
                boolean module = layout.getModuleChoices().stream()
                        .anyMatch(choice -> choice.getPath().equals(entry.getKey()) &&
                                choice.getDescriptor().getCandidates().contains(entry.getValue()));
                if (!branch && !module) return;
            }
        } catch (IllegalArgumentException exception) {
            return;
        }
        serverOptions = new DiagnosisSelection(counts.length == 0 ? null : counts.clone(), choices,
                DiagnosisMirror.getEntries().get(mirror), measured);
        serverOptionsName = name;
    }

    private String currentName() {
        return controller.getDefinition().getStructureOrder().get(structureIndex);
    }

    @Override
    public void serverTick() {
        super.serverTick();
        if (diagnosis == null) return;
        if (!validMenu()) {
            closeRequest();
            return;
        }
        diagnosis.tick();
        DiagnosisReport progress = diagnosis.progress();
        if (!progress.equals(lastProgress)) {
            lastProgress = progress;
            sendReport(progress);
        }
        if (progress.getStatus() != DiagnosisStatus.RUNNING) closeRequest();
    }

    private void sendReport(DiagnosisReport report) {
        reportEvent.send(DiagnosisReportCodec.encode(report), request);
    }

    private void showReport(DiagnosisReport report) {
        StructureIssueOverlay.clear();
        rows.clearAllScrollViewChildren();
        status.setText(Component
                .translatable("gtpm.multiblock.diagnosis." + report.getStatus().name().toLowerCase(Locale.ROOT))
                .append(" (" + report.getVisited() + ")"));
        int y = 0;
        DiagnosisBasis basis = report.getBasis();
        if (basis != null) {
            y = label(y, Component.translatable("gtpm.multiblock.diagnosis.basis", basis.getFront(), basis.getFlipped(),
                    Arrays.toString(basis.getRepetitions())));
            for (var entry : basis.getMeasurements().object2IntEntrySet())
                y = label(y, Component.literal(entry.getKey() + " = " + entry.getIntValue()));
            for (var entry : basis.getChoices().entrySet())
                y = label(y, Component.literal(entry.getKey() + " → " + entry.getValue()));
            for (var module : basis.getModules()) y = label(y,
                    Component.literal(module.getId() + " [" + module.getKind() + "] = " + module.getSelection()));
        }
        if (report.getIncomplete()) y = label(y, Component.translatable("gtpm.multiblock.diagnosis.incomplete"));
        if (report.getTruncated()) y = label(y, Component.translatable("gtpm.multiblock.diagnosis.truncated"));
        LongArrayList all = new LongArrayList();
        for (DiagnosisIssue issue : report.getIssues()) {
            long[] positions = issue.getPositions();
            all.addElements(all.size(), positions);
            GTButtonElement problem = new GTButtonElement(2, y, 280, 20);
            problem.setText(
                    Component.literal(issue.getCount() + " × ").append(Component.translatable(issue.getExpected())))
                    .setOnClick(event -> StructureIssueOverlay.toggleGroup(controller.getBlockPos(), positions));
            Component coordinates = Component.literal(
                    Arrays.stream(positions).limit(8).mapToObj(position -> BlockPos.of(position).toShortString())
                            .reduce((left, right) -> left + "; " + right).orElse(""));
            problem.getStyle().tooltips(Component.literal(issue.getPath()),
                    Component.translatable("gtpm.multiblock.diagnosis.actual",
                            Component.translatable(issue.getActual())),
                    Component.translatable("gtpm.multiblock.diagnosis.difference", issue.getFact(),
                            issue.getDifference()),
                    coordinates);
            rows.addScrollViewChild(problem);
            y += 22;
        }
        if (!all.isEmpty()) {
            GTButtonElement showAll = new GTButtonElement(2, y, 280, 20);
            showAll.setText(Component.translatable("gtpm.multiblock.diagnosis.highlight_all"))
                    .setOnClick(
                            event -> StructureIssueOverlay.toggleGroup(controller.getBlockPos(), all.toLongArray()));
            rows.addScrollViewChild(showAll);
        }
    }

    private int label(int y, Component text) {
        rows.addScrollViewChild(new GTLabelElement(2, y, 280, 14, text));
        return y + 16;
    }

    private int[] defaults(String name) {
        return controller.getPattern(name).getRepeatDescriptors().stream().mapToInt(entry -> entry.minimum()).toArray();
    }

    private Object2IntMap<String> defaultMeasurements(String name, Object2IntMap<String> selected) {
        Object2IntMap<String> result = new Object2IntLinkedOpenHashMap<>();
        controller.getPattern(name).getMeasurements()
                .forEach(entry -> result.put(entry.getParameter(),
                        selected.getOrDefault(entry.getParameter(), entry.getMinimum())));
        return result;
    }

    private void resetOptions() {
        manualRepetitions = null;
        manualMirror = DiagnosisMirror.AUTOMATIC;
        manualChoices.clear();
        manualMeasurements.clear();
    }

    private void showOptions() {
        rows.clearAllScrollViewChildren();
        int y = 0;
        GTButtonElement reset = new GTButtonElement(2, y, 280, 20);
        reset.setText(Component.translatable("gtpm.multiblock.diagnosis.automatic"))
                .setOnClick(event -> {
                    resetOptions();
                    showOptions();
                });
        rows.addScrollViewChild(reset);
        y += 22;
        GTButtonElement mirror = new GTButtonElement(2, y, 280, 20);
        mirror.setText(Component.translatable("gtpm.multiblock.diagnosis.flip", manualMirror.getLabel()))
                .setOnClick(event -> {
                    manualMirror = manualMirror.next();
                    showOptions();
                });
        rows.addScrollViewChild(mirror);
        y += 22;
        String name = currentName();
        var pattern = controller.getPattern(name);
        int[] defaults = defaults(name);
        int[] counts = manualRepetitions == null ? defaults : manualRepetitions;
        var descriptors = pattern.getRepeatDescriptors();
        for (int index = 0; index < descriptors.size(); index++) {
            final int selected = index;
            var descriptor = descriptors.get(index);
            y = stepper(y, descriptor.id(), counts[index], descriptor.minimum(), descriptor.maximum(), value -> {
                if (manualRepetitions == null) manualRepetitions = defaults.clone();
                manualRepetitions[selected] = value;
                showOptions();
            });
        }
        for (var measurement : pattern.getMeasurements()) {
            String parameter = measurement.getParameter();
            y = stepper(y, parameter, manualMeasurements.getOrDefault(parameter, measurement.getMinimum()),
                    measurement.getMinimum(), measurement.getMaximum(), value -> {
                        manualMeasurements.put(parameter, value);
                        showOptions();
                    });
        }
        Object2IntMap<String> measured = defaultMeasurements(name, manualMeasurements);
        PatternLayout layout = pattern.expandLayout(counts, manualChoices, Object2IntMaps.emptyMap(), measured);
        for (var choice : layout.getChoices()) y = choice(y, choice.path(), choice.descriptor().alternatives());
        for (var module : layout.getModuleChoices())
            y = choice(y, module.getPath(), module.getDescriptor().getCandidates());
    }

    private int stepper(int y, String name, int value, int minimum, int maximum, IntConsumer setter) {
        rows.addScrollViewChild(new GTLabelElement(2, y, 190, 18, Component.literal(name + ": " + value)));
        GTButtonElement minus = new GTButtonElement(196, y, 38, 18);
        minus.setText("-").setOnClick(event -> setter.accept(value - 1));
        minus.setActive(value > minimum);
        GTButtonElement plus = new GTButtonElement(238, y, 38, 18);
        plus.setText("+").setOnClick(event -> setter.accept(value + 1));
        plus.setActive(value < maximum);
        rows.addScrollViewChild(minus);
        rows.addScrollViewChild(plus);
        return y + 20;
    }

    private int choice(int y, String path, ObjectList<String> alternatives) {
        String selected = manualChoices.get(path);
        int index = selected == null ? -1 : alternatives.indexOf(selected);
        GTButtonElement button = new GTButtonElement(2, y, 276, 20);
        button.setText(Component.literal(path + ": ")
                .append(selected == null ? Component.translatable("gtpm.multiblock.diagnosis.automatic") :
                        Component.literal(selected)))
                .setOnClick(event -> {
                    manualChoices.keySet().removeIf(key -> key.startsWith(path + "/"));
                    if (index + 1 >= alternatives.size()) manualChoices.remove(path);
                    else manualChoices.put(path, alternatives.get(index + 1));
                    showOptions();
                });
        rows.addScrollViewChild(button);
        return y + 22;
    }

    private void closeRequest() {
        if (diagnosis != null) diagnosis.close();
        diagnosis = null;
    }

    @Override
    public UIElement setVisible(boolean visible) {
        if (!visible && cancelEvent != null) cancelDiagnosis();
        return super.setVisible(visible);
    }

    @Override
    public UIElement setActive(boolean active) {
        if (!active && cancelEvent != null) cancelDiagnosis();
        return super.setActive(active);
    }

    @Override
    protected void onRemoved() {
        cancelDiagnosis();
        removed = true;
        closeRequest();
        super.onRemoved();
    }
}
