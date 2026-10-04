package com.gregtechceu.gtceu.data.pattern;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.dsl.PatternBuilder;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternDefinition;
import com.gregtechceu.gtceu.api.multiblock.pattern.model.PatternNode;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/** Resolves canonical definitions into the single runtime matcher used by all consumers. */
@NullMarked
public final class StructurePatternResolver {

    private StructurePatternResolver() {}

    public static PatternBuilder appendDefinition(PatternBuilder builder, StructurePatternKey key) {
        PatternDefinition definition = loadDefinition(key);
        PatternDefinitionAdapter.appendToBuilder(builder, definition);
        return builder;
    }

    public static PatternDefinition loadDefinition(StructurePatternKey key) {
        PatternDefinition definition = StructureCache.getPatternDefinition(key);
        if (definition == null) throw new IllegalStateException("Pattern definition for " + key + " was not found");
        return definition;
    }

    public static MultiBlockPattern rebuildPattern(MultiblockMachineDefinition owner, StructurePatternKey key,
                                                   MultiBlockPattern baseline, PatternDefinition definition) {
        return PatternDefinitionAdapter.compile(owner, key, baseline, definition);
    }

    public static MultiBlockPattern rebuildRuntimePattern(MultiblockMachineDefinition owner, StructurePatternKey key,
                                                          MultiBlockPattern baseline, List<PatternNode> nodes) {
        PatternDefinition source = loadDefinition(key);
        PatternDefinition runtime = PatternDefinitionAdapter.withBody(source, nodes);
        return PatternDefinitionAdapter.compile(owner, key, baseline, runtime);
    }
}
