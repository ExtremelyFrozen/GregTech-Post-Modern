package com.gregtechceu.gtceu.common.item.terminal.profile;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.BuildPolicy;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureOperation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.StructureRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.BatchPlanner;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachine;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalStructure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

/**
 * One typed terminal profile mutation sent to the authoritative server menu.
 */
public record TerminalChange(Type type, @Nullable String structureName, int repetitionIndex,
                             int repetitionValue, boolean booleanValue, @Nullable StructureOperation mode,
                             @Nullable String tierGroup, @Nullable ResourceLocation tierChoice,
                             @Nullable String choicePath, @Nullable String choiceBranch) {

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalChange> STREAM_CODEC = StreamCodec
            .of(TerminalChange::encode, TerminalChange::decode);

    public static TerminalChange sharedReplace(boolean value) {
        return booleanUpdate(Type.SHARED_REPLACE, null, value);
    }

    public static TerminalChange sharedNoHatch(boolean value) {
        return booleanUpdate(Type.SHARED_NO_HATCH, null, value);
    }

    public static TerminalChange sharedME(boolean value) {
        return booleanUpdate(Type.SHARED_USE_ME, null, value);
    }

    public static TerminalChange structureSelected(String name, boolean value) {
        return booleanUpdate(Type.STRUCTURE_SELECTED, name, value);
    }

    public static TerminalChange structureMode(String name, StructureOperation mode) {
        return new TerminalChange(Type.STRUCTURE_MODE, name, 0, 0, false, mode, null, null, null, null);
    }

    public static TerminalChange structureFlip(String name, boolean value) {
        return booleanUpdate(Type.STRUCTURE_FLIP, name, value);
    }

    public static TerminalChange repetition(String name, int index, int value) {
        return new TerminalChange(Type.REPETITION, name, index, value, false, null, null, null, null, null);
    }

    public static TerminalChange tier(String name, String group, ResourceLocation choice) {
        return new TerminalChange(Type.TIER, name, 0, 0, false, null, group, choice, null, null);
    }

    public static TerminalChange choice(String name, String path, String branch) {
        return new TerminalChange(Type.CHOICE, name, 0, 0, false, null, null, null, path, branch);
    }

    /** Applies this change only after validating structure identity, dependency rules and repetition bounds. */
    public TerminalMachine apply(MultiblockMachineDefinition definition, TerminalMachine profile) {
        return switch (type) {
            case SHARED_REPLACE -> profile.withSharedOptions(new BuildPolicy(booleanValue,
                    profile.sharedOptions().noHatchMode(), profile.sharedOptions().useME()));
            case SHARED_NO_HATCH -> profile.withSharedOptions(new BuildPolicy(
                    profile.sharedOptions().replaceMode(), booleanValue, profile.sharedOptions().useME()));
            case SHARED_USE_ME -> profile.withSharedOptions(new BuildPolicy(
                    profile.sharedOptions().replaceMode(), profile.sharedOptions().noHatchMode(), booleanValue));
            case STRUCTURE_SELECTED -> selectStructure(definition, profile);
            case STRUCTURE_MODE -> changeMode(definition, profile);
            case STRUCTURE_FLIP -> changeFlip(definition, profile);
            case REPETITION -> changeRepetition(definition, profile);
            case TIER -> changeTier(definition, profile);
            case CHOICE -> changeChoice(definition, profile);
        };
    }

    private TerminalMachine selectStructure(MultiblockMachineDefinition definition,
                                            TerminalMachine profile) {
        TerminalStructure structure = requireStructure(profile);
        if (!booleanValue) {
            for (var entry : profile.structures().entrySet()) {
                if (entry.getValue().selected() && entry.getValue().mode() == StructureOperation.BUILD &&
                        definition.getRequiredStructures(entry.getKey()).contains(structureName)) {
                    throw new IllegalArgumentException("Selected build structure '" + entry.getKey() +
                            "' requires '" + structureName + "'");
                }
            }
            long remaining = profile.structures().entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(structureName) && entry.getValue().selected()).count();
            if (remaining == 0) {
                throw new IllegalArgumentException("A terminal profile must select at least one structure");
            }
            return updateStructure(definition, profile, structure.withSelected(false));
        }
        TerminalMachine updated = updateStructure(definition, profile, structure.withSelected(true));
        return structure.mode() == StructureOperation.BUILD ? selectDependencies(definition, updated) : updated;
    }

    private TerminalMachine changeMode(MultiblockMachineDefinition definition,
                                       TerminalMachine profile) {
        TerminalStructure structure = requireStructure(profile);
        if (mode == StructureOperation.DEMOLISH) {
            for (var entry : profile.structures().entrySet()) {
                if (entry.getValue().selected() && entry.getValue().mode() == StructureOperation.BUILD &&
                        definition.getRequiredStructures(entry.getKey()).contains(structureName)) {
                    throw new IllegalArgumentException("Cannot demolish required structure '" + structureName +
                            "' while building '" + entry.getKey() + "'");
                }
            }
        }
        TerminalMachine updated = updateStructure(definition, profile, structure.withMode(mode));
        return mode == StructureOperation.BUILD && structure.selected() ? selectDependencies(definition, updated) :
                updated;
    }

    private TerminalMachine selectDependencies(MultiblockMachineDefinition definition,
                                               TerminalMachine profile) {
        TerminalMachine updated = profile;
        for (String dependency : definition.getRequiredStructures(structureName)) {
            TerminalStructure dependencyProfile = updated.structures().get(dependency);
            if (dependencyProfile == null) {
                throw new IllegalArgumentException("Missing required terminal structure profile: " + dependency);
            }
            updated = updated.withStructure(dependency,
                    dependencyProfile.withMode(StructureOperation.BUILD).withSelected(true));
        }
        return updated;
    }

    private TerminalMachine changeRepetition(MultiblockMachineDefinition definition,
                                             TerminalMachine profile) {
        TerminalStructure structure = requireStructure(profile);
        MultiBlockPattern pattern = definition.getPattern(structureName);
        var descriptors = pattern.getRepeatDescriptors();
        if (repetitionIndex < 0 || repetitionIndex >= descriptors.size()) {
            throw new IllegalArgumentException("Unknown repetition unit " + repetitionIndex + " for " +
                    structureName);
        }
        var limits = descriptors.get(repetitionIndex);
        if (repetitionValue < limits.minimum() || repetitionValue > limits.maximum()) {
            throw new IllegalArgumentException("Repetition value is outside pattern limits");
        }
        IntList repetitions = new IntArrayList(structure.repetitions());
        if (repetitions.size() != descriptors.size()) {
            throw new IllegalArgumentException("Stored repetition vector no longer matches the pattern");
        }
        repetitions.set(repetitionIndex, repetitionValue);
        return updateStructure(definition, profile,
                normalizeLayoutTiers(definition, structure.withRepetitions(repetitions)));
    }

    private TerminalMachine changeFlip(MultiblockMachineDefinition definition,
                                       TerminalMachine profile) {
        if (booleanValue && !definition.isAllowFlip()) {
            throw new IllegalArgumentException("This multiblock definition does not allow flipped structures");
        }
        return updateStructure(definition, profile, requireStructure(profile).withFlipMode(booleanValue));
    }

    private TerminalMachine changeTier(MultiblockMachineDefinition definition,
                                       TerminalMachine profile) {
        ObjectList<ResourceLocation> choices = new BatchPlanner().tierChoiceOptions(definition, structureName)
                .get(tierGroup);
        if (choices == null || !choices.contains(tierChoice)) {
            throw new IllegalArgumentException("Tier choice does not belong to this structure predicate");
        }
        return updateStructure(definition, profile,
                requireStructure(profile).withTierChoice(tierGroup, tierChoice));
    }

    private TerminalMachine changeChoice(MultiblockMachineDefinition definition,
                                         TerminalMachine profile) {
        boolean valid = definition.getPattern(structureName).getChoiceDescriptors().stream()
                .anyMatch(choice -> choice.path().equals(choicePath) && choice.alternatives().contains(choiceBranch));
        if (!valid) throw new IllegalArgumentException("Choice branch does not belong to this structure node");
        return updateStructure(definition, profile,
                normalizeLayoutTiers(definition, requireStructure(profile).withChoice(choicePath, choiceBranch)));
    }

    private TerminalStructure normalizeLayoutTiers(MultiblockMachineDefinition definition,
                                                   TerminalStructure structure) {
        var options = new StructureRequest(structureName, structure.repetitions(), structure.tierChoices(),
                structure.flipMode(), structure.mode(), structure.choices());
        var domains = new BatchPlanner().activeTierChoiceOptions(definition, options);
        TerminalStructure normalized = structure;
        for (var entry : domains.entrySet()) {
            if (!entry.getValue().contains(structure.tierChoices().get(entry.getKey()))) {
                normalized = normalized.withTierChoice(entry.getKey(), entry.getValue().getFirst());
            }
        }
        return normalized;
    }

    private TerminalMachine updateStructure(MultiblockMachineDefinition definition,
                                            TerminalMachine profile,
                                            TerminalStructure structure) {
        if (!definition.getStructureNames().contains(structureName)) {
            throw new IllegalArgumentException("Unknown terminal structure: " + structureName);
        }
        return profile.withStructure(structureName, structure);
    }

    private TerminalStructure requireStructure(TerminalMachine profile) {
        TerminalStructure structure = profile.structures().get(structureName);
        if (structure == null) {
            throw new IllegalArgumentException("Unknown terminal structure profile: " + structureName);
        }
        return structure;
    }

    private static TerminalChange booleanUpdate(Type type, @Nullable String structureName, boolean value) {
        return new TerminalChange(type, structureName, 0, 0, value, null, null, null, null, null);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalChange update) {
        buffer.writeEnum(update.type);
        switch (update.type) {
            case SHARED_REPLACE, SHARED_NO_HATCH, SHARED_USE_ME -> buffer.writeBoolean(update.booleanValue);
            case STRUCTURE_SELECTED, STRUCTURE_FLIP -> {
                buffer.writeUtf(update.structureName);
                buffer.writeBoolean(update.booleanValue);
            }
            case STRUCTURE_MODE -> {
                buffer.writeUtf(update.structureName);
                buffer.writeEnum(update.mode);
            }
            case REPETITION -> {
                buffer.writeUtf(update.structureName);
                buffer.writeVarInt(update.repetitionIndex);
                buffer.writeVarInt(update.repetitionValue);
            }
            case TIER -> {
                buffer.writeUtf(update.structureName);
                buffer.writeUtf(update.tierGroup);
                buffer.writeResourceLocation(update.tierChoice);
            }
            case CHOICE -> {
                buffer.writeUtf(update.structureName);
                buffer.writeUtf(update.choicePath);
                buffer.writeUtf(update.choiceBranch);
            }
        }
    }

    private static TerminalChange decode(RegistryFriendlyByteBuf buffer) {
        Type type = buffer.readEnum(Type.class);
        return switch (type) {
            case SHARED_REPLACE, SHARED_NO_HATCH, SHARED_USE_ME -> booleanUpdate(type, null, buffer.readBoolean());
            case STRUCTURE_SELECTED, STRUCTURE_FLIP -> booleanUpdate(type, buffer.readUtf(),
                    buffer.readBoolean());
            case STRUCTURE_MODE -> structureMode(buffer.readUtf(),
                    buffer.readEnum(StructureOperation.class));
            case REPETITION -> repetition(buffer.readUtf(), buffer.readVarInt(),
                    buffer.readVarInt());
            case TIER -> tier(buffer.readUtf(), buffer.readUtf(), buffer.readResourceLocation());
            case CHOICE -> choice(buffer.readUtf(), buffer.readUtf(), buffer.readUtf());
        };
    }

    public enum Type {
        SHARED_REPLACE,
        SHARED_NO_HATCH,
        SHARED_USE_ME,
        STRUCTURE_SELECTED,
        STRUCTURE_MODE,
        STRUCTURE_FLIP,
        REPETITION,
        TIER,
        CHOICE
    }
}
