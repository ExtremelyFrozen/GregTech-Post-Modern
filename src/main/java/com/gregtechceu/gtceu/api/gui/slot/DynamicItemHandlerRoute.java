package com.gregtechceu.gtceu.api.gui.slot;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.transfer.item.NonMutatingItemCapacity;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Routes a fixed logical slot range to the current item handler of one target identity.
 *
 * <p>
 * The target is resolved for every operation. This keeps a menu slot attached to its UUID without retaining a
 * machine-owned group, handler, or mutable list index across lifecycle changes.
 * </p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DynamicItemHandlerRoute implements IItemHandlerModifiable, NonMutatingItemCapacity {

    @Getter
    private final UUID targetId;
    @Getter
    private final UUID targetIncarnation;
    private final int logicalSlotCount;
    private final BiFunction<UUID, UUID, IItemHandlerModifiable> handlerResolver;
    private boolean reportedInvalidShape;
    private boolean reportedUnsafeCapacity;

    /**
     * Creates a route whose logical shape remains stable while its target handler may be replaced.
     *
     * @param targetId          stable business identity resolved for every inventory operation
     * @param targetIncarnation identity of the routed logical target lifecycle
     * @param logicalSlotCount  fixed slot count exposed to the menu
     * @param handlerResolver   resolver returning the matching lifecycle's current handler, or {@code null} when absent
     */
    public DynamicItemHandlerRoute(UUID targetId, UUID targetIncarnation, int logicalSlotCount,
                                   BiFunction<UUID, UUID, IItemHandlerModifiable> handlerResolver) {
        this.targetId = targetId;
        this.targetIncarnation = targetIncarnation;
        this.logicalSlotCount = logicalSlotCount;
        this.handlerResolver = handlerResolver;
    }

    @Override
    public int getSlots() {
        return logicalSlotCount;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        IItemHandlerModifiable handler = resolveCurrentHandler();
        return handler == null ? ItemStack.EMPTY : handler.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        IItemHandlerModifiable handler = resolveCurrentHandler();
        if (handler == null) {
            if (!stack.isEmpty()) {
                GTCEu.LOGGER.error("Rejected a non-empty stack for unresolved dynamic item handler target {} slot {}",
                        targetId, slot);
            }
            return;
        }
        handler.setStackInSlot(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        IItemHandlerModifiable handler = resolveCurrentHandler();
        return handler == null ? stack : handler.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        IItemHandlerModifiable handler = resolveCurrentHandler();
        return handler == null ? ItemStack.EMPTY : handler.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        IItemHandlerModifiable handler = resolveCurrentHandler();
        return handler == null ? 0 : handler.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        IItemHandlerModifiable handler = resolveCurrentHandler();
        return handler != null && handler.isItemValid(slot, stack);
    }

    /**
     * Returns whether this target currently resolves to a correctly shaped handler with a non-mutating capacity path.
     *
     * <p>
     * The resolver is invoked for every call so protocol preparation never relies on a previously resolved handler.
     * </p>
     */
    public boolean isResolved() {
        return resolveCurrentHandler() != null;
    }

    /**
     * Advertises that the route itself never mutates a slot while calculating capacity.
     *
     * <p>
     * An unresolved target produces zero capacity instead of falling back to a clear-and-restore query.
     * </p>
     */
    @Override
    public boolean isNonMutatingEmptySlotCapacityQueryEnabled() {
        return true;
    }

    /**
     * Delegates the special empty-slot capacity query to one resolved handler snapshot.
     */
    @Override
    public int getMaxStackSizeForEmptySlot(int slot, ItemStack stack) {
        IItemHandlerModifiable handler = resolveCurrentHandler();
        if (handler == null) {
            return 0;
        }
        return ((NonMutatingItemCapacity) handler).getMaxStackSizeForEmptySlot(slot, stack);
    }

    @Nullable
    private IItemHandlerModifiable resolveCurrentHandler() {
        IItemHandlerModifiable handler = handlerResolver.apply(targetId, targetIncarnation);
        if (handler == null) {
            reportedInvalidShape = false;
            reportedUnsafeCapacity = false;
            return null;
        }
        if (!hasRequiredSlots(handler)) {
            reportedUnsafeCapacity = false;
            return null;
        }
        reportedInvalidShape = false;
        if (!(handler instanceof NonMutatingItemCapacity itemCapacity) ||
                !itemCapacity.isNonMutatingEmptySlotCapacityQueryEnabled()) {
            if (!reportedUnsafeCapacity) {
                GTCEu.LOGGER.error(
                        "Dynamic item handler target {} does not support non-mutating empty-slot capacity queries",
                        targetId);
                reportedUnsafeCapacity = true;
            }
            return null;
        }
        reportedUnsafeCapacity = false;
        return handler;
    }

    private boolean hasRequiredSlots(IItemHandlerModifiable handler) {
        int resolvedSlotCount = handler.getSlots();
        if (resolvedSlotCount < logicalSlotCount) {
            if (!reportedInvalidShape) {
                GTCEu.LOGGER.error("Dynamic item handler target {} resolved {} slots but its route requires {}",
                        targetId, resolvedSlotCount, logicalSlotCount);
                reportedInvalidShape = true;
            }
            return false;
        }
        return true;
    }
}
