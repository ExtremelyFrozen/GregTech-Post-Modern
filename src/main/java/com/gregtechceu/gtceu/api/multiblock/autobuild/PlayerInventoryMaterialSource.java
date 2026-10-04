package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Supplies automatic-build materials from the player's ordered inventory and supported nested item handlers.
 */
final class PlayerInventoryMaterialSource implements AutoBuildMaterialSource {

    private static final int MAX_CONTAINER_DEPTH = 4;
    private static final AtomicBoolean DEPTH_LIMIT_LOGGED = new AtomicBoolean();

    private final @Nullable IItemHandler root;
    private final ItemStack excludedContainer;
    private final SourceKind kind;
    @Nullable
    private final Player refundOwner;

    PlayerInventoryMaterialSource(@Nullable IItemHandler root, ItemStack excludedContainer, SourceKind kind,
                                  @Nullable Player refundOwner) {
        this.root = root;
        this.excludedContainer = excludedContainer;
        this.kind = kind;
        this.refundOwner = refundOwner;
    }

    @Override
    public SourceKind kind() {
        return kind;
    }

    @Override
    public Session openSession() {
        return new PlayerInventorySession(root, excludedContainer, refundOwner);
    }

    @Override
    public @Nullable AutoBuildProblem unavailableProblem() {
        return root == null ? new AutoBuildProblem(AutoBuildProblem.Type.PLAYER_INVENTORY_UNAVAILABLE, null,
                Component.translatable("gtpm.multiblock.autobuild.player_inventory_unavailable")) : null;
    }

    private static final class PlayerInventorySession implements Session {

        private final @Nullable IItemHandler root;
        private final List<SlotRef> slots = new ArrayList<>();
        private final List<InsertionHandler> insertionHandlers = new ArrayList<>();
        private final ItemStack excludedContainer;
        @Nullable
        private final Player refundOwner;
        private final Map<IItemHandler, Boolean> visitedHandlers = new IdentityHashMap<>();
        private final Map<SlotRef, Long> reservations = new IdentityHashMap<>();
        @Nullable
        private AutoBuildProblem problem;

        private PlayerInventorySession(@Nullable IItemHandler root, ItemStack excludedContainer,
                                       @Nullable Player refundOwner) {
            this.root = root;
            this.excludedContainer = excludedContainer;
            this.refundOwner = refundOwner;
            if (root == null) {
                problem = new AutoBuildProblem(AutoBuildProblem.Type.PLAYER_INVENTORY_UNAVAILABLE, null,
                        Component.translatable("gtpm.multiblock.autobuild.player_inventory_unavailable"));
            } else {
                collectSlots(root, 0);
            }
        }

        @Override
        public @Nullable AutoBuildProblem problem() {
            return problem;
        }

        @Override
        public long available(AutoBuildItemKey key) {
            long available = 0;
            for (SlotRef slot : slots) {
                ItemStack stack = slot.stack();
                if (!key.matches(stack)) {
                    continue;
                }
                long slotAvailable = Math.max(0, stack.getCount() - reservations.getOrDefault(slot, 0L));
                ItemStack simulated = slot.handler().extractItem(slot.slot(), 1, true);
                if (slotAvailable == 0 || !key.matches(simulated) || simulated.getCount() != 1) {
                    continue;
                }
                try {
                    available = Math.addExact(available, slotAvailable);
                } catch (ArithmeticException exception) {
                    problem = new AutoBuildProblem(AutoBuildProblem.Type.COUNT_OVERFLOW, null,
                            Component.translatable("gtpm.multiblock.autobuild.count_overflow"));
                    GTCEu.LOGGER.error("Player automatic-build material availability overflow", exception);
                    return 0;
                }
            }
            return available;
        }

        @Override
        public @Nullable Reservation reserve(List<ItemStack> candidates) {
            for (ItemStack candidate : candidates) {
                if (candidate.isEmpty()) {
                    continue;
                }
                Reservation reservation = reserve(AutoBuildItemKey.of(candidate), 1);
                if (reservation != null) {
                    return reservation;
                }
            }
            return null;
        }

