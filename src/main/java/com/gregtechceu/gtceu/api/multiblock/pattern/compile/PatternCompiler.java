package com.gregtechceu.gtceu.api.multiblock.pattern.compile;

import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternBinding;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternConstraint;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternFragment;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternParameter;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates recursive definitions and produces one canonical immutable compiler output. */
@NullMarked
public final class PatternCompiler {

    public static final int MAX_RECURSION_DEPTH = 64;

    public CompiledPattern compile(PatternDefinition definition) {
        if (definition == null) throw new PatternCompileException("Pattern definition must not be null");
        var cells = new ObjectArrayList<CompiledPattern.Cell>();
        var nodeCounts = new Object2IntOpenHashMap<String>();
        var nodeIds = new ObjectOpenHashSet<String>();
        var fragmentStack = new ArrayDeque<String>();
        int depth = validateNode(definition, definition.body(), "$", Map.of(), fragmentStack, nodeIds, nodeCounts,
                cells,
                0);
        validateConstraints(definition, nodeIds);
        return new CompiledPattern(definition, cells, nodeCounts, depth);
    }

    private int validateNode(PatternDefinition definition, PatternNode node, String path,
                             Map<String, PatternBinding> bindings, Deque<String> fragmentStack,
                             Set<String> nodeIds, Object2IntOpenHashMap<String> nodeCounts,
                             List<CompiledPattern.Cell> cells, int recursionDepth) {
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            throw new PatternCompileException("Pattern recursion depth exceeds " + MAX_RECURSION_DEPTH + " at " + path);
        }
        if (node instanceof PatternNode.Fixed fixed) {
            validateSymbols(definition, fixed, path);
            cells.add(new CompiledPattern.Cell(path, fixed, 0, 0, 0));
            nodeCounts.addTo(path, 1);
            return recursionDepth;
        }
        if (node instanceof PatternNode.Sequence sequence) {
            int maximum = recursionDepth;
            for (int index = 0; index < sequence.children().size(); index++) {
                maximum = Math.max(maximum, validateNode(definition, sequence.children().get(index),
                        path + "/sequence[" + index + "]", bindings, fragmentStack, nodeIds, nodeCounts, cells,
                        recursionDepth));
            }
            return maximum;
        }
        if (node instanceof PatternNode.Repeat repeat) {
            registerNodeId(nodeIds, repeat.id(), path);
            int maximum = validateNode(definition, repeat.body(), path + "/repeat:" + repeat.id(), bindings,
                    fragmentStack, nodeIds, nodeCounts, cells, recursionDepth);
            nodeCounts.addTo("repeat:" + repeat.id(), repeat.maximum());
            return maximum;
        }
        if (node instanceof PatternNode.Choice choice) {
            registerNodeId(nodeIds, choice.id(), path);
            int maximum = recursionDepth;
            var alternatives = new ObjectOpenHashSet<String>();
            for (int index = 0; index < choice.alternatives().size(); index++) {
                var alternative = choice.alternatives().get(index);
                if (!alternatives.add(alternative.id())) {
                    throw new PatternCompileException(
                            "Duplicate choice alternative '" + alternative.id() + "' at " + path);
                }
                maximum = Math.max(maximum, validateNode(definition, alternative.node(),
                        path + "/choice:" + choice.id() + "/" + alternative.id(), bindings, fragmentStack,
                        nodeIds, nodeCounts, cells, recursionDepth));
            }
            return maximum;
        }
        PatternNode.Fragment fragmentNode = (PatternNode.Fragment) node;
        PatternFragment fragment = definition.fragments().get(fragmentNode.id());
        if (fragment == null) {
            throw new PatternCompileException("Unknown fragment '" + fragmentNode.id() + "' at " + path);
        }
        if (fragmentStack.contains(fragmentNode.id())) {
            throw new PatternCompileException("Fragment cycle at " + path + ": " + fragmentStack);
        }
        validateBindings(fragment, fragmentNode.bindings(), path);
        fragmentStack.push(fragmentNode.id());
        int maximum = validateNode(definition, fragment.body(), path + "/fragment:" + fragmentNode.id(),
                fragmentNode.bindings(), fragmentStack, nodeIds, nodeCounts, cells, recursionDepth + 1);
        fragmentStack.pop();
        return maximum;
    }

    private static void validateSymbols(PatternDefinition definition, PatternNode.Fixed fixed, String path) {
        for (List<String> layer : fixed.layers()) {
            for (String row : layer) {
                for (int index = 0; index < row.length(); index++) {
                    char symbol = row.charAt(index);
                    if (symbol == ' ' || symbol == '#' || symbol == '@') continue;
                    if (!definition.predicates().containsKey(symbol)) {
                        throw new PatternCompileException("Unknown predicate symbol '" + symbol + "' at " + path);
                    }
                }
            }
        }
    }

    private static void validateBindings(PatternFragment fragment, Map<String, PatternBinding> bindings, String path) {
        for (Map.Entry<String, PatternParameter> parameter : fragment.parameters().entrySet()) {
            PatternBinding binding = bindings.get(parameter.getKey());
            if (binding == null) {
                throw new PatternCompileException("Missing binding '" + parameter.getKey() + "' at " + path);
            }
            if (!compatible(parameter.getValue(), binding)) {
                throw new PatternCompileException("Binding '" + parameter.getKey() + "' has the wrong type at " + path);
            }
        }
        for (String binding : bindings.keySet()) {
            if (!fragment.parameters().containsKey(binding)) {
                throw new PatternCompileException("Unknown fragment parameter '" + binding + "' at " + path);
            }
        }
    }

    private static boolean compatible(PatternParameter parameter, PatternBinding binding) {
        return parameter instanceof PatternParameter.Token && binding instanceof PatternBinding.Token ||
                parameter instanceof PatternParameter.Predicate && binding instanceof PatternBinding.Predicate ||
                parameter instanceof PatternParameter.IntegerRange integer &&
                        binding instanceof PatternBinding.IntegerValue value &&
                        value.value() >= integer.minimum() && value.value() <= integer.maximum() ||
                parameter instanceof PatternParameter.Direction && binding instanceof PatternBinding.Direction ||
                parameter instanceof PatternParameter.Fragment && binding instanceof PatternBinding.Fragment;
    }

    private static void registerNodeId(Set<String> ids, String id, String path) {
        if (!ids.add(id)) throw new PatternCompileException("Duplicate node id '" + id + "' at " + path);
    }

    private static void validateConstraints(PatternDefinition definition, Set<String> nodeIds) {
        for (PatternConstraint constraint : definition.constraints()) {
            if (constraint instanceof PatternConstraint.Count count) {
                if (count.scope() instanceof PatternConstraint.Scope.Node node && !nodeIds.contains(node.id())) {
                    throw new PatternCompileException("Unknown node constraint scope '" + node.id() + "'");
                }
                if (count.scope() instanceof PatternConstraint.Scope.Repeat repeat && !nodeIds.contains(repeat.id())) {
                    throw new PatternCompileException("Unknown repeat constraint scope '" + repeat.id() + "'");
                }
                if (count.scope() instanceof PatternConstraint.Scope.Fragment fragment &&
                        !definition.fragments().containsKey(fragment.id())) {
                    throw new PatternCompileException("Unknown fragment constraint scope '" + fragment.id() + "'");
                }
            }
        }
    }
}
