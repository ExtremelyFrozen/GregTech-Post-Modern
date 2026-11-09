package com.gregtechceu.gtceu.common.item.behavior;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.factory.HeldItemUIHelper;
import com.gregtechceu.gtceu.api.gui.factory.HeldItemUIHolder;
import com.gregtechceu.gtceu.api.gui.widget.BlockChoiceList;
import com.gregtechceu.gtceu.api.item.component.IItemUIFactory;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSources;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchStatus;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BuildPolicy;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis;
import com.gregtechceu.gtceu.api.multiblock.preview.PreviewVisibility;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.sync_system.annotations.ClientFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.managed.ItemSyncHolder;
import com.gregtechceu.gtceu.api.sync_system.managed.SyncItem;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;
import com.gregtechceu.gtceu.client.gui.ClientMultiblockPreviewElement;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalControllerBinding;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.math.Size;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.function.UnaryOperator;

@NullMarked
public class TerminalBehavior extends ItemSyncHolder implements IItemUIFactory, SyncItem {

    @Getter
    @Setter
    @SaveField
    private @Nullable TerminalControllerBinding binding;
    @Getter
    @SaveField
    @SyncToClient
    private final Object2ObjectMap<ResourceLocation, BuildPolicy> policies = new Object2ObjectLinkedOpenHashMap<>();
    @Getter
    @SaveField
    @SyncToClient
    private final Object2ObjectMap<ResourceLocation, Object2ObjectMap<String, StructureRequest>> structures = new Object2ObjectLinkedOpenHashMap<>();
    @Getter
    @SaveField
    @SyncToClient
    private final Object2ObjectMap<ResourceLocation, ObjectSet<String>> included = new Object2ObjectLinkedOpenHashMap<>();
    @Getter
    @SyncToClient
    private @Nullable StructurePreview preview;
    @Getter
    @SyncToClient
    private @Nullable BatchStatus status;
    @Getter
    @SyncToClient
    private int placed;
    @Getter
    @SyncToClient
    private int removed;
    private boolean refreshUI;
    private @Nullable MultiblockControllerMachine openedController;
    private ObjectList<MaterialRequest.Result> boundSources = ObjectList.of();
    private long patternGeneration;

    /** The registered component creates independent annotated state for each opened stack. */
    public TerminalBehavior() {
        this(ItemStack.EMPTY, RegistryAccess.EMPTY);
    }

    public TerminalBehavior(ItemStack stack, RegistryAccess registries) {
        super(stack, registries);
    }

    @ClientFieldChangeListener(fieldName = "policies")
    public void onPoliciesChanged() {
        refreshUI = true;
    }

    @ClientFieldChangeListener(fieldName = "structures")
    public void onStructuresChanged() {
        refreshUI = true;
    }

    @ClientFieldChangeListener(fieldName = "included")
    public void onIncludedChanged() {
        refreshUI = true;
    }

    @ClientFieldChangeListener(fieldName = "preview")
    public void onPreviewChanged() {
        refreshUI = true;
    }

    @ClientFieldChangeListener(fieldName = "status")
    public void onStatusChanged() {
        refreshUI = true;
    }

    @ClientFieldChangeListener(fieldName = "placed")
    public void onPlacedChanged() {
        refreshUI = true;
    }

    @ClientFieldChangeListener(fieldName = "removed")
    public void onRemovedChanged() {
        refreshUI = true;
    }

    @RPCMethod
    public void setPolicy(RPCSender sender, BuildPolicy policy) {
        policies.put(binding.definitionId(), policy);
        refresh(sender.asPlayer());
    }

    @RPCMethod
    public void setStructure(RPCSender sender, StructureRequest request) {
        var definition = (MultiblockMachineDefinition) openedController.getDefinition();
        String name = request.structureName();
        var current = structures.get(binding.definitionId()).get(name);
        if (request.mode() == StructureOperation.DEMOLISH && isRequired(name)) return;
        structures.get(binding.definitionId()).put(name, normalize(definition, request));
        if (current.mode() != request.mode() && request.mode() == StructureOperation.BUILD &&
                included.get(binding.definitionId()).contains(name)) {
            includeDependencies(definition, name);
        }
        refresh(sender.asPlayer());
    }

