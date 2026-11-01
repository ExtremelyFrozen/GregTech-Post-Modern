package com.gregtechceu.gtceu.integration.ae2.autobuild;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.items.tools.powered.WirelessTerminalItem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import org.jspecify.annotations.Nullable;


/**
 * Supplies automatic-build materials through one deterministic AE wireless-terminal binding.
 *
 * <p>
 * Construction selects the first valid supported terminal in stable inventory order. Every later operation
 * revalidates that same slot path, terminal item and link target; a new terminal state refresh creates a new source
 * and may select a different first valid binding.
 * </p>
 */
public final class MEWirelessMaterialSource implements MaterialSource {

    private final ServerPlayer player;
    private final @Nullable WirelessBinding binding;
    private final @Nullable BuildProblem selectionProblem;

    private MEWirelessMaterialSource(ServerPlayer player, Selection selection) {
        this.player = player;
        this.binding = selection.binding();
        this.selectionProblem = selection.problem();
    }

    public static MaterialRequest.Result request(ServerPlayer player, MaterialRequest request) {
        return request.fulfill(new MEWirelessMaterialSource(player,
                selectBinding(player, request.getExcludedContainer())));
    }

    @Override
    public SourceKind kind() {
        return SourceKind.ME;
    }

    @Override
    public @Nullable SourceDescriptor descriptor() {
        return binding == null ? null : binding.descriptor();
    }

    @Override
    public long bindingFingerprint() {
        return binding == null ? 0 : binding.fingerprint();
    }

    @Override
    public @Nullable BuildProblem unavailableProblem() {
        return validatedInventory().problem();
    }

    @Override
    public Availability availability(ObjectCollection<MaterialKey> keys) {
        InventoryAccess access = validatedInventory();
        SourceDescriptor descriptor = binding == null ? null : binding.descriptor();
        long bindingFingerprint = binding == null ? 0 : binding.fingerprint();
        if (access.problem() != null) {
            return new Availability(kind(), Object2LongMaps.emptyMap(), access.problem(), descriptor,
                    bindingFingerprint);
        }
        Object2LongMap<MaterialKey> availability = new Object2LongLinkedOpenHashMap<>();
        for (MaterialKey key : keys) {
            AEItemKey aeKey = AEItemKey.of(key.prototype());
            long amount = aeKey == null ? 0 : access.inventory().extract(aeKey, Long.MAX_VALUE,
                    Actionable.SIMULATE, IActionSource.ofPlayer(player));
            availability.put(key, Math.max(0, amount));
        }
        return new Availability(kind(), availability, null, descriptor, bindingFingerprint);
    }

    @Override
    public Session openSession() {
        return new MEWirelessSession(player, binding, selectionProblem);
    }

    private InventoryAccess validatedInventory() {
        if (binding == null) {
            BuildProblem problem = selectionProblem == null ? problem(BuildProblem.Type.AE_NOT_LINKED,
                    "gtpm.multiblock.autobuild.me_no_linked_terminal") : selectionProblem;
            return new InventoryAccess(null, problem);
        }
        return validateBinding(player, binding);
    }

    private static Selection selectBinding(ServerPlayer player, ItemStack excludedTerminal) {
        IItemHandler inventory = player.getCapability(Capabilities.ItemHandler.ENTITY);
        if (inventory == null) {
            return Selection.failed(problem(BuildProblem.Type.ME_UNAVAILABLE,
                    "gtpm.multiblock.autobuild.me_no_player_inventory"));
        }
        Selection selected = selectBinding(player, excludedTerminal, inventory, new ReferenceOpenHashSet<>(), IntList.of());
        if (selected.binding() != null || selected.problem() != null) {
            return selected;
        }
        return Selection.failed(problem(BuildProblem.Type.AE_NOT_LINKED,
                "gtpm.multiblock.autobuild.me_no_linked_terminal"));
    }

