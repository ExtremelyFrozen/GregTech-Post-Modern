package com.gregtechceu.gtceu.integration.ae2.autobuild;

import com.gregtechceu.gtceu.api.multiblock.autobuild.BuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import appeng.api.config.Actionable;
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
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import org.jspecify.annotations.Nullable;

/**
 * Supplies automatic-build materials through one deterministic AE wireless-terminal binding.
 *
 * <p>
 * Construction selects the first usable supported terminal through its linked-grid API in stable inventory order.
 * Every later operation
 * revalidates that same slot path, terminal item and link target; a new terminal state refresh creates a new source
 * and may select a different first valid binding.
 * </p>
 */
public final class MEWirelessMaterialSource implements MaterialSource {

    private final ServerPlayer player;
    private final @Nullable WirelessBinding binding;

    private MEWirelessMaterialSource(ServerPlayer player, @Nullable WirelessBinding binding) {
        this.player = player;
        this.binding = binding;
    }

    public static MaterialRequest.Result request(ServerPlayer player, MaterialRequest request) {
        return request.fulfill(() -> new MEWirelessMaterialSource(player,
                selectBinding(player, request.getExcludedContainer())));
    }

    @Override
    public long bindingFingerprint() {
        return binding == null ? 0 : binding.fingerprint();
    }

    @Override
    public Availability availability(ObjectCollection<MaterialKey> keys) {
        MEStorage inventory = binding == null ? null : validateBinding(player, binding);
        if (inventory == null) {
            return new Availability(Object2LongMaps.emptyMap(), bindingFingerprint());
        }
        Object2LongMap<MaterialKey> availability = new Object2LongLinkedOpenHashMap<>();
        for (MaterialKey key : keys) {
            AEItemKey aeKey = AEItemKey.of(key.prototype());
            long amount = aeKey == null ? 0 : inventory.extract(aeKey, Long.MAX_VALUE,
                    Actionable.SIMULATE, IActionSource.ofPlayer(player));
            availability.put(key, Math.max(0, amount));
        }
        return new Availability(availability, bindingFingerprint());
    }

    @Override
    public Session openSession() {
        return new MEWirelessSession(player, binding);
    }

    private static @Nullable WirelessBinding selectBinding(ServerPlayer player, ItemStack excludedTerminal) {
        IItemHandler inventory = player.getCapability(Capabilities.ItemHandler.ENTITY);
        return inventory == null ? null :
                selectBinding(player, excludedTerminal, inventory, new ReferenceOpenHashSet<>(),
                        IntList.of());
    }

