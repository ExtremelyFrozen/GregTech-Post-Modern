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
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.pattern.layout.PatternAxis;
import com.gregtechceu.gtceu.api.multiblock.preview.PreviewVisibility;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.sync_system.annotations.ClientFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.ItemSave;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.managed.ItemSyncHolder;
import com.gregtechceu.gtceu.api.sync_system.managed.SyncItem;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;
import com.gregtechceu.gtceu.client.gui.ClientMultiblockPreviewElement;
import com.gregtechceu.gtceu.client.renderer.MultiblockInWorldPreviewRenderer;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalControllerBinding;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalStructure;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalMenuState;
import com.gregtechceu.gtceu.common.item.terminal.menu.TerminalSession;
import com.gregtechceu.gtceu.common.item.terminal.network.TerminalChange;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;
import com.gregtechceu.gtceu.config.ConfigHolder;

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

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@NullMarked
public class TerminalBehavior extends ItemSyncHolder implements IItemUIFactory, SyncItem {

    @Getter
    @Setter
    @ItemSave
    private @Nullable TerminalControllerBinding binding;
    @Getter
    @ItemSave
    private final Object2ObjectMap<ResourceLocation, TerminalMachine> machines = new Object2ObjectLinkedOpenHashMap<>();
    @Getter
    @Setter
    @SyncToClient
    private @Nullable TerminalMenuState state;
    @Getter
    @Setter
    private @Nullable TerminalSession session;
    @Setter
    private @Nullable Runnable stateListener;

    /** The registered component only creates stack-owned instances; it never holds an opened terminal's state. */
    public TerminalBehavior() {
        this(ItemStack.EMPTY, RegistryAccess.EMPTY);
    }

    public TerminalBehavior(ItemStack stack, RegistryAccess registries) {
        super(stack, registries);
    }

    @ClientFieldChangeListener(fieldName = "state")
    public void onStateChanged() {
        if (stateListener != null) stateListener.run();
    }

    @RPCMethod
    public void change(RPCSender sender, long sequence, long revision, TerminalChange update) {
        if (sender.isRemote() && session != null) {
            session.applyUpdate(sender.asPlayer(), sequence, revision, update);
        }
    }

    @RPCMethod
    public void execute(RPCSender sender, long sequence, long revision, long fingerprint) {
        if (sender.isRemote() && session != null) {
            session.execute(sender.asPlayer(), sequence, revision, fingerprint);
        }
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
        if (data.getSession() != null) return data.getSession().isStillValid(player);
        MultiblockControllerMachine controller = boundController(player, binding);
        return controller != null && canAccess(controller, player, holder.getHand(), holder.getHeld());
    }

