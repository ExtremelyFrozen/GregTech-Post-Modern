package com.gregtechceu.gtceu.api.multiblock.autobuild;

import net.minecraft.world.item.ItemStack;

/**
 * Identifies an automatic-build material by item and complete data components while ignoring its count.
 *
 * <p>
 * The stored prototype is never exposed directly so callers cannot mutate a key after it has entered a map.
 * </p>
 */
public final class AutoBuildItemKey {

    private final ItemStack prototype;
    private final int hashCode;

    private AutoBuildItemKey(ItemStack prototype) {
        this.prototype = prototype.copyWithCount(1);
        this.hashCode = ItemStack.hashItemAndComponents(this.prototype);
    }

    /**
     * Creates a key from a non-empty stack.
     */
    public static AutoBuildItemKey of(ItemStack stack) {
        if (stack.isEmpty()) {
            throw new IllegalArgumentException("An automatic-build material key cannot be empty");
        }
        return new AutoBuildItemKey(stack);
    }

    /**
     * Returns a defensive, single-item prototype suitable for placement or display.
     */
    public ItemStack prototype() {
        return prototype.copy();
    }

    /**
     * Tests a stack using the exact item-and-components semantics used by material transactions.
     */
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && ItemStack.isSameItemSameComponents(prototype, stack);
    }

    @Override
    public boolean equals(Object object) {
        return this == object || object instanceof AutoBuildItemKey other &&
                ItemStack.isSameItemSameComponents(prototype, other.prototype);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return prototype.toString();
    }
}