    @RPCMethod
    public void setIncluded(RPCSender sender, String name, boolean value) {
        var selected = included.get(binding.definitionId());
        if (value) {
            selected.add(name);
            includeDependencies((MultiblockMachineDefinition) openedController.getDefinition(), name);
        } else {
            if (isRequired(name) || selected.size() == 1) return;
            selected.remove(name);
        }
        refresh(sender.asPlayer());
    }

    @RPCMethod
    public void execute(RPCSender sender, long fingerprint) {
        var player = sender.asPlayer();
        if (fingerprint != Long.parseUnsignedLong(preview.fingerprint(), 16)) return;
        try {
            var result = openedController.autoBuildBatch(player, createRequest(boundSources), fingerprint);
            status = result.status();
            placed = result.placed();
            removed = result.removed();
            player.displayClientMessage(Component.translatable("gtpm.multiblock.autobuild.execution_result",
                    Component
                            .translatable("gtpm.multiblock.autobuild.status." + status.name().toLowerCase(Locale.ROOT)),
                    placed, removed), true);
            refresh(player);
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Terminal execution failed for {}", binding.definitionId(), exception);
            player.displayClientMessage(Component.translatable("gtpm.multiblock.autobuild.execution_failed"), true);
            player.closeContainer();
        }
    }

