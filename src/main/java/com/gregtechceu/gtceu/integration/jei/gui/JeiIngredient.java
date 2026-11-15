package com.gregtechceu.gtceu.integration.jei.gui;

import com.gregtechceu.gtceu.api.gui.element.GTFluidSlotElement;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;
import com.lowdragmc.lowdraglib2.integration.xei.jei.LDLibJEIPlugin;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.neoforge.NeoForgeTypes;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Client-side ingredient event bindings. Load only after confirming JEI is available. */
@ApiStatus.Internal
@NullMarked
public final class JeiIngredient {

    private JeiIngredient() {}

    /** Registers ghost drops through the GT fluid setter so phantom slots write back to their owner. */
    public static void ghostFluid(GTFluidSlotElement element) {
        LDLibJEIPlugin.ghostIngredient(element, NeoForgeTypes.FLUID_STACK, ingredient -> true, element::setFluid);
    }

    /** Supplies the current nonempty alternatives when JEI collects recipe ingredients. */
    public static void recipeFluidIngredient(GTFluidSlotElement element, IngredientIO io,
                                             Supplier<Stream<FluidStack>> fluids) {
        LDLibJEIPlugin.recipeIngredient(element, io, () -> fluids.get()
                .filter(fluid -> !fluid.isEmpty())
                .map(fluid -> LDLibJEIPlugin.createTypedIngredient(NeoForgeTypes.FLUID_STACK, fluid))
                .flatMap(Optional::stream)
                .collect(ObjectArrayList::new, ObjectArrayList::add, ObjectArrayList::addAll));
    }

    /** Binds recipe-slot display changes without notifying the fluid value owner. */
    public static void recipeFluidSlot(GTFluidSlotElement element, IngredientIO io,
                                       Supplier<Stream<FluidStack>> fluids) {
        element.registerValueListener(LDLibJEIPlugin.recipeSlot(element, io, NeoForgeTypes.FLUID_STACK,
                fluids, fluid -> element.setFluid(fluid, false)));
    }

    /** Binds a read-only item viewport and returns its JEI refresh callback. */
    public static Consumer<ItemStack> recipeItemSlot(UIElement element, Supplier<ItemStack> hovered) {
        return LDLibJEIPlugin.recipeSlot(element, IngredientIO.INPUT,
                VanillaTypes.ITEM_STACK, () -> Stream.of(hovered.get()), ignored -> {});
    }
}
