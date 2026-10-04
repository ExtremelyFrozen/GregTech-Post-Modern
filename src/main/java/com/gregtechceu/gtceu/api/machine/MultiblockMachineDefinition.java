package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.data.pattern.StructurePatternRegistry;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.function.TriFunction;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public class MultiblockMachineDefinition extends MachineDefinition {

    @Getter
    @Setter
    private boolean generator;
    private final Map<String, Function<MultiblockMachineDefinition, MultiBlockPattern>> patternFactories = new LinkedHashMap<>();
    private final Map<String, MultiBlockPattern> patterns = new LinkedHashMap<>();
    private Map<String, List<String>> structureDependencies = Map.of();
    /**
     * Set this to false only if your multiblock is set up such that it could have a wall-shared controller.
     */
    @Getter
    @Setter
    private boolean allowFlip;
    @Getter
    @Setter
    private boolean renderXEIPreview;
    @Setter
    @Getter
    @Nullable
    private Supplier<ItemStack[]> recoveryItems;
    @Setter
    @Getter
    private Function<MultiblockControllerMachine, Comparator<IMultiPart>> partSorter;
    @Getter
    @Setter
    private TriFunction<MultiblockControllerMachine, IMultiPart, Direction, BlockState> partAppearance;
    @Getter
    @Setter
    private BiConsumer<MultiblockControllerMachine, List<Component>> additionalDisplay;

    public MultiblockMachineDefinition(ResourceLocation id) {
        super(id);
    }

    public void setPatternFactory(String structureName,
                                  Function<MultiblockMachineDefinition, MultiBlockPattern> patternFactory) {
        structureName = validateStructureName(structureName);
        this.patternFactories.put(structureName, patternFactory);
        StructurePatternRegistry.registerJavaDefinition(this, structureName);
    }

    public MultiBlockPattern getPattern(String structureName) {
        structureName = validateStructureName(structureName);
        requirePatternFactory(structureName);
        synchronized (this.patterns) {
            MultiBlockPattern pattern = this.patterns.get(structureName);
            if (pattern == null) {
                pattern = StructurePatternRegistry.resolvePattern(this, structureName);
                patterns.put(structureName, pattern);
            }
            return pattern;
        }
    }

    public void reloadPattern(String structureName) {
        structureName = validateStructureName(structureName);
        requirePatternFactory(structureName);
        synchronized (this.patterns) {
            patterns.put(structureName, StructurePatternRegistry.resolvePattern(this, structureName));
        }
    }

    public MultiBlockPattern createJavaPattern(String structureName) {
        structureName = validateStructureName(structureName);
        return requirePatternFactory(structureName).apply(this);
    }

    public Set<String> getStructureNames() {
        return Collections.unmodifiableSet(this.patternFactories.keySet());
    }

    /**
     * Returns the stable definition order used by batch building and demolition.
     */
    public List<String> getStructureOrder() {
        ArrayList<String> order = new ArrayList<>(patternFactories.size());
        if (patternFactories.containsKey(MultiblockControllerMachine.DEFAULT_STRUCTURE)) {
            order.add(MultiblockControllerMachine.DEFAULT_STRUCTURE);
        }
        patternFactories.keySet().stream()
                .filter(name -> !MultiblockControllerMachine.DEFAULT_STRUCTURE.equals(name))
                .forEach(order::add);
        return List.copyOf(order);
    }

    /**
     * Installs and validates direct structure dependencies after all patterns have been registered.
     */
    public void setStructureDependencies(Map<String, List<String>> dependencies) {
        for (String structureName : dependencies.keySet()) {
            if (!patternFactories.containsKey(structureName)) {
                throw new IllegalArgumentException("Unknown structure dependency owner '" + structureName +
                        "' in " + getId());
            }
        }
        LinkedHashMap<String, List<String>> validated = new LinkedHashMap<>();
        for (String structureName : getStructureOrder()) {
            List<String> required = List.copyOf(dependencies.getOrDefault(structureName, List.of()));
            if (new HashSet<>(required).size() != required.size()) {
                throw new IllegalArgumentException("Duplicate required structure for '" + structureName + "' in " +
                        getId());
            }
            for (String dependency : required) {
                if (!patternFactories.containsKey(dependency)) {
                    throw new IllegalArgumentException("Unknown required structure '" + dependency + "' for '" +
                            structureName + "' in " + getId());
                }
                if (structureName.equals(dependency)) {
                    throw new IllegalArgumentException("Structure '" + structureName + "' cannot require itself in " +
                            getId());
                }
            }
            validated.put(structureName, required);
        }
        validateDependencyGraph(validated);
        structureDependencies = Collections.unmodifiableMap(validated);
    }

    /**
     * Returns direct dependencies in their declaration order.
     */
    public List<String> getStructureDependencies(String structureName) {
        requirePatternFactory(validateStructureName(structureName));
        return structureDependencies.getOrDefault(structureName, List.of());
    }

    /**
     * Returns the transitive dependency closure in machine definition order.
     */
    public List<String> getRequiredStructures(String structureName) {
        requirePatternFactory(validateStructureName(structureName));
        LinkedHashSet<String> required = new LinkedHashSet<>();
        collectDependencies(structureName, required);
        return getStructureOrder().stream().filter(required::contains).toList();
    }

    private Function<MultiblockMachineDefinition, MultiBlockPattern> requirePatternFactory(String structureName) {
        Function<MultiblockMachineDefinition, MultiBlockPattern> factory = this.patternFactories.get(structureName);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown multiblock structure '" + structureName + "' for " + getId());
        }
        return factory;
    }

    private static String validateStructureName(String structureName) {
        if (structureName.isBlank()) {
            throw new IllegalArgumentException("structureName must not be blank");
        }
        return structureName;
    }

    private void collectDependencies(String structureName, Set<String> required) {
        for (String dependency : structureDependencies.getOrDefault(structureName, List.of())) {
            if (required.add(dependency)) {
                collectDependencies(dependency, required);
            }
        }
    }

    private void validateDependencyGraph(Map<String, List<String>> dependencies) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String structureName : getStructureOrder()) {
            validateDependencyNode(structureName, dependencies, visiting, visited);
        }
    }

    private void validateDependencyNode(String structureName, Map<String, List<String>> dependencies,
                                        Set<String> visiting, Set<String> visited) {
        if (visited.contains(structureName)) return;
        if (!visiting.add(structureName)) {
            throw new IllegalArgumentException("Cyclic structure dependency at '" + structureName + "' in " +
                    getId());
        }
        for (String dependency : dependencies.getOrDefault(structureName, List.of())) {
            validateDependencyNode(dependency, dependencies, visiting, visited);
        }
        visiting.remove(structureName);
        visited.add(structureName);
    }
}