    private void initialize(MultiblockMachineDefinition definition) {
        ResourceLocation id = definition.getId();
        policies.putIfAbsent(id, BuildPolicy.DEFAULT);
        var saved = structures.computeIfAbsent(id, ignored -> new Object2ObjectLinkedOpenHashMap<>());
        saved.keySet().retainAll(definition.getStructureOrder());
        var planner = new BatchPlanner();
        for (String name : definition.getStructureOrder()) {
            var request = saved.computeIfAbsent(name, ignored -> planner.defaultStructureOptions(definition, name));
            try {
                saved.put(name, normalize(definition, request));
            } catch (IllegalArgumentException | IllegalStateException exception) {
                saved.put(name, planner.defaultStructureOptions(definition, name));
            }
        }
        var selected = included.computeIfAbsent(id, ignored -> new ObjectLinkedOpenHashSet<>());
        selected.retainAll(definition.getStructureOrder());
        if (selected.isEmpty()) selected.add(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        for (String name : new ObjectArrayList<>(selected)) includeDependencies(definition, name);
        patternGeneration = StructurePatternRegistry.generation();
    }

    private StructureRequest normalize(MultiblockMachineDefinition definition, StructureRequest request) {
        var domains = new BatchPlanner().activeTierChoiceOptions(definition, request);
        var tiers = new Object2ObjectLinkedOpenHashMap<String, ResourceLocation>();
        for (var entry : domains.object2ObjectEntrySet()) {
            ResourceLocation current = request.tierChoices().get(entry.getKey());
            tiers.put(entry.getKey(), entry.getValue().contains(current) ? current : entry.getValue().getFirst());
        }
        return new StructureRequest(request.structureName(), request.repetitions(), tiers,
                request.flipMode(), request.mode(), request.choices());
    }

    private void includeDependencies(MultiblockMachineDefinition definition, String name) {
        var saved = structures.get(definition.getId());
        if (saved.get(name).mode() != StructureOperation.BUILD) return;
        for (String dependency : definition.getRequiredStructures(name)) {
            included.get(definition.getId()).add(dependency);
            saved.put(dependency, saved.get(dependency).withMode(StructureOperation.BUILD));
        }
    }

    private boolean isRequired(String name) {
        var definition = (MultiblockMachineDefinition) openedController.getDefinition();
        return included.get(binding.definitionId()).stream().anyMatch(
                selected -> structures.get(binding.definitionId()).get(selected).mode() == StructureOperation.BUILD &&
                        definition.getRequiredStructures(selected).contains(name));
    }

    private BatchRequest createRequest(ObjectList<MaterialRequest.Result> sources) {
        var selected = included.get(binding.definitionId());
        var definition = (MultiblockMachineDefinition) openedController.getDefinition();
        return new BatchRequest(definition.getStructureOrder().stream().filter(selected::contains)
                .map(structures.get(binding.definitionId())::get).collect(ObjectArrayList.toList()),
                policies.get(binding.definitionId()), sources);
    }

    public void refresh(ServerPlayer player) {
        var definition = (MultiblockMachineDefinition) openedController.getDefinition();
        if (patternGeneration != StructurePatternRegistry.generation()) initialize(definition);
        var policy = policies.get(binding.definitionId());
        boolean build = included.get(binding.definitionId()).stream()
                .anyMatch(name -> structures.get(binding.definitionId()).get(name).mode() == StructureOperation.BUILD);
        boundSources = build ? MaterialSources.request(player, new MaterialRequest(ObjectList.of(), getStack(),
                kind -> policy.useME() || kind != MaterialSource.SourceKind.ME, null)) : ObjectList.of();
        preview = StructurePreview.bound(definition.getId(),
                new BatchPlanner().resolveBatch(openedController, player, createRequest(boundSources)));
    }

    @Override
    public TerminalBehavior createSyncHolder(ItemStack stack, RegistryAccess registries) {
        return new TerminalBehavior(stack, registries);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack itemStack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        if (MetaMachine.getMachine(level, blockPos) instanceof MultiblockControllerMachine controller) {
            if (player instanceof ServerPlayer serverPlayer &&
                    !bind(controller, serverPlayer, context.getHand(), itemStack)) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(ItemStack item, Level level, Player player,
                                                  InteractionHand usedHand) {
        if (player instanceof ServerPlayer serverPlayer &&
                !openBound(serverPlayer, usedHand, item)) {
            return InteractionResultHolder.fail(item);
        }
        return InteractionResultHolder.sidedSuccess(item, level.isClientSide);
    }

    @Override
    public boolean canCreateUI(HeldItemUIHolder holder, Player player) {
        TerminalControllerBinding binding = ((TerminalBehavior) holder.getSyncHolder()).getBinding();
        return binding != null &&
                GTRegistries.MACHINES.get(binding.definitionId()) instanceof MultiblockMachineDefinition &&
                isUIStillValid(holder, player);
    }

    @Override
    public boolean isUIStillValid(HeldItemUIHolder holder, Player player) {
        TerminalBehavior data = (TerminalBehavior) holder.getSyncHolder();
        TerminalControllerBinding binding = data.getBinding();
        if (binding == null || !ItemStack.isSameItem(holder.getHeld(), holder.getOpenedStack()) ||
                !binding.dimension().equals(player.level().dimension())) {
            return false;
        }
        if (player.level().isClientSide) return true;
        MultiblockControllerMachine controller = boundController(player, binding);
        return controller != null && (data.openedController == null || data.openedController == controller) &&
                holder.getHeld() == data.getStack() &&
                canAccess(controller, player, holder.getHand(), holder.getHeld());
    }

    @Override
    public UI createUI(HeldItemUIHolder holder, Player player) {
        TerminalBehavior terminal = (TerminalBehavior) holder.getSyncHolder();
        TerminalControllerBinding binding = terminal.getBinding();
        var definition = (MultiblockMachineDefinition) GTRegistries.MACHINES.get(binding.definitionId());
        terminal.initialize(definition);
        if (player instanceof ServerPlayer serverPlayer) {
            var controller = (MultiblockControllerMachine) MetaMachine.getMachine(player.level(), binding.position());
            terminal.openedController = controller;
            terminal.refresh(serverPlayer);
            var root = new UIElement() {

                private int refreshTicks;

                @Override
                public void serverTick() {
                    super.serverTick();
                    if (++refreshTicks < 20) return;
                    refreshTicks = 0;
                    try {
                        terminal.refresh(serverPlayer);
                    } catch (RuntimeException exception) {
                        GTCEu.LOGGER.error("Closed terminal after refresh failed for {}", definition.getId(),
                                exception);
                        player.closeContainer();
                    }
                }

                @Override
                protected void onRemoved() {
                    terminal.openedController = null;
                    super.onRemoved();
                }
            };
            UITemplate.setBounds(root, 0, 0, 300, 200);
            return UI.of(root);
        }

        class Page extends UIElement {

            private final ClientMultiblockPreviewElement preview;
            private final BlockChoiceList blocks;
            private final GTScrollerViewElement options = new GTScrollerViewElement();
            private final ObjectList<GTButtonElement> controls = new ObjectArrayList<>();
            private final GTButtonElement execute;
            private final GTButtonElement fullscreen;
            private final GTButtonElement edit;
            private final GTLabelElement title;
            private final GTLabelElement result = label(192, 167, 100, Component.empty());
            private @Nullable GTButtonElement visibility;
            private @Nullable StructurePreview displayedPreview;
            private boolean expanded;
            private boolean editing;
            private boolean worldVisible;
            private int selectedStructure;
            private int width = 300;
            private int height = 200;

            Page() {
                var empty = new StructurePreview(definition.getId(), "awaiting-server", false,
                        ObjectList.of(), ObjectList.of(), ObjectList.of(), ObjectList.of());

                UITemplate.setBounds(this, 0, 0, width, height);
                getStyle().backgroundTexture(GuiTextures.BACKGROUND);
                setOverflowVisible(false);
                title = label(6, 6, 244, definition.getBlock().getName());
                title.getStyle().tooltips(Component.literal(binding.dimension().location() + " @ " +
                        binding.position().toShortString()));
                preview = new ClientMultiblockPreviewElement(180, 176, new PreviewVisibility(empty), true);
                blocks = new BlockChoiceList((group, id) -> change(value -> value.withTierChoice(group, id)));
                blocks.setDisplay(false);
                options.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                        .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .scrollerViewStyle(0));
                options.viewPort(port -> {
                    port.getLayout().paddingAll(0);
                    port.getStyle().backgroundTexture(GuiTextures.BACKGROUND_INVERSE);
                });
                options.verticalScroller(scroll -> scroll.getLayout().width(2));
                execute = button(188, 180, 108, Component.translatable("gtpm.multiblock.autobuild.terminal.execute"));
                execute.setOnClick(event -> {
                    terminal.rpcToServer("execute", Long.parseUnsignedLong(terminal.preview.fingerprint(), 16));
                    event.stopPropagation();
                });
                edit = button(260, 4, 16, Component.literal("✎"));
                edit.getStyle().tooltips(text("edit"));
                edit.setOnClick(event -> {
                    editing = !editing;
                    edit.setText(Component.literal(editing ? "✘" : "✎"));
                    blocks.setDisplay(editing);
                    if (!editing) blocks.close();
                    var ui = getModularUI();
                    ui.init(ui.getScreenWidth(), ui.getScreenHeight());
                    ((ModularUIContainerScreen) ui.getScreen()).init();
                    event.stopPropagation();
                });
                fullscreen = button(278, 4, 16, Component.literal("↗"));
                fullscreen.getStyle().tooltips(text("fullscreen"));
                fullscreen.setOnClick(event -> {
                    expanded = !expanded;
                    fullscreen.setText(Component.literal(expanded ? "↙" : "↗"));
                    fullscreen.getStyle().tooltips(text(expanded ? "compact" : "fullscreen"));
                    var ui = getModularUI();
                    ui.init(ui.getScreenWidth(), ui.getScreenHeight());
                    ((ModularUIContainerScreen) ui.getScreen()).init();
                    event.stopPropagation();
                });
                addChildren(title, preview, options, blocks, result, execute, edit, fullscreen);
                rebuildOptions();
                updateDisplay();
            }

            private Size resize(Size screen) {
                width = expanded ? screen.getWidth() :
                        Math.min(300 + (editing ? BlockChoiceList.WIDTH + 4 : 0), screen.getWidth());
                height = expanded ? screen.getHeight() : Math.min(200, screen.getHeight());
                int contentWidth = width - (editing ? BlockChoiceList.WIDTH + 4 : 0);
                title.getLayout().width(contentWidth - 48);
                edit.getLayout().left(contentWidth - 40);
                fullscreen.getLayout().left(contentWidth - 22);
                result.getLayout().left(contentWidth - 108).top(height - 33).width(100);
                execute.getLayout().left(contentWidth - 112).top(height - 20);
                rebuildOptions();
                return Size.of(width, height);
            }

            private String structureName() {
                return definition.getStructureOrder().get(selectedStructure);
            }

            private void rebuildOptions() {
                options.clearAllScrollViewChildren();
                controls.clear();
                visibility = null;
                int contentWidth = 104;
                var requests = terminal.structures.get(definition.getId());
                var selected = terminal.included.get(definition.getId());
                var shared = terminal.policies.get(definition.getId());
                String name = structureName();
                var structure = requests.get(name);
                var replace = button(0, 0, 50, text("replace"));
                replace.textStyle(style -> style.textColor(shared.replaceMode() ? 0xff167129 : 0xff404040));
                replace.setOnClick(event -> {
                    terminal.rpcToServer("setPolicy",
                            new BuildPolicy(!shared.replaceMode(), shared.noHatchMode(), shared.useME()));
                    event.stopPropagation();
                });
                var casings = button(52, 0, 52, text("casings"));
                casings.textStyle(style -> style.textColor(shared.noHatchMode() ? 0xff167129 : 0xff404040));
                casings.setOnClick(event -> {
                    terminal.rpcToServer("setPolicy",
                            new BuildPolicy(shared.replaceMode(), !shared.noHatchMode(), shared.useME()));
                    event.stopPropagation();
                });
                options.addScrollViewChildren(replace, casings);
                controls.add(replace);
                controls.add(casings);
                int y = 20;
                if (GTCEu.MODS.AE2.isLoaded()) {
                    var me = button(0, y, contentWidth, text("me"));
                    me.textStyle(style -> style.textColor(shared.useME() ? 0xff167129 : 0xff404040));
                    me.setOnClick(event -> {
                        terminal.rpcToServer("setPolicy",
                                new BuildPolicy(shared.replaceMode(), shared.noHatchMode(), !shared.useME()));
                        event.stopPropagation();
                    });
                    options.addScrollViewChild(me);
                    controls.add(me);
                    y += 20;
                }
                var order = definition.getStructureOrder();
                if (order.size() > 1) {
                    addStep(y, contentWidth, displayName(name), () -> {
                        selectedStructure = (selectedStructure + order.size() - 1) % order.size();
                        blocks.close();
                        rebuildOptions();
                    }, () -> {
                        selectedStructure = (selectedStructure + 1) % order.size();
                        blocks.close();
                        rebuildOptions();
                    }, true, true);
                    y += 20;
                }
                boolean required = selected.stream()
                        .anyMatch(entry -> requests.get(entry).mode() == StructureOperation.BUILD &&
                                definition.getRequiredStructures(entry).contains(name));
                boolean locked = required || selected.contains(name) && selected.size() == 1;
                if (!locked) {
                    var included = button(0, y, definition.isRenderWorldPreview() ? 50 : contentWidth,
                            text(selected.contains(name) ? "selected" : "unselected"));
                    included.setOnClick(event -> {
                        terminal.rpcToServer("setIncluded", name, !selected.contains(name));
                        event.stopPropagation();
                    });
                    options.addScrollViewChild(included);
                    controls.add(included);
                }
                if (definition.isRenderWorldPreview()) {
                    worldVisible = MultiblockInWorldPreviewRenderer.isPreviewVisible(binding.position(), name);
                    visibility = button(locked ? 0 : 54, y, locked ? contentWidth : 50,
                            text(worldVisible ? "visible" : "hidden"));
                    visibility.getStyle().tooltips(text("world_preview"));
                    visibility.setOnClick(event -> {
                        MultiblockInWorldPreviewRenderer.togglePreview(binding.position(), terminal.preview,
                                structureName(),
                                ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
                        event.stopPropagation();
                    });
                    options.addScrollViewChild(visibility);
                }
                if (!locked || definition.isRenderWorldPreview()) y += 20;
                var mode = button(0, y, definition.isAllowFlip() ? 50 : contentWidth,
                        text("mode." + structure.mode().name().toLowerCase(Locale.ROOT)));
                mode.setActive(!required);
                mode.setOnClick(event -> {
                    change(value -> value.withMode(value.mode() == StructureOperation.BUILD ?
                            StructureOperation.DEMOLISH : StructureOperation.BUILD));
                    event.stopPropagation();
                });
                options.addScrollViewChild(mode);
                if (!required) controls.add(mode);
                if (definition.isAllowFlip()) {
                    var flip = button(54, y, 50,
                            text("flip", text(structure.flipMode() ? "on" : "off")));
                    flip.setOnClick(event -> {
                        change(value -> value.withFlipMode(!value.flipMode()));
                        event.stopPropagation();
                    });
                    options.addScrollViewChild(flip);
                    controls.add(flip);
                }
                y += 20;
                var pattern = definition.getPattern(name);
                var axes = pattern.getSourceDefinition().getAxes();
                var layout = pattern.expandLayout(structure.repetitions().toIntArray(), structure.choices());
                for (var repetition : pattern.getRepeatDescriptors()) {
                    if (repetition.minimum() == repetition.maximum()) continue;
                    int index = repetition.index();
                    int amount = structure.repetitions().getInt(index);
                    var axis = PatternAxis.of(switch (repetition.axis()) {
                        case DEPTH -> axes.getDepth();
                        case HEIGHT -> axes.getHeight();
                        case WIDTH -> axes.getWidth();
                    });
                    addStep(y, contentWidth, text("dimension", text("axis." + axis.name().toLowerCase(Locale.ROOT)),
                            layout.size(repetition.axis())),
                            () -> changeRepetition(index, amount - 1),
                            () -> changeRepetition(index, amount + 1),
                            amount > repetition.minimum(), amount < repetition.maximum());
                    y += 20;
                }
                for (var choice : pattern.getChoiceDescriptors()) {
                    var values = choice.alternatives();
                    int index = values.indexOf(structure.choices().getOrDefault(choice.path(), values.getFirst()));
                    addStep(y, contentWidth, text("variant_index", index + 1, values.size()),
                            () -> change(value -> value.withChoice(choice.path(), values.get(index - 1))),
                            () -> change(value -> value.withChoice(choice.path(), values.get(index + 1))),
                            index > 0, index + 1 < values.size());
                    y += 20;
                }
                options.viewContainer.getLayout().width(contentWidth - 3).height(y);
                int bodyWidth = width - (editing ? BlockChoiceList.WIDTH + 4 : 0);
                UITemplate.setBounds(options, bodyWidth - 112, 20, 108, height - 56);
                preview.getLayout().left(4).top(20);
                preview.resize(bodyWidth - 120, height - 24);
                blocks.resize(bodyWidth, 20, height - 24);
                blocks.setChoices(new BatchPlanner().activeTierChoiceOptions(definition, structure),
                        structure.tierChoices());
                updateInteractivity();
            }

            private void addStep(int y, int width, Component name, Runnable previous, Runnable next,
                                 boolean hasPrevious, boolean hasNext) {
                options.addScrollViewChild(label(18, y + 3, width - 36, name));
                var back = button(0, y, 16, Component.literal("◀"));
                back.setOnClick(event -> {
                    previous.run();
                    event.stopPropagation();
                });
                back.setActive(hasPrevious);
                var forward = button(width - 19, y, 16, Component.literal("▶"));
                forward.setOnClick(event -> {
                    next.run();
                    event.stopPropagation();
                });
                forward.setActive(hasNext);
                options.addScrollViewChildren(back, forward);
                if (hasPrevious) controls.add(back);
                if (hasNext) controls.add(forward);
            }

            private void change(UnaryOperator<StructureRequest> update) {
                terminal.rpcToServer("setStructure",
                        update.apply(terminal.structures.get(definition.getId()).get(structureName())));
            }

            private void changeRepetition(int index, int amount) {
                change(value -> {
                    var repetitions = new IntArrayList(value.repetitions());
                    repetitions.set(index, amount);
                    return value.withRepetitions(repetitions);
                });
            }

            private void updateDisplay() {
                if (terminal.preview != null && terminal.preview != displayedPreview) {
                    displayedPreview = terminal.preview;
                    preview.setPreview(displayedPreview);
                    MultiblockInWorldPreviewRenderer.updatePreview(binding.position(), displayedPreview,
                            ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
                }
                result.setValue(terminal.status == null ? Component.empty() :
                        Component.translatable("gtpm.multiblock.autobuild.status." +
                                terminal.status.name().toLowerCase(Locale.ROOT)));
                result.getStyle().tooltips(terminal.status == null ? Component.empty() :
                        Component.translatable("gtpm.multiblock.autobuild.terminal.result",
                                Component.translatable("gtpm.multiblock.autobuild.status." +
                                        terminal.status.name().toLowerCase(Locale.ROOT)),
                                terminal.placed, terminal.removed));
                updateInteractivity();
            }

            private void updateInteractivity() {
                boolean ready = terminal.preview != null;
                controls.forEach(button -> button.setActive(ready));
                blocks.setInteractive(ready);
                execute.setActive(ready);
                if (visibility != null) visibility.setActive(ready);
            }

            @Override
            public void screenTick() {
                super.screenTick();
                if (terminal.refreshUI) {
                    terminal.refreshUI = false;
                    rebuildOptions();
                    updateDisplay();
                }
                if (visibility != null) {
                    boolean visible = MultiblockInWorldPreviewRenderer.isPreviewVisible(binding.position(),
                            structureName());
                    if (worldVisible != visible) {
                        worldVisible = visible;
                        visibility.setText(text(visible ? "visible" : "hidden"));
                    }
                }
            }
        }

        var root = new Page();
        return UI.of(root, root::resize);
    }

    private static GTButtonElement button(int x, int y, int width, Component text) {
        var button = new GTButtonElement(x, y, width, 16);
        button.setButtonTexture(GuiTextures.VANILLA_BUTTON).setText(text);
        button.getLayout().paddingAll(0);
        button.text.getLayout().widthPercent(100).heightPercent(100).marginAll(0).minWidth(0);
        button.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(8)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        button.getStyle().tooltips(text);
        return button;
    }

    private static GTLabelElement label(int x, int y, int width, Component text) {
        var label = new GTLabelElement(x, y, width, 10, text);
        label.textStyle(style -> style.adaptiveWidth(false).adaptiveHeight(false).fontSize(9)
                .textColor(0xff404040).textShadow(false).textWrap(TextWrap.HIDE));
        label.getStyle().tooltips(text);
        return label;
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("gtpm.multiblock.configuration." + key, arguments);
    }

    private static Component displayName(String name) {
        return name.equals(MultiblockControllerMachine.DEFAULT_STRUCTURE) ? text("main") : Component.translatable(name);
    }

    private static boolean bind(MultiblockControllerMachine controller, ServerPlayer player, InteractionHand hand,
                                ItemStack terminal) {
        if (!canAccess(controller, player, hand, terminal)) return reject(player, "access_denied");
        TerminalBehavior data = (TerminalBehavior) ItemSyncHolder.create(terminal, player.registryAccess());
        data.setBinding(new TerminalControllerBinding(controller.getBlockPos(),
                player.level().dimension(), controller.getDefinition().getId()));
        data.saveToStack();
        player.displayClientMessage(Component.translatable("gtpm.multiblock.autobuild.terminal.bound",
                controller.getDefinition().getBlock().getName(), controller.getBlockPos().toShortString()), true);
        return true;
    }

    private static boolean openBound(ServerPlayer player, InteractionHand hand, ItemStack terminal) {
        TerminalControllerBinding binding = ((TerminalBehavior) ItemSyncHolder.create(terminal,
                player.registryAccess()))
                .getBinding();
        if (binding == null) return reject(player, "unbound");
        if (!binding.dimension().equals(player.level().dimension())) return reject(player, "wrong_dimension");
        if (!player.level().isLoaded(binding.position())) return reject(player, "unloaded");
        MultiblockControllerMachine controller = boundController(player, binding);
        if (controller == null) return reject(player, "controller_missing");
        if (player.distanceToSqr(Vec3.atCenterOf(binding.position())) > 64) return reject(player, "out_of_range");
        if (!canAccess(controller, player, hand, terminal)) return reject(player, "access_denied");
        try {
            return HeldItemUIHelper.open(player, hand) || reject(player, "open_failed");
        } catch (RuntimeException exception) {
            GTCEu.LOGGER.error("Failed to open terminal bound to {} at {} for {}", binding.definitionId(),
                    binding.position(), player.getGameProfile().getName(), exception);
            return reject(player, "open_failed");
        }
    }

    private static @Nullable MultiblockControllerMachine boundController(Player player,
                                                                         TerminalControllerBinding binding) {
        if (!player.level().isLoaded(binding.position())) return null;
        return MetaMachine.getMachine(player.level(),
                binding.position()) instanceof MultiblockControllerMachine controller &&
                binding.definitionId().equals(controller.getDefinition().getId()) ? controller : null;
    }

    private static boolean canAccess(MultiblockControllerMachine controller, Player player,
                                     InteractionHand hand, ItemStack terminal) {
        return !player.isSpectator() && player.getItemInHand(hand) == terminal &&
                controller.getLevel() == player.level() &&
                player.distanceToSqr(Vec3.atCenterOf(controller.getBlockPos())) <= 64 &&
                player.level().mayInteract(player, controller.getBlockPos()) &&
                MachineOwner.canOpenOwnerMachine(player, controller) &&
                MachineOwner.canBreakOwnerMachine(player, controller);
    }

    private static boolean reject(ServerPlayer player, String reason) {
        player.displayClientMessage(Component.translatable("gtpm.multiblock.autobuild.terminal." + reason), true);
        return false;
    }
}
