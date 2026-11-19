package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidTagList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemTagList;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;

import dev.emi.emi.api.stack.EmiStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NullMarked;

import java.util.function.IntFunction;

/** Caches tag members on the thread building the recipe ingredients. Each recipe owns its EMI stacks. */
@NullMarked
final class EmiTagStacks {

    private final Object2ObjectOpenHashMap<TagKey<Item>, Item[]> items = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<TagKey<Fluid>, Fluid[]> fluids = new Object2ObjectOpenHashMap<>();

    void append(ItemEntryList entries, ObjectList<? super EmiStack> target) {
        if (entries instanceof ItemTagList tags) {
            for (var entry : tags.getEntries()) {
                var patch = entry.componentPatch();
                for (Item item : membersOfItemTag(entry.tag())) {
                    var components = patch.isEmpty() ? patch :
                            PatchedDataComponentMap.fromPatch(item.components(), patch).asPatch();
                    target.add(item == Items.AIR ? EmiStack.EMPTY : EmiStack.of(item, components, entry.amount()));
                }
            }
        } else {
            for (var stack : entries.getStacks()) {
                target.add(EmiStack.of(stack));
            }
        }
    }

    void append(FluidEntryList entries, ObjectList<? super EmiStack> target) {
        if (entries instanceof FluidTagList tags) {
            for (var entry : tags.getEntries()) {
                var patch = entry.componentPatch();
                var components = patch.isEmpty() ? patch :
                        PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, patch).asPatch();
                for (Fluid fluid : membersOfFluidTag(entry.tag())) {
                    target.add(EmiStack.of(fluid, components, entry.amount()));
                }
            }
        } else {
            for (var stack : entries.getStacks()) {
                target.add(EmiStack.of(stack.getFluid(), stack.getComponentsPatch(), stack.getAmount()));
            }
        }
    }

    private Item[] membersOfItemTag(TagKey<Item> tag) {
        return items.computeIfAbsent(tag,
                (TagKey<Item> key) -> loadTagMembers(BuiltInRegistries.ITEM, key, Item[]::new));
    }

    private Fluid[] membersOfFluidTag(TagKey<Fluid> tag) {
        return fluids.computeIfAbsent(tag,
                (TagKey<Fluid> key) -> loadTagMembers(BuiltInRegistries.FLUID, key, Fluid[]::new));
    }

    private static <T> T[] loadTagMembers(Registry<T> registry, TagKey<T> tag, IntFunction<T[]> array) {
        return registry.getTag(tag).map(members -> members.stream().map(Holder::value).toArray(array))
                .orElseGet(() -> array.apply(0));
    }
}