    private static Selection selectBinding(ServerPlayer player, ItemStack excludedTerminal, IItemHandler handler,
                                           ReferenceSet<IItemHandler> visitedHandlers,
                                           IntList parentPath) {
        if (!visitedHandlers.add(handler)) {
            return Selection.empty();
        }
        Selection firstFailure = Selection.empty();
        // Direct terminals in this handler always precede terminals contained by any of its slots.
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || stack == excludedTerminal) {
                continue;
            }
            IntList slotPath = append(parentPath, slot);
            if (stack.getItem() instanceof WirelessTerminalItem) {
                GlobalPos linkedPos = stack.get(AEComponents.WIRELESS_LINK_TARGET);
                if (linkedPos == null) {
                    if (firstFailure.problem() == null) {
                        firstFailure = Selection.failed(problem(BuildProblem.Type.AE_NOT_LINKED,
                                "gtpm.multiblock.autobuild.me_no_linked_terminal"));
                    }
                } else {
                    WirelessBinding candidate = WirelessBinding.create(stack, linkedPos, slotPath);
                    InventoryAccess access = validateGrid(player, linkedPos);
                    if (access.problem() == null) {
                        return new Selection(candidate, null);
                    }
                    if (firstFailure.problem() == null) {
                        firstFailure = new Selection(candidate, access.problem());
                    }
                }
            }
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || stack == excludedTerminal) {
                continue;
            }
            IntList slotPath = append(parentPath, slot);
            IItemHandler childHandler = stack.getCapability(Capabilities.ItemHandler.ITEM);
            if (childHandler != null) {
                Selection nested = selectBinding(player, excludedTerminal, childHandler, visitedHandlers,
                        slotPath);
                if (nested.binding() != null && nested.problem() == null) {
                    return nested;
                }
                if (firstFailure.problem() == null && nested.problem() != null) {
                    firstFailure = nested;
                }
            }
        }
        return firstFailure;
    }

    private static InventoryAccess validateBinding(ServerPlayer player, WirelessBinding binding) {
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler == null) {
            return InventoryAccess.failed(problem(BuildProblem.Type.ME_UNAVAILABLE,
                    "gtpm.multiblock.autobuild.me_no_player_inventory"));
        }
        ItemStack terminal = ItemStack.EMPTY;
        for (int pathIndex = 0; pathIndex < binding.slotPath().size(); pathIndex++) {
            int slot = binding.slotPath().getInt(pathIndex);
            if (slot < 0 || slot >= handler.getSlots()) {
                return InventoryAccess.failed(sourceChanged());
            }
            ItemStack stack = handler.getStackInSlot(slot);
            if (pathIndex == binding.slotPath().size() - 1) {
                terminal = stack;
                break;
            }
            IItemHandler child = stack.getCapability(Capabilities.ItemHandler.ITEM);
            if (child == null) {
                return InventoryAccess.failed(sourceChanged());
            }
            handler = child;
        }
        if (!(terminal.getItem() instanceof WirelessTerminalItem) ||
                !BuiltInRegistries.ITEM.getKey(terminal.getItem()).equals(binding.itemId()) ||
                !binding.terminalKey().matches(terminal) ||
                !binding.linkTarget().equals(terminal.get(AEComponents.WIRELESS_LINK_TARGET))) {
            return InventoryAccess.failed(sourceChanged());
        }
        return validateGrid(player, binding.linkTarget());
    }

    private static InventoryAccess validateGrid(ServerPlayer player, GlobalPos linkedPos) {
        ServerLevel playerLevel = player.serverLevel();
        ServerLevel linkedLevel = player.getServer().getLevel(linkedPos.dimension());
        if (linkedLevel == null) {
            return InventoryAccess.failed(problem(BuildProblem.Type.AE_LINKED_DIMENSION_MISSING,
                    "gtpm.multiblock.autobuild.me_linked_level_missing"));
        }
        if (!linkedLevel.dimension().equals(playerLevel.dimension())) {
            return InventoryAccess.failed(problem(BuildProblem.Type.AE_WRONG_DIMENSION,
                    "gtpm.multiblock.autobuild.me_wrong_dimension"));
        }
        if (!linkedLevel.hasChunkAt(linkedPos.pos())) {
            return InventoryAccess.failed(problem(BuildProblem.Type.AE_ACCESS_POINT_MISSING,
                    "gtpm.multiblock.autobuild.me_access_point_unloaded"));
        }
        BlockEntity blockEntity = linkedLevel.getBlockEntity(linkedPos.pos());
        if (!(blockEntity instanceof IWirelessAccessPoint accessPoint)) {
            return InventoryAccess.failed(problem(BuildProblem.Type.AE_ACCESS_POINT_MISSING,
                    "gtpm.multiblock.autobuild.me_access_point_missing"));
        }
        if (!accessPoint.isActive()) {
            return InventoryAccess.failed(problem(BuildProblem.Type.AE_ACCESS_POINT_INACTIVE,
                    "gtpm.multiblock.autobuild.me_access_point_inactive"));
        }
        double range = accessPoint.getRange();
        if (player.distanceToSqr(Vec3.atCenterOf(linkedPos.pos())) >= range * range) {
            return InventoryAccess.failed(problem(BuildProblem.Type.AE_OUT_OF_RANGE,
                    "gtpm.multiblock.autobuild.me_out_of_range"));
        }
        IGrid grid = accessPoint.getGrid();
        if (grid == null) {
            return InventoryAccess.failed(problem(BuildProblem.Type.AE_GRID_UNAVAILABLE,
                    "gtpm.multiblock.autobuild.me_grid_missing"));
        }
        return new InventoryAccess(grid.getStorageService().getInventory(), null);
    }

    private static IntList append(IntList path, int slot) {
        IntList nested = new IntArrayList(path.size() + 1);
        nested.addAll(path);
        nested.add(slot);
        return new IntImmutableList(nested);
    }

    private static BuildProblem sourceChanged() {
        return problem(BuildProblem.Type.SOURCE_CHANGED, "gtpm.multiblock.autobuild.me_source_changed");
    }

    private static BuildProblem problem(BuildProblem.Type type, String key) {
        return new BuildProblem(type, null, Component.translatable(key));
    }

    private record Selection(@Nullable WirelessBinding binding, @Nullable BuildProblem problem) {

        private static Selection empty() {
            return new Selection(null, null);
        }

        private static Selection failed(BuildProblem problem) {
            return new Selection(null, problem);
        }
    }

    private record WirelessBinding(ResourceLocation itemId, MaterialKey terminalKey, GlobalPos linkTarget,
                                   IntList slotPath, String terminalName, String selectionIdentity,
                                   long fingerprint) {

        private static WirelessBinding create(ItemStack terminal, GlobalPos linkTarget, IntList slotPath) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(terminal.getItem());
            MaterialKey terminalKey = MaterialKey.of(terminal);
            String path = "inventory/" + String.join("/",
                    slotPath.intStream().mapToObj(Integer::toString).collect(ObjectImmutableList.toList()));
            String componentHash = Integer.toUnsignedString(terminalKey.hashCode(), 16);
            String identity = path + '@' + itemId + '#' + componentHash;
            String name = terminal.getHoverName().getString();
            long fingerprint = 0xcbf29ce484222325L;
            fingerprint = hash(fingerprint, itemId.hashCode());
            fingerprint = hash(fingerprint, terminalKey.hashCode());
            fingerprint = hash(fingerprint, linkTarget.hashCode());
            for (int slot : slotPath) {
                fingerprint = hash(fingerprint, slot);
            }
            return new WirelessBinding(itemId, terminalKey, linkTarget, slotPath, name, identity, fingerprint);
        }

        private SourceDescriptor descriptor() {
            return new SourceDescriptor(SourceKind.ME, terminalName, selectionIdentity, linkTarget);
        }

        private static long hash(long current, long value) {
            return (current ^ value) * 0x100000001b3L;
        }
    }

    private record InventoryAccess(@Nullable MEStorage inventory, @Nullable BuildProblem problem) {

        private static InventoryAccess failed(BuildProblem problem) {
            return new InventoryAccess(null, problem);
        }
    }

    private static final class MEWirelessSession implements Session {

        private final ServerPlayer player;
        private final @Nullable WirelessBinding binding;
        private final @Nullable BuildProblem selectionProblem;
        private final IActionSource actionSource;
        private final Object2LongOpenHashMap<AEItemKey> reservations = new Object2LongOpenHashMap<>();
        private @Nullable BuildProblem reservationProblem;

        private MEWirelessSession(ServerPlayer player, @Nullable WirelessBinding binding,
                                  @Nullable BuildProblem selectionProblem) {
            this.player = player;
            this.binding = binding;
            this.selectionProblem = selectionProblem;
            this.actionSource = IActionSource.ofPlayer(player);
        }

        @Override
        public @Nullable BuildProblem problem() {
            return reservationProblem != null ? reservationProblem : validatedInventory().problem();
        }

        @Override
        public long available(MaterialKey key) {
            InventoryAccess access = validatedInventory();
            if (access.problem() != null) {
                return 0;
            }
            AEItemKey aeKey = AEItemKey.of(key.prototype());
            if (aeKey == null) {
                return 0;
            }
            long available = access.inventory().extract(aeKey, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
            return Math.max(0, available - reservations.getLong(aeKey));
        }

        @Override
        public @Nullable Reservation reserve(ObjectList<ItemStack> candidates) {
            for (ItemStack candidate : candidates) {
                if (!candidate.isEmpty()) {
                    Reservation reservation = reserve(MaterialKey.of(candidate), 1);
                    if (reservation != null) {
                        return reservation;
                    }
                }
            }
            return null;
        }

        @Override
        public @Nullable Reservation reserve(MaterialKey key, long amount) {
            InventoryAccess access = validatedInventory();
            if (access.problem() != null) {
                return null;
            }
            AEItemKey aeKey = AEItemKey.of(key.prototype());
            if (aeKey == null) {
                return null;
            }
            long alreadyReserved = reservations.getLong(aeKey);
            long requested;
            try {
                requested = Math.addExact(alreadyReserved, amount);
            } catch (ArithmeticException exception) {
                reservationProblem = MEWirelessMaterialSource.problem(BuildProblem.Type.COUNT_OVERFLOW,
                        "gtpm.multiblock.autobuild.count_overflow");
                GTCEu.LOGGER.error("ME automatic-build reservation count overflow", exception);
                return null;
            }
            long simulated = access.inventory().extract(aeKey, requested, Actionable.SIMULATE, actionSource);
            long reserved = Math.clamp(simulated - alreadyReserved, 0, amount);
            if (reserved == 0) {
                return null;
            }
            reservations.put(aeKey, alreadyReserved + reserved);
            return new MEWirelessReservation(this, key, aeKey, reserved);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            InventoryAccess access = validatedInventory();
            if (access.problem() != null) {
                return stack;
            }
            AEItemKey key = AEItemKey.of(stack);
            if (key == null) {
                return stack;
            }
            long inserted = access.inventory().insert(key, stack.getCount(),
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);
            int insertedCount = Math.clamp(inserted, 0, stack.getCount());
            ItemStack remainder = stack.copy();
            remainder.shrink(insertedCount);
            return remainder;
        }

        private InventoryAccess validatedInventory() {
            if (binding == null) {
                BuildProblem problem = selectionProblem == null ? MEWirelessMaterialSource.problem(
                        BuildProblem.Type.AE_NOT_LINKED,
                        "gtpm.multiblock.autobuild.me_no_linked_terminal") : selectionProblem;
                return InventoryAccess.failed(problem);
            }
            return validateBinding(player, binding);
        }

        private void release(AEItemKey key, long amount) {
            long reserved = reservations.getLong(key);
            if (reserved < amount) {
                throw new IllegalStateException("ME material reservation accounting is inconsistent");
            }
            if (reserved == amount) {
                reservations.removeLong(key);
            } else {
                reservations.put(key, reserved - amount);
            }
        }
    }

    private static final class MEWirelessReservation implements Reservation {

        private final MEWirelessSession session;
        private final MaterialKey key;
        private final AEItemKey aeKey;
        private final long reservedAmount;
        private boolean attempted;
        private long committed;
        private long restored;
        private @Nullable BuildProblem commitProblem;

        private MEWirelessReservation(MEWirelessSession session, MaterialKey key, AEItemKey aeKey,
                                      long reservedAmount) {
            this.session = session;
            this.key = key;
            this.aeKey = aeKey;
            this.reservedAmount = reservedAmount;
        }

        @Override
        public ItemStack stack() {
            return key.prototype();
        }

        @Override
        public MaterialKey key() {
            return key;
        }

        @Override
        public long reservedAmount() {
            return reservedAmount;
        }

        @Override
        public boolean commit() {
            return commitExact().complete();
        }

        @Override
        public CommitResult commitExact() {
            if (attempted) {
                return new CommitResult(reservedAmount, committed, commitProblem);
            }
            attempted = true;
            InventoryAccess access = session.validatedInventory();
            if (access.problem() != null) {
                commitProblem = access.problem();
            } else {
                long extracted = access.inventory().extract(aeKey, reservedAmount, Actionable.MODULATE,
                        session.actionSource);
                committed = Math.clamp(extracted, 0, reservedAmount);
            }
            session.release(aeKey, reservedAmount);
            return new CommitResult(reservedAmount, committed, commitProblem);
        }

        @Override
        public ObjectList<ItemStack> rejectedStacks() {
            return ObjectList.of();
        }

        @Override
        public RollbackResult rollback() {
            if (!attempted || restored == committed) {
                return new RollbackResult(committed, restored, null);
            }
            InventoryAccess access = session.validatedInventory();
            if (access.problem() != null) {
                BuildProblem refundProblem = new BuildProblem(BuildProblem.Type.REFUND_FAILED, null,
                        Component.translatable("gtpm.multiblock.autobuild.refund_failed")
                                .append(": ").append(access.problem().message()));
                return new RollbackResult(committed, restored, refundProblem);
            }
            long inserted = access.inventory().insert(aeKey, committed - restored, Actionable.MODULATE,
                    session.actionSource);
            restored += Math.clamp(inserted, 0, committed - restored);
            BuildProblem refundProblem = restored == committed ? null : problem(
                    BuildProblem.Type.REFUND_FAILED, "gtpm.multiblock.autobuild.refund_failed");
            return new RollbackResult(committed, restored, refundProblem);
        }
    }
}
