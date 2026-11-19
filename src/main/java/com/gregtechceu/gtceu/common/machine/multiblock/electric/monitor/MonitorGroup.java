package com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.IMonitorComponent;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.item.IComponentItem;
import com.gregtechceu.gtceu.api.item.component.IItemComponent;
import com.gregtechceu.gtceu.api.item.component.IMonitorModuleItem;
import com.gregtechceu.gtceu.api.placeholder.PlaceholderContext;
import com.gregtechceu.gtceu.api.sync_system.SyncDataHolder;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.managed.ISyncManaged;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.item.datacomponents.TextLineList;
import com.gregtechceu.gtceu.utils.GlobalPosWithRot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.UnaryOperator;

public class MonitorGroup implements ISyncManaged {

    @Getter
    @SaveField
    @SyncToClient
    private UUID identity;
    @Getter
    @SaveField
    @SyncToClient
    private UUID dynamicItemSlotIncarnation;
    @Getter
    @SaveField
    @SyncToClient
    private UUID moduleSlotIncarnation;
    @Getter
    @SaveField
    @SyncToClient
    private long textConfigurationRevision;
    @Getter
    @SaveField
    @SyncToClient
    private final Set<BlockPos> monitorPositions = new HashSet<>();
    @Getter
    @SaveField
    @SyncToClient
    private String name;
    @Getter
    @SaveField
    @SyncToClient
    private final CustomItemStackHandler itemStackHandler;
    @Getter
    @SaveField
    @SyncToClient
    private final CustomItemStackHandler placeholderSlotsHandler;
    @SaveField
    @SyncToClient
    @Setter
    private @Nullable BlockPos target;
    @SaveField
    @SyncToClient
    @Getter
    @Setter
    private @Nullable Direction targetCoverSide;
    @SaveField
    @SyncToClient
    @Getter
    @Setter
    private int dataSlot = 0;

    private SyncDataHolder syncDataHolder;

    public MonitorGroup() {
        this("");
    }

    @Override
    public SyncDataHolder getSyncDataHolder() {
        if (syncDataHolder == null) syncDataHolder = new SyncDataHolder(this);
        return syncDataHolder;
    }

    @Override
    public ISyncManaged getParentSyncObject() {
        return null;
    }

    public static boolean isModule(ItemStack stack) {
        if (stack.getItem() instanceof IComponentItem componentItem) {
            for (IItemComponent itemComponent : componentItem.getComponents()) {
                if (itemComponent instanceof IMonitorModuleItem) return true;
            }
        }
        return false;
    }

    public static CustomItemStackHandler createModuleHandler() {
        CustomItemStackHandler customItemStackHandler = new CustomItemStackHandler(1);
        customItemStackHandler.setFilter(MonitorGroup::isModule);
        customItemStackHandler.setNonMutatingEmptySlotCapacityQueryEnabled(true);
        return customItemStackHandler;
    }

    /** Creates the fixed placeholder inventory used by new and codec-restored groups. */
    public static CustomItemStackHandler createPlaceholderHandler() {
        CustomItemStackHandler placeholderHandler = new CustomItemStackHandler(
                CentralMonitorGroupItemHandler.PLACEHOLDER_SLOT_COUNT);
        placeholderHandler.setNonMutatingEmptySlotCapacityQueryEnabled(true);
        return placeholderHandler;
    }

    public MonitorGroup(String name) {
        this(name, createModuleHandler(), createPlaceholderHandler());
    }

    public MonitorGroup(String name, CustomItemStackHandler handler, CustomItemStackHandler placeholderSlotsHandler) {
        this(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 0, name, handler, placeholderSlotsHandler);
    }

    private MonitorGroup(UUID identity, UUID dynamicItemSlotIncarnation, UUID moduleSlotIncarnation,
                         long textConfigurationRevision,
                         String name, CustomItemStackHandler handler,
                         CustomItemStackHandler placeholderSlotsHandler) {
        this.identity = identity;
        this.dynamicItemSlotIncarnation = dynamicItemSlotIncarnation;
        this.moduleSlotIncarnation = moduleSlotIncarnation;
        setTextConfigurationRevision(textConfigurationRevision);
        this.name = name;
        this.itemStackHandler = handler;
        this.itemStackHandler.setFilter(MonitorGroup::isModule);
        this.placeholderSlotsHandler = placeholderSlotsHandler;
    }

    /**
     * Restores a group with the stable identities carried by saved or synchronized data.
     */
    public static MonitorGroup restore(UUID identity, UUID dynamicItemSlotIncarnation, UUID moduleSlotIncarnation,
                                       String name,
                                       CustomItemStackHandler handler,
                                       CustomItemStackHandler placeholderSlotsHandler) {
        return restore(identity, dynamicItemSlotIncarnation, moduleSlotIncarnation, 0, name, handler,
                placeholderSlotsHandler);
    }

    /**
     * Restores a group with its stable identities and text configuration revision.
     */
    public static MonitorGroup restore(UUID identity, UUID dynamicItemSlotIncarnation, UUID moduleSlotIncarnation,
                                       long textConfigurationRevision,
                                       String name,
                                       CustomItemStackHandler handler,
                                       CustomItemStackHandler placeholderSlotsHandler) {
        return new MonitorGroup(identity, dynamicItemSlotIncarnation, moduleSlotIncarnation,
                textConfigurationRevision, name, handler, placeholderSlotsHandler);
    }

