package com.gregtechceu.gtceu.api.transfer.item;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class CustomItemStackHandler extends ItemStackHandler implements NonMutatingItemCapacity {

    @Getter
    @Setter
    protected @NotNull Runnable onContentsChanged = () -> {};
    @Getter
    @Setter
    protected Predicate<ItemStack> filter = stack -> true;

    /**
     * Enables the non-mutating capacity query only for handlers whose validation and insertion limits do not depend
     * on the current occupant.
     */
    @Getter
    @Setter
    private boolean nonMutatingEmptySlotCapacityQueryEnabled;

    public CustomItemStackHandler() {
        super();
    }

    public CustomItemStackHandler(int size) {
        super(size);
    }

    public CustomItemStackHandler(ItemStack itemStack) {
        this(NonNullList.of(ItemStack.EMPTY, itemStack));
    }

    public CustomItemStackHandler(NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return filter.test(stack);
    }

    @Override
    public void onContentsChanged(int slot) {
        onContentsChanged.run();
    }

    /**
     * Returns the amount this handler would accept into an empty slot without mutating the current occupant.
     */
    @Override
    public int getMaxStackSizeForEmptySlot(int slot, ItemStack stack) {
        if (!isNonMutatingEmptySlotCapacityQueryEnabled()) {
            GTCEu.LOGGER.error("Handler {} does not support the default non-mutating empty-slot capacity query",
                    getClass().getSimpleName());
            throw new UnsupportedOperationException(
                    "Handler does not support the default non-mutating empty-slot capacity query");
        }
        validateSlotIndex(slot);
        if (stack.isEmpty()) {
            return stack.getMaxStackSize();
        }
        if (!isItemValid(slot, stack)) {
            return 0;
        }
        return getStackLimit(slot, stack);
    }

    /**
     * Don't use unless necessary.<br>
     * (A good use case is loading/saving this container's items from/to a {@link ItemContainerContents} component)
     *
     * @return the internal list of items in this handler
     */
    @ApiStatus.Internal
    public NonNullList<ItemStack> getStacks() {
        return this.stacks;
    }

    public void clear() {
        stacks.clear();
        onContentsChanged.run();
    }

    public void dropInventoryInWorld(Level world, BlockPos pos) {
        for (ItemStack stack : stacks) {
            Block.popResource(world, pos, stack);
        }
        clear();
    }

    public static Codec<CustomItemStackHandler> codec(ContextualFieldCodec.Context<CustomItemStackHandler> context) {
        return context.currentValue().codec();
    }

    public Codec<CustomItemStackHandler> codec() {
        return ItemStack.OPTIONAL_CODEC.listOf().xmap(items -> {
            for (int i = 0; i < stacks.size(); i++) {
                stacks.set(i, items.get(i).copy());
            }
            onContentsChanged.run();
            return this;
        }, handler -> handler.stacks.stream().map(ItemStack::copy).toList());
    }
}
