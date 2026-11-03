package com.gregtechceu.gtceu.common.cover.ender;

import com.gregtechceu.gtceu.api.blockentity.ConfigCopyHelper;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.SelectableEnum;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTEnumSelectorElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTScrollerViewElement;
import com.gregtechceu.gtceu.api.gui.element.GTTextFieldElement;
import com.gregtechceu.gtceu.api.gui.element.GTToggleButtonElement;
import com.gregtechceu.gtceu.api.gui.factory.CoverUIProvider;
import com.gregtechceu.gtceu.api.gui.factory.UICoverHolder;
import com.gregtechceu.gtceu.api.gui.sync.StreamAccessor;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.MachineCoverContainer;
import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEnderRegistry;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCSender;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.sync.SyncValue;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.syncdata.AccessorRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.regex.Pattern;

@SuppressWarnings("SameParameterValue")
public abstract class AbstractEnderLinkCover<T extends VirtualEntry> extends CoverBehavior
                                            implements CoverUIProvider, IControllable {

    public static final Pattern COLOR_INPUT_PATTERN = Pattern.compile("^[0-9a-fA-F]{0,8}$");

    private static final int WIDGET_BOARD = 20;
    private static final int GROUP_WIDTH = 176;
    private static final int TOTAL_WIDTH = 156;
    private static final int BUTTON_SIZE = 16;

    static {
        AccessorRegistries.registerAccessor(new StreamAccessor<>(VirtualEntry.class, VirtualEntry.STREAM_CODEC));
    }

    protected final ConditionalSubscriptionHandler subscriptionHandler;

    @SaveField
    @SyncToClient
    protected String colorStr = VirtualEntry.DEFAULT_COLOR;
    @Getter
    @SaveField
    @SyncToClient
    protected Permissions permission = Permissions.PUBLIC;
    @SaveField
    @Getter
    protected boolean isWorkingEnabled = true;
    @Getter
    @SaveField
    @SyncToClient
    protected ManualIOMode manualIOMode = ManualIOMode.DISABLED;
    @Getter
    @SaveField
    @SyncToClient
    @RerenderOnChanged
    protected IO io = IO.OUT;
    @SyncToClient
    boolean isAnyChanged = false;

    public AbstractEnderLinkCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        subscriptionHandler = new ConditionalSubscriptionHandler(coverHolder, this::update, this::isSubscriptionActive);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        subscriptionHandler.initialize(coverHolder.getLevel());
    }

    @Override
    public abstract boolean canAttach();

    @Override
    public void onAttached(@NotNull ItemStack itemStack, @Nullable ServerPlayer player) {
        super.onAttached(itemStack, player);
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        subscriptionHandler.unsubscribe();
        if (!coverHolder.isRemote()) {
            VirtualEnderRegistry.getInstance()
                    .deleteEntryIf(getOwner(), getEntryType(), getChannelName(), VirtualEntry::canRemove);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        subscriptionHandler.unsubscribe();
        if (!coverHolder.isRemote()) {
            VirtualEnderRegistry.getInstance()
                    .deleteEntryIf(getOwner(), getEntryType(), getChannelName(), VirtualEntry::canRemove);
        }
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        if (this.isWorkingEnabled != isWorkingAllowed) {
            this.isWorkingEnabled = isWorkingAllowed;
            subscriptionHandler.updateSubscription();
        }
    }

    @Override
    public boolean canCreateUI(Player player, UICoverHolder holder) {
        return holder.getCover() == this;
    }

    @Override
    public UI createUI(Player player, UICoverHolder holder) {
        return UI.of(new EnderLinkRootElement(player, holder, this));
    }

    public void setIo(@NotNull IO io) {
        if (io == IO.IN || io == IO.OUT) {
            this.io = io;
            syncDataHolder.markClientSyncFieldDirty("io");
            subscriptionHandler.updateSubscription();
        }
    }

    public UUID getOwner() {
        if (permission == Permissions.PRIVATE && coverHolder instanceof MachineCoverContainer mcc) {
            var owner = mcc.getMachine().getOwner();
            return owner != null ? owner.getPlayerUUID() : null;
        }
        return null;
    }

    protected boolean isSubscriptionActive() {
        return isWorkingEnabled();
    }

    protected abstract String identifier();

    protected abstract VirtualEntry getEntry();

    protected abstract void setEntry(VirtualEntry entry);

    protected final String getChannelName() {
        return identifier() + this.colorStr;
    }

    public void setChannelName(@NotNull String name) {
        if (coverHolder.isRemote()) return;
        VirtualEnderRegistry.getInstance().deleteEntryIf(getOwner(), getEntryType(), getChannelName(),
                VirtualEntry::canRemove);
        this.colorStr = normalizeColorInput(name);
        syncDataHolder.markClientSyncFieldDirty("colorStr");
        setVirtualEntry();
    }

    protected final String getChannelName(VirtualEntry entry) {
        return identifier() + entry.getColorStr();
    }

    public void setPermission(@NotNull Permissions permission) {
        if (coverHolder.isRemote()) return;
        VirtualEnderRegistry.getInstance().deleteEntryIf(getOwner(), getEntryType(), getChannelName(),
                VirtualEntry::canRemove);
        this.permission = permission;
        syncDataHolder.markClientSyncFieldDirty("permission");

        setVirtualEntry();
    }

    protected void setVirtualEntry() {
        setEntry(VirtualEnderRegistry.getInstance().getOrCreateEntry(getOwner(), getEntryType(), getChannelName()));
        getEntry().setColor(this.colorStr);
        markEnderLinkUIChanged();
        subscriptionHandler.updateSubscription();
    }

    protected abstract EntryTypes<T> getEntryType();

    protected void update() {
        long timer = coverHolder.getOffsetTimer();
        if (timer % 5 != 0) return;

        if (isWorkingEnabled() && !coverHolder.isRemote()) {
            var entry = VirtualEnderRegistry.getInstance().getOrCreateEntry(getOwner(), getEntryType(),
                    getChannelName());
            if (!entry.getColorStr().equals(this.colorStr)) {
                entry.setColor(this.colorStr);
            }
            if (!getEntry().equals(entry)) {
                setEntry(entry);
            }
            transfer();
        }

        if (isAnyChanged) {
            isAnyChanged = false;
        }
        subscriptionHandler.updateSubscription();
    }

    protected abstract void transfer();

    public void setManualIOMode(@NotNull ManualIOMode manualIOMode) {
        this.manualIOMode = manualIOMode;
        syncDataHolder.markClientSyncFieldDirty("manualIOMode");
        subscriptionHandler.updateSubscription();
    }

    @Nullable
    protected FilterHandler<?, ?> getFilterHandler() {
        return null;
    }

    protected abstract UIElement addVirtualEntryElement(VirtualEntry entry, int x, int y, int width, int height,
                                                        boolean canClick);

    protected abstract String getUITitle();

    protected int getColor() {
        return VirtualEntry.parseColor(this.colorStr);
    }

    @Override
    public SyncFieldData copyConfig(HolderLookup.Provider registries) {
        return ConfigCopyHelper.withFields(super.copyConfig(registries), fields -> fields
                .put(SyncFieldData.key("colorStr"),
                        colorStr)
                .put(SyncFieldData.key("permission"),
                        getPermission().ordinal())
                .put(SyncFieldData.key("io"),
                        getIo().ordinal())
                .put(SyncFieldData.key("manualIO"),
                        getManualIOMode().ordinal()));
    }

    @Override
    public void pasteConfig(ServerPlayer player, HolderLookup.Provider registries, SyncFieldData config) {
        setChannelName(ConfigCopyHelper.getString(config, "colorStr"));
        setPermission(Permissions.values()[ConfigCopyHelper.getInt(config, "permission")]);
        setIo(IO.values()[ConfigCopyHelper.getInt(config, "io")]);
        setManualIOMode(ManualIOMode.values()[ConfigCopyHelper.getInt(config, "manualIO")]);
        super.pasteConfig(player, registries, config);
    }

    @RPCMethod
    public void configureAbstractEnderLink(String channelColor, Permissions permission, IO io,
                                           ManualIOMode manualIOMode, boolean workingEnabled) {
        setChannelName(normalizeColorInput(channelColor));
        setPermission(permission);
        setIo(io);
        setManualIOMode(manualIOMode);
        setWorkingEnabled(workingEnabled);
        markEnderLinkUIChanged();
    }

    @RPCMethod
    public void setChannelDescription(String channelName, String description) {
        VirtualEntry entry = findEnderLinkActionChannel(channelName);
        if (entry == null) {
            throw new IllegalStateException("Ender link channel is missing: " + channelName);
        }
        setEnderLinkActionChannelDescription(entry, description);
        markEnderLinkUIChanged();
    }

    @RPCMethod
    public void requestChannels(RPCSender sender) {
        sendChannelList(sender);
    }

    @RPCMethod
    public void clearChannelDescription(RPCSender sender, String channelName) {
        VirtualEntry entry = findEnderLinkActionChannel(channelName);
        if (entry == null) {
            throw new IllegalStateException("Ender link channel is missing: " + channelName);
        }
        setEnderLinkActionChannelDescription(entry, "");
        sendChannelList(sender);
    }

    private void sendChannelList(RPCSender sender) {
        BlockPos pos = coverHolder.getBlockPos();
        Direction side = attachedSide;
        List<VirtualEntry> entries = getEnderLinkActionChannelNames().stream()
                .map(name -> {
                    VirtualEntry entry = findEnderLinkActionChannel(name);
                    if (entry == null) {
                        throw new IllegalStateException("Ender link channel is missing: " + name);
                    }
                    return entry;
                })
                .sorted(Comparator.comparing(this::getEnderLinkActionChannelColor))
                .toList();
        sendEnderLinkActionChannelList(sender.asPlayer(), pos, side, entries);
    }

    private void setChannelColor(Player player, UICoverHolder holder, String channelColor) {
        String normalized = normalizeColorInput(channelColor);
        if (player.level().isClientSide()) {
            colorStr = normalized;
            holder.getCover().rpcToServer("configureAbstractEnderLink", colorStr, permission, io, manualIOMode,
                    isWorkingEnabled);
        } else {
            setChannelName(normalized);
        }
    }

    private void setPermission(Player player, UICoverHolder holder, Permissions permission) {
        if (player.level().isClientSide()) {
            this.permission = permission;
            holder.getCover().rpcToServer("configureAbstractEnderLink", colorStr, permission, io, manualIOMode,
                    isWorkingEnabled);
        } else {
            setPermission(permission);
        }
    }

    private void setIo(Player player, UICoverHolder holder, IO io) {
        if (player.level().isClientSide()) {
            this.io = io;
            holder.getCover().rpcToServer("configureAbstractEnderLink", colorStr, permission, io, manualIOMode,
                    isWorkingEnabled);
        } else {
            setIo(io);
        }
    }

    private void setManualIOMode(Player player, UICoverHolder holder, ManualIOMode manualIOMode) {
        if (player.level().isClientSide()) {
            this.manualIOMode = manualIOMode;
            holder.getCover().rpcToServer("configureAbstractEnderLink", colorStr, permission, io, manualIOMode,
                    isWorkingEnabled);
        } else {
            setManualIOMode(manualIOMode);
        }
    }

    private void setWorkingEnabled(Player player, UICoverHolder holder, boolean workingEnabled) {
        if (player.level().isClientSide()) {
            isWorkingEnabled = workingEnabled;
            holder.getCover().rpcToServer("configureAbstractEnderLink", colorStr, permission, io, manualIOMode,
                    isWorkingEnabled);
        } else {
            setWorkingEnabled(workingEnabled);
        }
    }

    private void setDescription(Player player, UICoverHolder holder, String description) {
        VirtualEntry entry = getEntry();
        if (entry != null) {
            entry.setDescription(description);
        }
        if (player.level().isClientSide()) {
            holder.getCover().rpcToServer("setChannelDescription", getChannelName(), description);
        } else {
            markEnderLinkUIChanged();
        }
    }

    private void requestChannels(Player player, UICoverHolder holder) {
        if (player.level().isClientSide()) {
            holder.getCover().rpcToServer("requestChannels");
        }
    }

    private void clearDescription(Player player, UICoverHolder holder, VirtualEntry entry) {
        entry.setDescription("");
        if (player.level().isClientSide()) {
            holder.getCover().rpcToServer("clearChannelDescription", getChannelName(entry));
            requestChannels(player, holder);
        }
    }

    public void markEnderLinkUIChanged() {
        syncDataHolder.markClientSyncFieldDirty("isAnyChanged");
        this.isAnyChanged = true;
    }

    public @NotNull List<String> getEnderLinkActionChannelNames() {
        return List.copyOf(VirtualEnderRegistry.getInstance().getEntryNames(getOwner(), getEntryType()));
    }

    @Nullable
    public VirtualEntry findEnderLinkActionChannel(@NotNull String channelName) {
        return VirtualEnderRegistry.getInstance().getEntry(getOwner(), getEntryType(), channelName);
    }

    public void setEnderLinkActionChannelDescription(@NotNull VirtualEntry entry, @NotNull String description) {
        entry.setDescription(description);
    }

    public @NotNull String getEnderLinkActionChannelColor(@NotNull VirtualEntry entry) {
        return entry.getColorStr();
    }

    public void sendEnderLinkActionChannelList(@NotNull ServerPlayer player, @NotNull BlockPos pos,
                                               @NotNull Direction side,
                                               @NotNull List<? extends VirtualEntry> entries) {
        if (!(player.containerMenu instanceof ModularUIContainerMenu menu)) return;
        List<VirtualEntry> snapshot = entries.stream().map(VirtualEntry::copy).toList();
        menu.getModularUI().ui.rootElement.selfAndAllChildren()
                .filter(EnderLinkChannelListReceiver.class::isInstance)
                .map(EnderLinkChannelListReceiver.class::cast)
                .filter(receiver -> receiver.acceptsEnderLinkChannelList(pos, side, coverDefinition.getId()))
                .forEach(receiver -> receiver.receiveEnderLinkChannelList(snapshot, player.registryAccess()));
    }

    private static String normalizeColorInput(String color) {
        String normalized = color;
        if (normalized == null || !COLOR_INPUT_PATTERN.matcher(normalized).matches()) {
            normalized = VirtualEntry.DEFAULT_COLOR;
        }
        if (normalized.length() < 8) {
            normalized += "F".repeat(8 - normalized.length());
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean isValidColorInput(String color) {
        return color != null && COLOR_INPUT_PATTERN.matcher(color).matches();
    }

    public enum Permissions implements SelectableEnum {

        PUBLIC("cover.ender_fluid_link.private.tooltip.disabled",
                GuiTextures.BUTTON_PUBLIC_PRIVATE.getSubTexture(0, 0, 1, 0.5)),

        PRIVATE("cover.ender_fluid_link.private.tooltip.enabled",
                GuiTextures.BUTTON_PUBLIC_PRIVATE.getSubTexture(0, 0.5, 1, 0.5));

        @Getter
        private final String tooltip;
        @Getter
        private final IGuiTexture icon;

        Permissions(String tooltip, IGuiTexture icon) {
            this.tooltip = tooltip;
            this.icon = icon;
        }
    }

    private static final class EnderLinkRootElement extends UIElement implements EnderLinkChannelListReceiver {

        private final Player player;
        private final UICoverHolder holder;
        private final AbstractEnderLinkCover<?> cover;
        private final UIElement mainGroup;
        private final UIElement mainChannelGroup;
        private final GTScrollerViewElement channelsGroup;
        // Each server menu owns this value; its native UI transport publishes only to that opening's player.
        private final SyncValue<VirtualEntry[]> channelEntries;
        private boolean showChannels;

        EnderLinkRootElement(Player player, UICoverHolder holder, AbstractEnderLinkCover<?> cover) {
            this.player = player;
            this.holder = holder;
            this.cover = cover;
            this.mainGroup = UITemplate.setBounds(new UIElement(), 0, 0, GROUP_WIDTH, 137);
            this.channelsGroup = new GTScrollerViewElement(0, 20, 170, 110);
            this.mainChannelGroup = UITemplate.setBounds(new UIElement(), 10, 20, 156, 20);
            setId("ender_link_root");
            UITemplate.setBounds(this, 0, 0, GROUP_WIDTH, 137 + 82);
            style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));
            initWidgets();
            channelEntries = new SyncValue<>("ender_link_channels", VirtualEntry[].class, new VirtualEntry[0]);
            channelEntries.setToSync(!player.level().isClientSide());
            channelEntries.setAcceptSync(player.level().isClientSide());
            channelEntries.addListener(entries -> addChannelWidgets(Arrays.asList(entries)));
            addSyncValue(channelEntries);
        }

        @Override
        public boolean acceptsEnderLinkChannelList(BlockPos pos, Direction side, ResourceLocation coverDefinitionId) {
            return holder.getPos().equals(pos) &&
                    holder.getSide() == side &&
                    holder.getCoverDefinitionId().equals(coverDefinitionId);
        }

        @Override
        public void receiveEnderLinkChannelList(List<VirtualEntry> entries, HolderLookup.Provider registries) {
            channelEntries.setValue(entries.toArray(VirtualEntry[]::new));
            channelEntries.markAsChanged();
        }

        private void initWidgets() {
            int currentX = 0;
            UIElement titleGroup = UITemplate.setBounds(new UIElement(), 10, 5, GROUP_WIDTH, 20);

            addChild(titleGroup);
            addChild(mainGroup);
            addChild(channelsGroup);
            addChild(UITemplate.bindPlayerInventory(player.getInventory(), GuiTextures.SLOT, 7, 137, true));

            channelsGroup.setVisible(false);
            titleGroup.addChild(createToggleButton());
            titleGroup.addChild(createLabel(15, 3, GROUP_WIDTH - 15, 10, cover.getUITitle(), true));

            mainChannelGroup.addChild(createToggleButtonForPrivacy(currentX));
            currentX += WIDGET_BOARD + 2;
            mainChannelGroup.addChild(createColorBlockElement(currentX, cover::getColor));
            currentX += WIDGET_BOARD + 2;
            mainChannelGroup.addChild(createChannelColorInput(currentX));

            mainChannelGroup.addChild(new ConfirmTextInputElement(0, WIDGET_BOARD + 2, GROUP_WIDTH - WIDGET_BOARD,
                    WIDGET_BOARD, getEntryDescription(), description -> description,
                    description -> description,
                    value -> cover.setDescription(player, holder, value))
                    .setTooltip("cover.ender_fluid_link.tooltip.channel_description"));

            mainGroup.addChild(mainChannelGroup);
            mainGroup.addChild(createWorkingEnabledButton());
            addEnumSelectorElements();
            mainGroup.addChild(
                    cover.addVirtualEntryElement(cover.getEntry(), 146, WIDGET_BOARD, WIDGET_BOARD,
                            WIDGET_BOARD, true));

            FilterHandler<?, ?> filterHandler = cover.getFilterHandler();
            if (filterHandler != null) {
                mainGroup.addChild(filterHandler.createFilterSlotUI(117, 108));
                mainGroup.addChild(filterHandler.createFilterConfigUI(10, 72, 156, 60));
            }
        }

        @Override
        public void screenTick() {
            updateVisibility();
            super.screenTick();
        }

        private GTToggleButtonElement createToggleButton() {
            GTToggleButtonElement button = new GTToggleButtonElement(0, 0, 12, 12, GuiTextures.BUTTON_LIST,
                    () -> showChannels, this::setShowChannels)
                    .setShouldUseBaseBackground();
            button.style(style -> style.tooltips("cover.ender_fluid_link.tooltip.list_button"));
            return button;
        }

        @Contract("_ -> new")
        private @NotNull UIElement createToggleButtonForPrivacy(int currentX) {
            return GTEnumSelectorElement.selectable(currentX, 0, WIDGET_BOARD, WIDGET_BOARD, Permissions.values(),
                    cover::getPermission, value -> cover.setPermission(player, holder, value));
        }

        private ColorBlockElement createColorBlockElement(int currentX, IntSupplier colorSupplier) {
            return new ColorBlockElement(currentX, 0, WIDGET_BOARD, WIDGET_BOARD, colorSupplier);
        }

        private ConfirmTextInputElement createChannelColorInput(int currentX) {
            int groupX = 10;
            int textInputWidth = (GROUP_WIDTH - groupX * 2) - currentX - WIDGET_BOARD - 2;
            return new ConfirmTextInputElement(currentX, 0, textInputWidth, WIDGET_BOARD, cover.colorStr,
                    value -> isValidColorInput(value) ? value : VirtualEntry.DEFAULT_COLOR,
                    AbstractEnderLinkCover::normalizeColorInput,
                    value -> cover.setChannelColor(player, holder, value))
                    .setTooltip("cover.ender_fluid_link.tooltip.channel_name");
        }

        @Contract(" -> new")
        private @NotNull GTToggleButtonElement createWorkingEnabledButton() {
            return new GTToggleButtonElement(116, 82, WIDGET_BOARD, WIDGET_BOARD, GuiTextures.BUTTON_POWER,
                    cover::isWorkingEnabled, value -> cover.setWorkingEnabled(player, holder, value));
        }

        private void addEnumSelectorElements() {
            mainGroup.addChild(GTEnumSelectorElement.selectable(146, 82, WIDGET_BOARD, WIDGET_BOARD,
                    List.of(IO.IN, IO.OUT), cover::getIo, value -> cover.setIo(player, holder, value)));
            mainGroup.addChild(GTEnumSelectorElement.selectable(146, 107, WIDGET_BOARD, WIDGET_BOARD,
                    ManualIOMode.VALUES, cover::getManualIOMode,
                    value -> cover.setManualIOMode(player, holder, value)));
        }

        private void addChannelWidgets(List<? extends VirtualEntry> entries) {
            channelsGroup.clearAllScrollViewChildren();
            int y = 1;
            for (var entry : entries.stream().sorted(Comparator.comparing(VirtualEntry::getColorStr)).toList()) {
                channelsGroup.addScrollViewChild(createChannelElement(entry, 10, y));
                y += 22;
            }
        }

        private @NotNull UIElement createChannelElement(@NotNull VirtualEntry entry, int x, int y) {
            int currentX = 0;
            int margin = 2;
            int availableWidth = TOTAL_WIDTH - (BUTTON_SIZE + margin) * 3;
            String description = entry.getDescription();
            ChannelElement channel = new ChannelElement(x, y, TOTAL_WIDTH, BUTTON_SIZE,
                    () -> cover.getChannelName().equals(cover.getChannelName(entry)),
                    () -> cover.setChannelColor(player, holder, entry.getColorStr()));

            channel.addChild(new ColorBlockElement(currentX, 0, BUTTON_SIZE, BUTTON_SIZE,
                    () -> VirtualEntry.parseColor(entry.getColorStr())));
            currentX += BUTTON_SIZE + margin;

            GTLabelElement channelName = createLabel(BUTTON_SIZE + margin, !description.isEmpty() ? 0 : 4,
                    availableWidth, 8, Component.literal(entry.getColorStr()));
            channelName.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
            channel.addChild(channelName);
            currentX += availableWidth + margin;

            if (!description.isEmpty()) {
                GTLabelElement descriptionLabel = createLabel(BUTTON_SIZE + margin, 10, availableWidth, 8,
                        Component.literal(ChatFormatting.DARK_GRAY + description));
                descriptionLabel.setFontSize(6.0f);
                channel.addChild(descriptionLabel);
            }

            channel.addChild(cover.addVirtualEntryElement(entry, currentX, 0, BUTTON_SIZE, BUTTON_SIZE, false));
            currentX += BUTTON_SIZE + margin;

            GTButtonElement clearButton = new GTButtonElement(currentX, 0, BUTTON_SIZE, BUTTON_SIZE,
                    GuiTextures.BUTTON_CLEAR_GRID, event -> cover.clearDescription(player, holder, entry));
            clearButton.noText();
            clearButton.style(style -> style.tooltips("cover.ender_fluid_link.tooltip.clear_button"));
            channel.addChild(clearButton);

            return channel;
        }

        private void setShowChannels(boolean showChannels) {
            this.showChannels = showChannels;
            updateVisibility();
            if (showChannels) {
                cover.requestChannels(player, holder);
            }
        }

        private void updateVisibility() {
            mainGroup.setVisible(!showChannels);
            channelsGroup.setVisible(showChannels);
        }

        private String getEntryDescription() {
            VirtualEntry entry = cover.getEntry();
            return entry == null ? "" : entry.getDescription();
        }
    }

    private static final class ConfirmTextInputElement extends UIElement {

        private final GTTextFieldElement textField;
        private final Function<String, String> validator;
        private final Function<String, String> returnValidator;
        private final Consumer<String> textResponder;
        private String inputText;

        ConfirmTextInputElement(int x, int y, int width, int height, String text,
                                Function<String, String> validator,
                                Function<String, String> returnValidator,
                                Consumer<String> textResponder) {
            this.validator = validator;
            this.returnValidator = returnValidator;
            this.textResponder = textResponder;
            this.inputText = text == null ? "" : text;
            UITemplate.setBounds(this, x, y, width, height);

            GTButtonElement button = new GTButtonElement(width - height, 0, height, height,
                    GuiTextures.group(GuiTextures.VANILLA_BUTTON, GuiTextures.BUTTON_CHECK), event -> confirm());
            button.noText();
            addChild(button);

            textField = new GTTextFieldElement(1, 1, width - height - 4, height - 2);
            textField.setAnyString();
            textField.setText(inputText, false);
            textField.setTextResponder(value -> inputText = validator.apply(value));
            textField.textFieldStyle(style -> style
                    .textColor(0x404040)
                    .textShadow(false));
            addChild(textField);
        }

        ConfirmTextInputElement setTooltip(String tooltip) {
            textField.style(style -> style.tooltips(tooltip));
            return this;
        }

        private void confirm() {
            inputText = returnValidator.apply(inputText);
            textField.setText(inputText, false);
            textResponder.accept(inputText);
        }
    }

    private static class ColorBlockElement extends UIElement {

        private static boolean showAlpha;

        private final IntSupplier colorSupplier;

        ColorBlockElement(int x, int y, int width, int height, IntSupplier colorSupplier) {
            this.colorSupplier = colorSupplier;
            UITemplate.setBounds(this, x, y, width, height);
            addEventListener(UIEvents.MOUSE_DOWN, event -> {
                showAlpha = !showAlpha;
                event.stopPropagation();
            });
        }

        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);
            int x = Math.round(getPositionX()) + 1;
            int y = Math.round(getPositionY()) + 1;
            int width = Math.round(getSizeWidth()) - 2;
            int height = Math.round(getSizeHeight()) - 2;
            int color = colorSupplier.getAsInt();
            int displayedColor = showAlpha ? color : color | 0xFF000000;
            guiContext.graphics.fill(x, y, x + width, y + height, displayedColor);
            DrawerHelper.drawBorder(guiContext.graphics, x, y, width, height, 0xFF000000, 1);
        }
    }

    private static final class ChannelElement extends UIElement {

        private final BooleanSupplier selectedSupplier;
        private final Runnable onSelected;

        ChannelElement(int x, int y, int width, int height, BooleanSupplier selectedSupplier,
                       Runnable onSelected) {
            this.selectedSupplier = selectedSupplier;
            this.onSelected = onSelected;
            UITemplate.setBounds(this, x, y, width, height);
            addEventListener(UIEvents.MOUSE_DOWN, event -> {
                this.onSelected.run();
                event.stopPropagation();
            });
        }

        @Override
        public void drawBackgroundAdditional(GUIContext guiContext) {
            super.drawBackgroundAdditional(guiContext);
            if (selectedSupplier.getAsBoolean()) {
                DrawerHelper.drawBorder(guiContext.graphics, Math.round(getPositionX()) - 1,
                        Math.round(getPositionY()) - 1, Math.round(getSizeWidth()) + 2,
                        Math.round(getSizeHeight()) + 2, 0xFFFFFFFF, 1);
            }
        }
    }

    private static GTLabelElement createLabel(int x, int y, int width, int height, String text,
                                              boolean translate) {
        GTLabelElement label = new GTLabelElement(x, y, width, height, text, translate);
        styleLabel(label);
        return label;
    }

    private static GTLabelElement createLabel(int x, int y, int width, int height, Component text) {
        GTLabelElement label = new GTLabelElement(x, y, width, height, text);
        styleLabel(label);
        return label;
    }

    private static void styleLabel(GTLabelElement label) {
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
    }
}