    /**
     * Creates a new empty group with an identity already validated by the owning Central Monitor.
     */
    public static MonitorGroup createWithIdentity(UUID identity, String name) {
        return new MonitorGroup(identity, UUID.randomUUID(), UUID.randomUUID(), 0, name, createModuleHandler(),
                createPlaceholderHandler());
    }

    /**
     * Invalidates actions opened for the previous physical module-slot occupant.
     */
    public void rotateModuleSlotIncarnation() {
        moduleSlotIncarnation = UUID.randomUUID();
        textConfigurationRevision = 0;
    }

    /**
     * Restores the authoritative revision carried by save data or server synchronization.
     */
    public void setTextConfigurationRevision(long revision) {
        if (revision < 0) {
            throw new IllegalArgumentException("Monitor group text configuration revision must be non-negative");
        }
        textConfigurationRevision = revision;
    }

    /**
     * Applies one validated text configuration without replacing the physical module-slot occupant.
     */
    public void applyTextConfiguration(TextLineList configuration) {
        long nextRevision = Math.incrementExact(textConfigurationRevision);
        itemStackHandler.getStackInSlot(0).set(GTDataComponents.FORMAT_STRING_LIST.get(), configuration);
        textConfigurationRevision = nextRevision;
    }

    /**
     * Atomically replaces the raw target configuration and invalidates the cover side derived from its old target.
     */
    public void setTargetAndDataSlot(@Nullable BlockPos target, int dataSlot) {
        this.target = target;
        this.dataSlot = dataSlot;
        this.targetCoverSide = null;
    }

    public void add(BlockPos pos) {
        monitorPositions.add(pos);
    }

    public void remove(BlockPos pos) {
        monitorPositions.remove(pos);
    }

    public List<BlockPos> getRow(int row, UnaryOperator<BlockPos> toRelative) throws IndexOutOfBoundsException {
        IntSet yLevelsSet = new IntOpenHashSet();
        for (BlockPos pos : monitorPositions) {
            yLevelsSet.add(toRelative.apply(pos).getY());
        }
        if (row < 0) row += yLevelsSet.size();
        int y = yLevelsSet.intStream().sorted().toArray()[row];
        List<BlockPos> rowPositions = new ArrayList<>();
        for (BlockPos pos : monitorPositions) {
            if (toRelative.apply(pos).getY() == y) {
                rowPositions.add(toRelative.apply(pos));
            }
        }
        rowPositions.sort(Comparator.comparingInt(Vec3i::getX));
        return rowPositions;
    }

    public boolean contains(BlockPos pos) {
        return monitorPositions.contains(pos);
    }

    public boolean isEmpty() {
        return monitorPositions.isEmpty();
    }

    public @Nullable CoverBehavior getTargetCover(Level level) {
        PlaceholderContext context = resolvePlaceholderContext(level, identity);
        return context == null ? null : context.cover();
    }

    public @Nullable BlockPos getTargetRaw() {
        return target;
    }

    public @Nullable BlockPos getTarget(Level level) {
        PlaceholderContext context = resolvePlaceholderContext(level, identity);
        if (context == null) return null;
        setTargetCoverSide(context.side());
        return context.pos();
    }

    public Level getTargetLevel(Level level) {
        PlaceholderContext context = resolvePlaceholderContext(level, identity);
        return context == null ? level : context.level();
    }

    /**
     * Resolves a loaded target in the monitor's level or through its selected data hatch slot.
     * The caller can skip evaluation while the target or its level is unavailable.
     */
    public @Nullable PlaceholderContext resolvePlaceholderContext(Level monitorLevel, UUID uuid) {
        BlockPos pos = target;
        if (pos == null || !monitorLevel.isLoaded(pos)) return null;

        Level targetLevel = monitorLevel;
        Direction side = targetCoverSide;
        IMonitorComponent component = GTCapabilityHelper.getMonitorComponent(monitorLevel, pos, null);
        IItemHandler dataItems = component == null ? null : component.getDataItems();
        if (dataItems != null) {
            if (dataSlot < 0 || dataSlot >= dataItems.getSlots()) return null;
            GlobalPosWithRot link = dataItems.getStackInSlot(dataSlot).get(GTDataComponents.MONITOR_TARGET);
            if (link == null) return null;
            if (!link.dimension().equals(monitorLevel.dimension())) {
                MinecraftServer server = monitorLevel.getServer();
                if (server == null) return null;
                targetLevel = server.getLevel(link.dimension());
                if (targetLevel == null) return null;
            }
            pos = link.pos();
            if (!targetLevel.isLoaded(pos)) return null;
            side = link.side();
        }

        CoverBehavior cover = null;
        if (side != null) {
            ICoverable coverable = GTCapabilityHelper.getCoverable(targetLevel, pos, side);
            if (coverable != null) cover = coverable.getCoverAtSide(side);
        }
        return new PlaceholderContext(targetLevel, pos, side, placeholderSlotsHandler, cover, null, uuid);
    }
}
