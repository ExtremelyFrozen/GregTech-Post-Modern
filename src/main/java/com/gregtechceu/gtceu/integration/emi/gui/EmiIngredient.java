package com.gregtechceu.gtceu.integration.emi.gui;

import com.gregtechceu.gtceu.api.gui.element.GTFluidSlotElement;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.emi.LDLibEMIPlugin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Client-side ingredient event bindings. Load only after confirming EMI is available. */
@ApiStatus.Internal
@NullMarked
public final class EmiIngredient {

    private EmiIngredient() {}

    /** Registers fluid ghost drops through the GT setter, preserving components and phantom writeback. */
    public static void ghostFluid(GTFluidSlotElement element) {
        LDLibEMIPlugin.renderDragHandler(element, dragged -> dragged instanceof FluidEmiStack);
        LDLibEMIPlugin.dropStackHandler(element,
                dragged -> dragged instanceof FluidEmiStack,
                dragged -> {
                    if (dragged instanceof FluidEmiStack fluid) {
                        element.setFluid(new FluidStack(
                                BuiltInRegistries.FLUID.wrapAsHolder((Fluid) fluid.getKey()),
                                Math.max(1000, (int) fluid.getAmount()), fluid.getComponentChanges()));
                    }
                });
    }

    /** Supplies the current fluid alternatives when EMI collects recipe ingredients. */
    public static void recipeFluidIngredient(GTFluidSlotElement element, IngredientIO io,
                                             Supplier<Stream<FluidStack>> fluids) {
        LDLibEMIPlugin.recipeIngredient(element, io, () -> fluids.get()
                .map(fluid -> EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()))
                .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll));
    }

    /** Binds current alternatives, amount and chance suppliers to a fluid recipe slot. */
    public static void recipeFluidSlot(GTFluidSlotElement element, Supplier<Float> chance, IntSupplier amount,
                                       Supplier<Stream<FluidStack>> fluids) {
        LDLibEMIPlugin.recipeSlot(element, () -> new ListEmiIngredient(
                fluids.get()
                        .map(fluid -> EmiStack.of(fluid.getFluid(), fluid.getComponentsPatch(), fluid.getAmount()))
                        .map(stack -> stack.setChance(chance.get()))
                        .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll),
                amount.getAsInt()).setChance(chance.get()));
    }

    /** Binds a read-only item viewport to its current hovered item. */
    public static void recipeItemSlot(UIElement element, Supplier<ItemStack> hovered) {
        LDLibEMIPlugin.recipeSlot(element, () -> EmiStack.of(hovered.get()));
    }
}