        @Override
        public @Nullable Reservation reserve(AutoBuildItemKey key, long amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Reserved material amount must be positive");
            }
            if (root == null) {
                return null;
            }
            long remaining = amount;
            List<SlotAllocation> allocations = new ArrayList<>();
            for (SlotRef slot : slots) {
                ItemStack stack = slot.stack();
                if (!key.matches(stack)) {
                    continue;
                }
                long alreadyReserved = reservations.getOrDefault(slot, 0L);
                long available = Math.max(0, stack.getCount() - alreadyReserved);
                ItemStack simulated = slot.handler().extractItem(slot.slot(), 1, true);
                if (available == 0 || !key.matches(simulated) || simulated.getCount() != 1) {
                    continue;
                }
                long allocated = Math.min(remaining, available);
                reservations.put(slot, Math.addExact(alreadyReserved, allocated));
                allocations.add(new SlotAllocation(slot, allocated));
                remaining -= allocated;
                if (remaining == 0) {
                    break;
                }
            }
            long reserved = amount - remaining;
            return reserved == 0 ? null : new PlayerInventoryReservation(this, key, reserved, allocations);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (root == null || stack.isEmpty()) {
                return stack;
            }
            ItemStack remainder = stack.copy();
            for (InsertionHandler insertion : insertionHandlers) {
                for (int slot = 0; slot < insertion.handler().getSlots() && !remainder.isEmpty(); slot++) {
                    if (insertion.shouldSkip(slot)) {
                        continue;
                    }
                    remainder = insertion.handler().insertItem(slot, remainder, simulate);
                }
            }
            return remainder;
        }

        private long insert(AutoBuildItemKey key, long amount) {
            long remaining = amount;
            while (remaining > 0) {
                int count = (int) Math.min(remaining, key.prototype().getMaxStackSize());
                ItemStack input = key.prototype();
                input.setCount(count);
                ItemStack remainder = insert(input, false);
                int inserted = count - remainder.getCount();
                if (inserted <= 0) {
                    break;
                }
                remaining -= inserted;
            }
            return amount - remaining;
        }

        private ItemStack returnUnexpected(ItemStack stack) {
            ItemStack remainder = insert(stack, false);
            if (!remainder.isEmpty() && refundOwner != null) {
                ItemEntity dropped = refundOwner.drop(remainder, false);
                if (dropped != null) {
                    return ItemStack.EMPTY;
                }
            }
            return remainder;
        }

        private void release(List<SlotAllocation> allocations) {
            for (SlotAllocation allocation : allocations) {
                reservations.compute(allocation.slot(), (slot, reserved) -> {
                    if (reserved == null || reserved < allocation.amount()) {
                        throw new IllegalStateException("Material reservation accounting is inconsistent");
                    }
                    long remaining = reserved - allocation.amount();
                    return remaining == 0 ? null : remaining;
                });
            }
        }