    private static @Nullable WirelessBinding selectBinding(ServerPlayer player, ItemStack excludedTerminal,
                                                           IItemHandler handler,
                                                           ReferenceSet<IItemHandler> visitedHandlers,
                                                           IntList parentPath) {
        if (!visitedHandlers.add(handler)) {
            return null;
        }
        // Direct terminals in this handler always precede terminals contained by any of its slots.
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty() || stack == excludedTerminal) {
                continue;
            }
            if (stack.getItem() instanceof WirelessTerminalItem terminal &&
                    resolveInventory(player, terminal, stack) != null) {
                return WirelessBinding.create(stack, append(parentPath, slot));
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
                WirelessBinding nested = selectBinding(player, excludedTerminal, childHandler, visitedHandlers,
                        slotPath);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static @Nullable MEStorage validateBinding(ServerPlayer player, WirelessBinding binding) {
        IItemHandler handler = player.getCapability(Capabilities.ItemHandler.ENTITY);
        if (handler == null) {
            return null;
        }
        ItemStack terminal = ItemStack.EMPTY;
        for (int pathIndex = 0; pathIndex < binding.slotPath().size(); pathIndex++) {
            int slot = binding.slotPath().getInt(pathIndex);
            if (slot < 0 || slot >= handler.getSlots()) {
                return null;
            }
            ItemStack stack = handler.getStackInSlot(slot);
            if (pathIndex == binding.slotPath().size() - 1) {
                terminal = stack;
                break;
            }
            IItemHandler child = stack.getCapability(Capabilities.ItemHandler.ITEM);
            if (child == null) {
                return null;
            }
            handler = child;
        }
        if (!(terminal.getItem() instanceof WirelessTerminalItem terminalItem) ||
                !binding.terminalKey().matches(terminal)) {
            return null;
        }
        return resolveInventory(player, terminalItem, terminal);
    }

    private static @Nullable MEStorage resolveInventory(ServerPlayer player, WirelessTerminalItem terminal,
                                                        ItemStack stack) {
        IGrid grid = terminal.getLinkedGrid(stack, player.level(), null);
        return grid == null ? null : grid.getStorageService().getInventory();
    }

    private static IntList append(IntList path, int slot) {
        IntList nested = new IntArrayList(path.size() + 1);
        nested.addAll(path);
        nested.add(slot);
        return new IntImmutableList(nested);
    }

    private static BuildProblem sourceChanged() {
        return problem("gtpm.multiblock.autobuild.me_source_changed");
    }

    private static BuildProblem problem(String key) {
        return new BuildProblem(null, Component.translatable(key));
    }

    private record WirelessBinding(MaterialKey terminalKey, IntList slotPath,
                                   long fingerprint) {

        private static WirelessBinding create(ItemStack terminal, IntList slotPath) {
            MaterialKey terminalKey = MaterialKey.of(terminal);
            long fingerprint = 0xcbf29ce484222325L;
            fingerprint = hash(fingerprint, terminalKey.hashCode());
            for (int slot : slotPath) {
                fingerprint = hash(fingerprint, slot);
            }
            return new WirelessBinding(terminalKey, slotPath, fingerprint);
        }

        private static long hash(long current, long value) {
            return (current ^ value) * 0x100000001b3L;
        }
    }

    private static final class MEWirelessSession implements Session {

        private final ServerPlayer player;
        private final @Nullable WirelessBinding binding;
        private final IActionSource actionSource;
        private final Object2LongOpenHashMap<AEItemKey> reservations = new Object2LongOpenHashMap<>();
        private final Object2LongMap<AEItemKey> simulatedAvailability = new Object2LongOpenHashMap<>();

        private MEWirelessSession(ServerPlayer player, @Nullable WirelessBinding binding) {
            this.player = player;
            this.binding = binding;
            this.actionSource = IActionSource.ofPlayer(player);
        }

        @Override
        public long available(MaterialKey key) {
            MEStorage inventory = validatedInventory();
            if (inventory == null) {
                return 0;
            }
            AEItemKey aeKey = AEItemKey.of(key.prototype());
            if (aeKey == null) {
                return 0;
            }
            long available = inventory.extract(aeKey, Long.MAX_VALUE, Actionable.SIMULATE, actionSource);
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
            MEStorage inventory = validatedInventory();
            if (inventory == null) {
                return null;
            }
            AEItemKey aeKey = AEItemKey.of(key.prototype());
            if (aeKey == null) {
                return null;
            }
            long alreadyReserved = reservations.getLong(aeKey);
            long simulated = simulatedAvailability.computeLongIfAbsent(aeKey,
                    material -> inventory.extract(material, Long.MAX_VALUE, Actionable.SIMULATE,
                            actionSource));
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
            MEStorage inventory = validatedInventory();
            if (inventory == null) {
                return stack;
            }
            AEItemKey key = AEItemKey.of(stack);
            if (key == null) {
                return stack;
            }
            long inserted = inventory.insert(key, stack.getCount(),
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE, actionSource);
            int insertedCount = Math.clamp(inserted, 0, stack.getCount());
            ItemStack remainder = stack.copy();
            remainder.shrink(insertedCount);
            return remainder;
        }

        private @Nullable MEStorage validatedInventory() {
            return binding == null ? null : validateBinding(player, binding);
        }

        private void release(AEItemKey key, long amount) {
            simulatedAvailability.removeLong(key);
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
            MEStorage inventory = session.validatedInventory();
            if (inventory == null) {
                commitProblem = sourceChanged();
            } else {
                long extracted = inventory.extract(aeKey, reservedAmount, Actionable.MODULATE,
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
            MEStorage inventory = session.validatedInventory();
            if (inventory == null) {
                return new RollbackResult(committed, restored, problem("gtpm.multiblock.autobuild.refund_failed"));
            }
            long inserted = inventory.insert(aeKey, committed - restored, Actionable.MODULATE,
                    session.actionSource);
            restored += Math.clamp(inserted, 0, committed - restored);
            BuildProblem refundProblem = restored == committed ? null : problem(
                    "gtpm.multiblock.autobuild.refund_failed");
            return new RollbackResult(committed, restored, refundProblem);
        }
    }
}