    @Override
    public UI createUI(HeldItemUIHolder holder, Player player) {
        TerminalBehavior terminal = (TerminalBehavior) holder.getSyncHolder();
        TerminalControllerBinding binding = terminal.getBinding();
        var definition = (MultiblockMachineDefinition) GTRegistries.MACHINES.get(binding.definitionId());
        if (player instanceof ServerPlayer serverPlayer) {
            var controller = (MultiblockControllerMachine) MetaMachine.getMachine(player.level(), binding.position());
            var session = new TerminalSession(serverPlayer, holder.getHand(), terminal, controller);
            terminal.setSession(session);
            var root = new UIElement() {

                private int refreshTicks;

                @Override
                public void serverTick() {
                    super.serverTick();
                    if (++refreshTicks < 20) return;
                    refreshTicks = 0;
                    try {
                        session.refresh(serverPlayer);
                    } catch (RuntimeException exception) {
                        GTCEu.LOGGER.error("Closed terminal after refresh failed for {}", definition.getId(),
                                exception);
                        player.closeContainer();
                    }
                }

                @Override
                protected void onRemoved() {
                    session.close();
                    terminal.setSession(null);
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
            private final GTLabelElement result = label(6, 183, 182, Component.empty());
            private @Nullable GTButtonElement visibility;
            private TerminalMenuState state;
            private boolean expanded;
            private boolean editing;
            private boolean worldVisible;
            private int selectedStructure;
            private int width = 300;
            private int height = 200;
            private int optionsHeight;
            private long pendingSequence = -1;

            Page() {
                var empty = new StructurePreview(definition.getId(), "awaiting-server", false,
                        ObjectList.of(), ObjectList.of(), ObjectList.of(), ObjectList.of());
                state = terminal.getState() == null ? new TerminalMenuState(definition.getId(), 0, 0,
                        TerminalMachine.defaults(definition), empty, false, null) : terminal.getState();
                UITemplate.setBounds(this, 0, 0, width, height);
                getStyle().backgroundTexture(GuiTextures.BACKGROUND);
                setOverflowVisible(false);
                title = label(6, 6, 244, definition.getBlock().getName());
                title.getStyle().tooltips(Component.literal(binding.dimension().location() + " @ " +
                        binding.position().toShortString()));
                preview = new ClientMultiblockPreviewElement(292, 136, new PreviewVisibility(state.preview()), true);
                blocks = new BlockChoiceList((group, id) -> request(TerminalChange.tier(structureName(), group, id)));
                blocks.setDisplay(false);
                options.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                        .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .scrollerViewStyle(0));
                options.viewPort(port -> port.getLayout().paddingAll(0));
                options.verticalScroller(scroll -> scroll.getLayout().width(2));
                execute = button(192, 180, 104, Component.translatable("gtpm.multiblock.autobuild.terminal.execute"));
                execute.setOnClick(event -> {
                    request(null);
                    event.stopPropagation();
                });
                edit = button(260, 4, 16, Component.literal("✎"));
                edit.getStyle().tooltips(text("edit"));
                edit.setOnClick(event -> {
                    editing = !editing;
                    edit.setText(Component.literal(editing ? "✘" : "✎"));
                    blocks.setDisplay(editing);
                    if (!editing) blocks.close();
                    rebuildOptions();
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
                terminal.setStateListener(this::receiveState);
                updateDisplay();
            }

            private Size resize(Size screen) {
                width = expanded ? screen.getWidth() : Math.min(300, screen.getWidth());
                height = expanded ? screen.getHeight() : Math.min(200, screen.getHeight());
                title.getLayout().width(width - 48);
                edit.getLayout().left(width - 40);
                fullscreen.getLayout().left(width - 22);
                result.getLayout().top(height - 17).width(width - 116);
                execute.getLayout().left(width - 108).top(height - 20);
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
                int contentWidth = width - (editing ? BlockChoiceList.WIDTH + 12 : 8);
                var machine = state.machine();
                var shared = machine.sharedOptions();
                String name = structureName();
                var structure = machine.structures().get(name);
                var replace = button(0, 0, 58, text("replace"));
                replace.textStyle(style -> style.textColor(shared.replaceMode() ? 0xff167129 : 0xff404040));
                replace.setOnClick(event -> {
                    request(TerminalChange.sharedReplace(!shared.replaceMode()));
                    event.stopPropagation();
                });
                var casings = button(60, 0, 66, text("casings"));
                casings.textStyle(style -> style.textColor(shared.noHatchMode() ? 0xff167129 : 0xff404040));
                casings.setOnClick(event -> {
                    request(TerminalChange.sharedNoHatch(!shared.noHatchMode()));
                    event.stopPropagation();
                });
                options.addScrollViewChildren(replace, casings);
                controls.add(replace);
                controls.add(casings);
                if (GTCEu.MODS.AE2.isLoaded()) {
                    var me = button(128, 0, 30, text("me"));
                    me.textStyle(style -> style.textColor(shared.useME() ? 0xff167129 : 0xff404040));
                    me.setOnClick(event -> {
                        request(TerminalChange.sharedME(!shared.useME()));
                        event.stopPropagation();
                    });
                    options.addScrollViewChild(me);
                    controls.add(me);
                }
                int y = 20;
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
                boolean required = machine.structures().entrySet().stream()
                        .anyMatch(entry -> entry.getValue().selected() &&
                                entry.getValue().mode() == StructureOperation.BUILD &&
                                definition.getRequiredStructures(entry.getKey()).contains(name));
                boolean locked = required || structure.selected() &&
                        machine.structures().values().stream().filter(TerminalStructure::selected).count() == 1;
                int x = 0;
                if (!locked) {
                    var included = button(x, y, 48, text(structure.selected() ? "selected" : "unselected"));
                    included.setOnClick(event -> {
                        request(TerminalChange.structureSelected(name, !structure.selected()));
                        event.stopPropagation();
                    });
                    options.addScrollViewChild(included);
                    controls.add(included);
                    x += 50;
                }
                if (definition.isRenderWorldPreview()) {
                    worldVisible = MultiblockInWorldPreviewRenderer.isPreviewVisible(binding.position(), name);
                    visibility = button(x, y, 42, text(worldVisible ? "visible" : "hidden"));
                    visibility.getStyle().tooltips(text("world_preview"));
                    visibility.setOnClick(event -> {
                        MultiblockInWorldPreviewRenderer.togglePreview(binding.position(), state.preview(),
                                structureName(),
                                ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
                        event.stopPropagation();
                    });
                    options.addScrollViewChild(visibility);
                    x += 44;
                }
                var mode = button(x, y, 52, text("mode." + structure.mode().name().toLowerCase(Locale.ROOT)));
                mode.setActive(!required);
                mode.setOnClick(event -> {
                    request(TerminalChange.structureMode(name, structure.mode() == StructureOperation.BUILD ?
                            StructureOperation.DEMOLISH : StructureOperation.BUILD));
                    event.stopPropagation();
                });
                options.addScrollViewChild(mode);
                if (!required) controls.add(mode);
                if (definition.isAllowFlip()) {
                    var flip = button(x + 54, y, contentWidth - x - 54,
                            text("flip", text(structure.flipMode() ? "on" : "off")));
                    flip.setOnClick(event -> {
                        request(TerminalChange.structureFlip(name, !structure.flipMode()));
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
                            () -> request(TerminalChange.repetition(name, index, amount - 1)),
                            () -> request(TerminalChange.repetition(name, index, amount + 1)),
                            amount > repetition.minimum(), amount < repetition.maximum());
                    y += 20;
                }
                for (var choice : pattern.getChoiceDescriptors()) {
                    var values = choice.alternatives();
                    int index = values.indexOf(structure.choices().getOrDefault(choice.path(), values.getFirst()));
                    addStep(y, contentWidth, text("variant_index", index + 1, values.size()),
                            () -> request(TerminalChange.choice(name, choice.path(), values.get(index - 1))),
                            () -> request(TerminalChange.choice(name, choice.path(), values.get(index + 1))),
                            index > 0, index + 1 < values.size());
                    y += 20;
                }
                options.viewContainer.getLayout().width(contentWidth - 3).height(y);
                optionsHeight = Math.min(y, Math.max(40, height / 3));
                UITemplate.setBounds(options, 4, 20, contentWidth, optionsHeight);
                preview.getLayout().left(4).top(22 + optionsHeight);
                preview.resize(contentWidth, height - optionsHeight - 46);
                blocks.resize(width - BlockChoiceList.WIDTH - 4, 20, height - 44);
                blocks.setChoices(new BatchPlanner().activeTierChoiceOptions(definition, new StructureRequest(name,
                        structure.repetitions(), structure.tierChoices(), structure.flipMode(), structure.mode(),
                        structure.choices())),
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

            private void request(@Nullable TerminalChange update) {
                if (!ready()) return;
                pendingSequence = state.acknowledgedSequence() + 1;
                if (update == null) {
                    terminal.rpcToServer("execute", pendingSequence, state.revision(),
                            Long.parseUnsignedLong(state.preview().fingerprint(), 16));
                } else {
                    terminal.rpcToServer("change", pendingSequence, state.revision(), update);
                }
                updateInteractivity();
            }

            private void receiveState() {
                var previous = state.machine();
                state = terminal.getState();
                if (state.acknowledgedSequence() >= pendingSequence) pendingSequence = -1;
                if (!previous.equals(state.machine())) rebuildOptions();
                updateDisplay();
            }

            private void updateDisplay() {
                preview.setPreview(state.preview());
                if (terminal.getState() != null) {
                    MultiblockInWorldPreviewRenderer.updatePreview(binding.position(), state.preview(),
                            ConfigHolder.INSTANCE.client.inWorldPreviewDuration * 20);
                }
                var summary = state.lastExecution();
                result.setValue(summary == null ? Component.empty() :
                        Component.translatable("gtpm.multiblock.autobuild.status." +
                                summary.status().name().toLowerCase(Locale.ROOT)));
                result.getStyle().tooltips(summary == null ? Component.empty() :
                        Component.translatable("gtpm.multiblock.autobuild.terminal.result",
                                Component.translatable("gtpm.multiblock.autobuild.status." +
                                        summary.status().name().toLowerCase(Locale.ROOT)),
                                summary.placed(), summary.removed()));
                if (controls.isEmpty()) rebuildOptions();
                updateInteractivity();
            }

            private boolean ready() {
                return terminal.getState() != null && pendingSequence < 0 && !state.executing();
            }

            private void updateInteractivity() {
                boolean ready = ready();
                controls.forEach(button -> button.setActive(ready));
                blocks.setInteractive(ready);
                execute.setActive(ready);
            }

            @Override
            public void screenTick() {
                super.screenTick();
                if (visibility != null) {
                    boolean visible = MultiblockInWorldPreviewRenderer.isPreviewVisible(binding.position(),
                            structureName());
                    if (worldVisible != visible) {
                        worldVisible = visible;
                        visibility.setText(text(visible ? "visible" : "hidden"));
                    }
                }
            }

            @Override
            protected void onRemoved() {
                terminal.setStateListener(null);
                super.onRemoved();
            }
        }

        var root = new Page();
        return UI.of(root, root::resize);
    }

    private static GTButtonElement button(int x, int y, int width, Component text) {
        var button = new GTButtonElement(x, y, width, 16);
        button.setButtonTexture(GuiTextures.VANILLA_BUTTON).setText(text);
        button.getLayout().paddingAll(0);
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
