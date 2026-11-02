package com.gregtechceu.gtceu.integration.xei.multiblock;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard.Resolution;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Shared recipe data. Live configuration and solved previews belong to the client thread; candidate indexing
 * may run on a recipe viewer's loading worker and never creates a UI or enumerates structure variants.
 */
@NullMarked
public final class MultiblockInfo {

    @Getter
    private final MultiblockMachineDefinition definition;
    private @Nullable TerminalMachine machine;
    private @Nullable Resolution<StructurePreview> preview;
    private volatile @Nullable Resolution<ObjectList<MaterialKey>> inputs;
    private long generation = -1;

    public MultiblockInfo(MultiblockMachineDefinition definition) {
        this.definition = definition;
    }

    public TerminalMachine getMachine() {
        if (machine == null) machine = TerminalMachine.defaults(definition);
        return machine;
    }

    public void setMachine(TerminalMachine machine) {
        this.machine = machine;
        generation = StructurePatternRegistry.generation();
        preview = null;
    }

    public Resolution<StructurePreview> resolvePreview() {
        long current = StructurePatternRegistry.generation();
        if (preview != null && preview.generation() == current) return preview;
        if (generation != current) machine = null;
        var resolved = PatternGenerationGuard.resolve(StructurePatternRegistry::generation, attempt -> {
            if (attempt > 0) machine = null;
            var plan = new BatchPlanner().resolveCanonical(definition,
                    getMachine().createRequest(definition, ObjectList.of()));
            return StructurePreview.canonical(definition.getId(), plan);
        });
        generation = resolved.generation();
        preview = resolved;
        return resolved;
    }

    public ObjectList<StructurePreview.Material> getMaterials() {
        return resolvePreview().value().materials();
    }

    /** All possible item keys for the native coarse index; exact usage is resolved only when queried. */
    public ObjectList<MaterialKey> getPotentialInputs() {
        var cached = inputs;
        if (cached != null && cached.generation() == StructurePatternRegistry.generation()) return cached.value();
        Resolution<ObjectList<MaterialKey>> resolved = PatternGenerationGuard
                .resolve(StructurePatternRegistry::generation, attempt -> {
                    var keys = new ObjectLinkedOpenHashSet<MaterialKey>();
                    for (String structure : definition.getStructureOrder()) {
                        for (var predicate : definition.getPattern(structure).getAllPredicates()) {
                            for (var candidate : predicate.getPreviewCandidates(definition)) {
                                ItemStack stack = candidate.blockInfo().getItemStackForm();
                                if (!stack.isEmpty()) keys.add(MaterialKey.of(stack));
                            }
                        }
                    }
                    return new ObjectImmutableList<>(keys);
                });
        inputs = resolved;
        return resolved.value();
    }

    public ObjectList<ItemStack> getOutputs() {
        return definition.getStructureOrder().stream().map(name -> {
            ItemStack stack = definition.asStack();
            if (!MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(name)) {
                stack.set(DataComponents.CUSTOM_NAME,
                        stack.getHoverName().copy().append(" - ").append(Component.translatable(name)));
            }
            return stack;
        }).collect(ObjectImmutableList.toList());
    }
}
