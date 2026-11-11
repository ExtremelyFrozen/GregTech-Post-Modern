package com.gregtechceu.gtceu.integration.xei.multiblock;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialKey;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BuildPolicy;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard;
import com.gregtechceu.gtceu.api.multiblock.preview.PatternGenerationGuard.Resolution;
import com.gregtechceu.gtceu.api.multiblock.preview.StructurePreview;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import lombok.Getter;
import lombok.Setter;
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
    private BuildPolicy policy = BuildPolicy.DEFAULT;
    private final Object2ObjectMap<String, StructureRequest> structures = new Object2ObjectLinkedOpenHashMap<>();
    private final ObjectSet<String> included = new ObjectLinkedOpenHashSet<>();
    private @Nullable Resolution<StructurePreview> preview;
    private volatile @Nullable Resolution<ObjectList<MaterialKey>> inputs;
    private long generation = -1;
    @Getter
    @Setter
    private boolean resuming;

    public MultiblockInfo(MultiblockMachineDefinition definition) {
        this.definition = definition;
    }

    /** Restores default choices for a newly opened recipe page. */
    public void reset() {
        generation = -1;
        preview = null;
    }

    public BuildPolicy getPolicy() {
        initialize();
        return policy;
    }

    public void setPolicy(BuildPolicy policy) {
        initialize();
        this.policy = policy;
        preview = null;
    }

    public Object2ObjectMap<String, StructureRequest> getStructures() {
        initialize();
        return structures;
    }

    public ObjectSet<String> getIncluded() {
        initialize();
        return included;
    }

    public void setStructure(StructureRequest structure) {
        initialize();
        Object2ObjectMap<String, ResourceLocation> tiers = new Object2ObjectLinkedOpenHashMap<>();
        new BatchPlanner().activeTierChoiceOptions(definition, structure).forEach((group, values) -> {
            var selected = structure.tierChoices().get(group);
            tiers.put(group, values.contains(selected) ? selected : values.getFirst());
        });
        structures.put(structure.structureName(), new StructureRequest(structure.structureName(),
                structure.repetitions(), tiers, structure.flipMode(), structure.mode(), structure.choices()));
        preview = null;
    }

    public void setIncluded(String name, boolean selected) {
        initialize();
        if (selected) {
            included.add(name);
            included.addAll(definition.getRequiredStructures(name));
        } else {
            included.remove(name);
        }
        preview = null;
    }

    private void initialize() {
        long current = StructurePatternRegistry.generation();
        if (generation == current) return;
        structures.clear();
        included.clear();
        policy = BuildPolicy.DEFAULT;
        var planner = new BatchPlanner();
        for (String name : definition.getStructureOrder()) {
            structures.put(name, planner.defaultStructureOptions(definition, name));
        }
        included.add(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        included.addAll(definition.getRequiredStructures(MultiblockControllerMachine.DEFAULT_STRUCTURE));
        generation = current;
        preview = null;
    }

    public Resolution<StructurePreview> resolvePreview() {
        long current = StructurePatternRegistry.generation();
        if (preview != null && preview.generation() == current) return preview;
        var resolved = PatternGenerationGuard.resolve(StructurePatternRegistry::generation, attempt -> {
            if (attempt > 0) generation = -1;
            initialize();
            var requests = definition.getStructureOrder().stream().filter(included::contains)
                    .map(structures::get).collect(ObjectImmutableList.toList());
            var plan = new BatchPlanner().resolveCanonical(definition,
                    new BatchRequest(requests, policy, ObjectList.of()));
            var result = StructurePreview.canonical(definition.getId(), plan);
            var materials = new ObjectArrayList<>(result.materials());
            materials.addFirst(new StructurePreview.Material(ObjectList.of(definition.asStack()),
                    1, 0, false, ObjectList.of()));
            return new StructurePreview(result.definitionId(), result.fingerprint(), result.availabilityKnown(),
                    result.structures(), result.cells(), new ObjectImmutableList<>(materials));
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
                    keys.add(MaterialKey.of(definition.asStack()));
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