        private void collectSlots(IItemHandler handler, int depth) {
            if (visitedHandlers.put(handler, Boolean.TRUE) != null) {
                return;
            }
            List<IItemHandler> children = new ArrayList<>();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (isExcluded(stack)) {
                    continue;
                }
                IItemHandler child = stack.getCapability(Capabilities.ItemHandler.ITEM);
                if (child != null) {
                    if (depth < MAX_CONTAINER_DEPTH) {
                        children.add(child);
                    } else if (DEPTH_LIMIT_LOGGED.compareAndSet(false, true)) {
                        GTCEu.LOGGER.debug("Automatic-build inventory traversal stopped at maximum depth {}",
                                MAX_CONTAINER_DEPTH);
                    }
                } else {
                    slots.add(new SlotRef(handler, slot));
                }
            }
            insertionHandlers.add(new InsertionHandler(handler, excludedContainer));
            for (IItemHandler child : children) {
                collectSlots(child, depth + 1);
            }
        }

        private boolean isExcluded(ItemStack stack) {
            return !excludedContainer.isEmpty() && stack == excludedContainer;
        }
    }

    private record SlotRef(IItemHandler handler, int slot) {

        private ItemStack stack() {
            return handler.getStackInSlot(slot);
        }
    }

    private record SlotAllocation(SlotRef slot, long amount) {}

    private record InsertionHandler(IItemHandler handler, ItemStack excludedContainer) {

        private boolean shouldSkip(int slot) {
            ItemStack stack = handler.getStackInSlot(slot);
            return !excludedContainer.isEmpty() && stack == excludedContainer;
        }
    }

    private static final class PlayerInventoryReservation implements Reservation {

        private final PlayerInventorySession session;
        private final AutoBuildItemKey key;
        private final long reservedAmount;
        private final List<SlotAllocation> allocations;
        private boolean attempted;
        private long committed;
        private long restored;
        private final List<ItemStack> rejectedStacks = new ArrayList<>();
        @Nullable
        private AutoBuildProblem commitProblem;

        private PlayerInventoryReservation(PlayerInventorySession session, AutoBuildItemKey key, long reservedAmount,
                                           List<SlotAllocation> allocations) {
            this.session = session;
            this.key = key;
            this.reservedAmount = reservedAmount;
            this.allocations = List.copyOf(allocations);
        }

        @Override
        public ItemStack stack() {
            return key.prototype();
        }

        @Override
        public AutoBuildItemKey key() {
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
                return commitResult();
            }
            attempted = true;
            for (SlotAllocation allocation : allocations) {
                ItemStack current = allocation.slot().stack();
                if (!key.matches(current) || current.getCount() < allocation.amount()) {
                    commitProblem = problem(AutoBuildProblem.Type.SOURCE_CHANGED,
                            "gtpm.multiblock.autobuild.material_changed");
                    session.release(allocations);
                    return commitResult();
                }
                ItemStack simulated = allocation.slot().handler().extractItem(allocation.slot().slot(), 1, true);
                if (!key.matches(simulated) || simulated.getCount() != 1) {
                    session.release(allocations);
                    return commitResult();
                }
            }
            extraction:
            for (SlotAllocation allocation : allocations) {
                for (long index = 0; index < allocation.amount(); index++) {
                    ItemStack current = allocation.slot().stack();
                    if (!key.matches(current)) {
                        commitProblem = problem(AutoBuildProblem.Type.SOURCE_CHANGED,
                                "gtpm.multiblock.autobuild.material_changed");
                        break extraction;
                    }
                    ItemStack extracted = allocation.slot().handler().extractItem(allocation.slot().slot(), 1,
                            false);
                    if (!key.matches(extracted) || extracted.getCount() != 1) {
                        if (!extracted.isEmpty()) {
                            ItemStack rejected = session.returnUnexpected(extracted);
                            if (!rejected.isEmpty()) {
                                rejectedStacks.add(rejected.copy());
                                commitProblem = problem(AutoBuildProblem.Type.REFUND_FAILED,
                                        "gtpm.multiblock.autobuild.refund_failed");
                            } else {
                                commitProblem = problem(AutoBuildProblem.Type.SOURCE_CHANGED,
                                        "gtpm.multiblock.autobuild.material_changed");
                            }
                        }
                        break extraction;
                    }
                    committed = Math.addExact(committed, 1);
                }
            }
            session.release(allocations);
            return commitResult();
        }

        @Override
        public RollbackResult rollback() {
            if (!attempted || restored == committed) {
                return new RollbackResult(committed, restored, null);
            }
            restored = Math.addExact(restored, session.insert(key, committed - restored));
            AutoBuildProblem problem = restored == committed ? null : problem(AutoBuildProblem.Type.REFUND_FAILED,
                    "gtpm.multiblock.autobuild.refund_failed");
            return new RollbackResult(committed, restored, problem);
        }

        @Override
        public List<ItemStack> rejectedStacks() {
            return rejectedStacks.stream().map(ItemStack::copy).toList();
        }

        private CommitResult commitResult() {
            AutoBuildProblem problem = commitProblem;
            if (problem == null && committed != reservedAmount) {
                problem = problem(AutoBuildProblem.Type.EXTRACTION_FAILED,
                        "gtpm.multiblock.autobuild.material_changed");
            }
            return new CommitResult(reservedAmount, committed, problem);
        }

        private static AutoBuildProblem problem(AutoBuildProblem.Type type, String key) {
            return new AutoBuildProblem(type, null, Component.translatable(key));
        }
    }
}
