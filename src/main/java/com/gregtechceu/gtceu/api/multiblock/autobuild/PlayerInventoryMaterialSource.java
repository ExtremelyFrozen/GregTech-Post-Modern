package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import org.jspecify.annotations.Nullable;

/**
 * Supplies automatic-build materials from the player's ordered inventory and supported nested item handlers.
 */
final class PlayerInventoryMaterialSource implements MaterialSource {

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

    static MaterialRequest.Result request(ServerPlayer player, MaterialRequest request) {
        return request.fulfill(() -> new PlayerInventoryMaterialSource(
                player.getCapability(Capabilities.ItemHandler.ENTITY), request.getExcludedContainer(),
                SourceKind.PLAYER, player));
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
    public @Nullable BuildProblem unavailableProblem() {
        return root == null ? new BuildProblem(BuildProblem.Type.PLAYER_INVENTORY_UNAVAILABLE, null,
                Component.translatable("gtpm.multiblock.autobuild.player_inventory_unavailable")) : null;
    }

    private static final class PlayerInventorySession implements Session {

        private final @Nullable IItemHandler root;
        private final ObjectList<SlotRef> slots = new ObjectArrayList<>();
        private final Object2ObjectMap<MaterialKey, ObjectList<SlotRef>> slotsByMaterial = new Object2ObjectOpenHashMap<>();
        private final Object2IntMap<MaterialKey> nextMaterialSlot = new Object2IntOpenHashMap<>();
        private final ObjectList<InsertionHandler> insertionHandlers = new ObjectArrayList<>();
        private final ItemStack excludedContainer;
        @Nullable
        private final Player refundOwner;
        private final ReferenceSet<IItemHandler> visitedHandlers = new ReferenceOpenHashSet<>();
        private final Reference2LongMap<SlotRef> reservations = new Reference2LongOpenHashMap<>();
        @Nullable
        private BuildProblem problem;

        private PlayerInventorySession(@Nullable IItemHandler root, ItemStack excludedContainer,
                                       @Nullable Player refundOwner) {
            this.root = root;
            this.excludedContainer = excludedContainer;
            this.refundOwner = refundOwner;
            if (root == null) {
                problem = new BuildProblem(BuildProblem.Type.PLAYER_INVENTORY_UNAVAILABLE, null,
                        Component.translatable("gtpm.multiblock.autobuild.player_inventory_unavailable"));
            } else {
                collectSlots(root);
            }
        }

        @Override
        public @Nullable BuildProblem problem() {
            return problem;
        }

        @Override
        public long available(MaterialKey key) {
            long available = 0;
            for (SlotRef slot : matchingSlots(key)) {
                ItemStack stack = slot.stack();
                if (!key.matches(stack)) {
                    continue;
                }
                long slotAvailable = Math.max(0, stack.getCount() - reservations.getLong(slot));
                ItemStack simulated = slot.handler().extractItem(slot.slot(), 1, true);
                if (slotAvailable == 0 || !key.matches(simulated) || simulated.getCount() != 1) {
                    continue;
                }
                try {
                    available = Math.addExact(available, slotAvailable);
                } catch (ArithmeticException exception) {
                    problem = new BuildProblem(BuildProblem.Type.COUNT_OVERFLOW, null,
                            Component.translatable("gtpm.multiblock.autobuild.count_overflow"));
                    GTCEu.LOGGER.error("Player automatic-build material availability overflow", exception);
                    return 0;
                }
            }
            return available;
        }

        @Override
        public @Nullable Reservation reserve(ObjectList<ItemStack> candidates) {
            for (ItemStack candidate : candidates) {
                if (candidate.isEmpty()) {
                    continue;
                }
                Reservation reservation = reserve(MaterialKey.of(candidate), 1);
                if (reservation != null) {
                    return reservation;
                }
            }
            return null;
        }

        @Override
        public @Nullable Reservation reserve(MaterialKey key, long amount) {
            if (root == null) {
                return null;
            }
            long remaining = amount;
            ObjectList<SlotAllocation> allocations = new ObjectArrayList<>();
            ObjectList<SlotRef> matching = matchingSlots(key);
            for (int slotIndex = nextMaterialSlot.getInt(key); slotIndex < matching.size(); slotIndex++) {
                SlotRef slot = matching.get(slotIndex);
                ItemStack stack = slot.stack();
                if (!key.matches(stack)) {
                    nextMaterialSlot.put(key, slotIndex + 1);
                    continue;
                }
                long alreadyReserved = reservations.getLong(slot);
                long available = Math.max(0, stack.getCount() - alreadyReserved);
                ItemStack simulated = slot.handler().extractItem(slot.slot(), 1, true);
                if (available == 0 || !key.matches(simulated) || simulated.getCount() != 1) {
                    nextMaterialSlot.put(key, slotIndex + 1);
                    continue;
                }
                long allocated = Math.min(remaining, available);
                reservations.put(slot, Math.addExact(alreadyReserved, allocated));
                allocations.add(new SlotAllocation(slot, allocated));
                remaining -= allocated;
                if (allocated == available) nextMaterialSlot.put(key, slotIndex + 1);
                if (remaining == 0) {
                    break;
                }
            }
            long reserved = amount - remaining;
            return reserved == 0 ? null : new PlayerInventoryReservation(this, key, reserved, allocations);
        }

        private ObjectList<SlotRef> matchingSlots(MaterialKey key) {
            ObjectList<SlotRef> matching = slotsByMaterial.get(key);
            if (matching == null) {
                matching = new ObjectArrayList<>();
                for (SlotRef slot : slots) {
                    if (key.matches(slot.stack())) matching.add(slot);
                }
                slotsByMaterial.put(key, matching);
            }
            return matching;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            if (root == null || stack.isEmpty()) {
                return stack;
            }
            if (!simulate) {
                slotsByMaterial.clear();
                nextMaterialSlot.clear();
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

        private long insert(MaterialKey key, long amount) {
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

        private void release(ObjectList<SlotAllocation> allocations) {
            for (SlotAllocation allocation : allocations) {
                long reserved = reservations.getLong(allocation.slot());
                if (reserved < allocation.amount()) {
                    throw new IllegalStateException("Material reservation accounting is inconsistent");
                }
                long remaining = reserved - allocation.amount();
                if (remaining == 0) reservations.removeLong(allocation.slot());
                else reservations.put(allocation.slot(), remaining);
            }
        }

        private void collectSlots(IItemHandler handler) {
            if (!visitedHandlers.add(handler)) {
                return;
            }
            ObjectList<IItemHandler> children = new ObjectArrayList<>();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (isExcluded(stack)) {
                    continue;
                }
                IItemHandler child = stack.getCapability(Capabilities.ItemHandler.ITEM);
                if (child != null) {
                    children.add(child);
                } else {
                    slots.add(new SlotRef(handler, slot));
                }
            }
            insertionHandlers.add(new InsertionHandler(handler, excludedContainer));
            for (IItemHandler child : children) {
                collectSlots(child);
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
        private final MaterialKey key;
        private final long reservedAmount;
        private final ObjectList<SlotAllocation> allocations;
        private boolean attempted;
        private long committed;
        private long restored;
        private final ObjectList<ItemStack> rejectedStacks = new ObjectArrayList<>();
        @Nullable
        private BuildProblem commitProblem;

        private PlayerInventoryReservation(PlayerInventorySession session, MaterialKey key, long reservedAmount,
                                           ObjectList<SlotAllocation> allocations) {
            this.session = session;
            this.key = key;
            this.reservedAmount = reservedAmount;
            this.allocations = new ObjectImmutableList<>(allocations);
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
                return commitResult();
            }
            attempted = true;
            for (SlotAllocation allocation : allocations) {
                ItemStack current = allocation.slot().stack();
                if (!key.matches(current) || current.getCount() < allocation.amount()) {
                    commitProblem = problem(BuildProblem.Type.SOURCE_CHANGED,
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
                        commitProblem = problem(BuildProblem.Type.SOURCE_CHANGED,
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
                                commitProblem = problem(BuildProblem.Type.REFUND_FAILED,
                                        "gtpm.multiblock.autobuild.refund_failed");
                            } else {
                                commitProblem = problem(BuildProblem.Type.SOURCE_CHANGED,
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
            BuildProblem problem = restored == committed ? null : problem(BuildProblem.Type.REFUND_FAILED,
                    "gtpm.multiblock.autobuild.refund_failed");
            return new RollbackResult(committed, restored, problem);
        }

        @Override
        public ObjectList<ItemStack> rejectedStacks() {
            return rejectedStacks.stream().map(ItemStack::copy).collect(ObjectImmutableList.toList());
        }

        private CommitResult commitResult() {
            BuildProblem problem = commitProblem;
            if (problem == null && committed != reservedAmount) {
                problem = problem(BuildProblem.Type.EXTRACTION_FAILED,
                        "gtpm.multiblock.autobuild.material_changed");
            }
            return new CommitResult(reservedAmount, committed, problem);
        }

        private static BuildProblem problem(BuildProblem.Type type, String key) {
            return new BuildProblem(type, null, Component.translatable(key));
        }
    }
}
