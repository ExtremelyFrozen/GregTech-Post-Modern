package com.gregtechceu.gtceu.common.item.terminal.profile;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.plan.MultiblockPlanResolver;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalMachineProfile;
import com.gregtechceu.gtceu.common.item.datacomponents.TerminalStructureProfile;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One bounded, typed terminal profile mutation sent to the authoritative server menu.
 */
public record TerminalProfileUpdate(Type type, @Nullable String structureName, int repetitionIndex,
                                    int repetitionValue, boolean booleanValue, @Nullable AutoBuildMode mode,
                                    @Nullable ResourceLocation tierGroup, @Nullable ResourceLocation tierChoice) {

    private static final int MAX_STRUCTURE_NAME = 128;
    private static final int MAX_RESOURCE_LOCATION = 256;

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalProfileUpdate> STREAM_CODEC = StreamCodec
            .of(TerminalProfileUpdate::encode, TerminalProfileUpdate::decode);

    public TerminalProfileUpdate {
        boolean structureAction = switch (type) {
            case STRUCTURE_SELECTED, STRUCTURE_MODE, STRUCTURE_FLIP, REPETITION, TIER -> true;
            default -> false;
        };
        if (structureAction && (structureName == null || structureName.isBlank() ||
                structureName.length() > MAX_STRUCTURE_NAME)) {
            throw new IllegalArgumentException("A bounded structure name is required for " + type);
        }
        if (type == Type.STRUCTURE_MODE && mode == null) {
            throw new IllegalArgumentException("A structure mode is required");
        }
        if (type == Type.REPETITION && repetitionIndex < 0) {
            throw new IllegalArgumentException("Repetition index cannot be negative");
        }
        if (type == Type.TIER && (tierGroup == null || tierChoice == null)) {
            throw new IllegalArgumentException("Tier group and choice are required");
        }
        if (tierGroup != null && tierChoice != null &&
                (tierGroup.toString().length() > MAX_RESOURCE_LOCATION ||
                        tierChoice.toString().length() > MAX_RESOURCE_LOCATION)) {
            throw new IllegalArgumentException("Tier group and choice must be bounded");
        }
    }

    public static TerminalProfileUpdate sharedReplace(boolean value) {
        return booleanUpdate(Type.SHARED_REPLACE, null, value);
    }

    public static TerminalProfileUpdate sharedNoHatch(boolean value) {
        return booleanUpdate(Type.SHARED_NO_HATCH, null, value);
    }

    public static TerminalProfileUpdate sharedME(boolean value) {
        return booleanUpdate(Type.SHARED_USE_ME, null, value);
    }

    public static TerminalProfileUpdate structureSelected(String name, boolean value) {
        return booleanUpdate(Type.STRUCTURE_SELECTED, name, value);
    }

    public static TerminalProfileUpdate structureMode(String name, AutoBuildMode mode) {
        return new TerminalProfileUpdate(Type.STRUCTURE_MODE, name, 0, 0, false, mode, null, null);
    }

    public static TerminalProfileUpdate structureFlip(String name, boolean value) {
        return booleanUpdate(Type.STRUCTURE_FLIP, name, value);
    }

    public static TerminalProfileUpdate repetition(String name, int index, int value) {
        return new TerminalProfileUpdate(Type.REPETITION, name, index, value, false, null, null, null);
    }

    public static TerminalProfileUpdate tier(String name, ResourceLocation group, ResourceLocation choice) {
        return new TerminalProfileUpdate(Type.TIER, name, 0, 0, false, null, group, choice);
    }

    /** Applies this change only after validating structure identity, dependency rules and repetition bounds. */
    public TerminalMachineProfile apply(MultiblockMachineDefinition definition, TerminalMachineProfile profile) {
        return switch (type) {
            case SHARED_REPLACE -> profile.withSharedOptions(new AutoBuildSharedOptions(booleanValue,
                    profile.sharedOptions().noHatchMode(), profile.sharedOptions().useME()));
            case SHARED_NO_HATCH -> profile.withSharedOptions(new AutoBuildSharedOptions(
                    profile.sharedOptions().replaceMode(), booleanValue, profile.sharedOptions().useME()));
            case SHARED_USE_ME -> profile.withSharedOptions(new AutoBuildSharedOptions(
                    profile.sharedOptions().replaceMode(), profile.sharedOptions().noHatchMode(), booleanValue));
            case STRUCTURE_SELECTED -> selectStructure(definition, profile);
            case STRUCTURE_MODE -> changeMode(definition, profile);
            case STRUCTURE_FLIP -> changeFlip(definition, profile);
            case REPETITION -> changeRepetition(definition, profile);
            case TIER -> changeTier(definition, profile);
        };
    }

    private TerminalMachineProfile selectStructure(MultiblockMachineDefinition definition,
                                                   TerminalMachineProfile profile) {
        TerminalStructureProfile structure = requireStructure(profile);
        if (!booleanValue) {
            for (var entry : profile.structures().entrySet()) {
                if (entry.getValue().selected() && entry.getValue().mode() == AutoBuildMode.BUILD &&
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
        TerminalMachineProfile updated = updateStructure(definition, profile, structure.withSelected(true));
        return structure.mode() == AutoBuildMode.BUILD ? selectDependencies(definition, updated) : updated;
    }

    private TerminalMachineProfile changeMode(MultiblockMachineDefinition definition,
                                              TerminalMachineProfile profile) {
        TerminalStructureProfile structure = requireStructure(profile);
        if (mode == AutoBuildMode.DEMOLISH) {
            for (var entry : profile.structures().entrySet()) {
                if (entry.getValue().selected() && entry.getValue().mode() == AutoBuildMode.BUILD &&
                        definition.getRequiredStructures(entry.getKey()).contains(structureName)) {
                    throw new IllegalArgumentException("Cannot demolish required structure '" + structureName +
                            "' while building '" + entry.getKey() + "'");
                }
            }
        }
        TerminalMachineProfile updated = updateStructure(definition, profile, structure.withMode(mode));
        return mode == AutoBuildMode.BUILD && structure.selected() ? selectDependencies(definition, updated) : updated;
    }

    private TerminalMachineProfile selectDependencies(MultiblockMachineDefinition definition,
                                                      TerminalMachineProfile profile) {
        TerminalMachineProfile updated = profile;
        for (String dependency : definition.getRequiredStructures(structureName)) {
            TerminalStructureProfile dependencyProfile = updated.structures().get(dependency);
            if (dependencyProfile == null) {
                throw new IllegalArgumentException("Missing required terminal structure profile: " + dependency);
            }
            updated = updated.withStructure(dependency,
                    dependencyProfile.withMode(AutoBuildMode.BUILD).withSelected(true));
        }
        return updated;
    }

    private TerminalMachineProfile changeRepetition(MultiblockMachineDefinition definition,
                                                    TerminalMachineProfile profile) {
        TerminalStructureProfile structure = requireStructure(profile);
        MultiBlockPattern pattern = definition.getPattern(structureName);
        if (repetitionIndex >= pattern.aisleRepetitions.length) {
            throw new IllegalArgumentException("Unknown repetition unit " + repetitionIndex + " for " +
                    structureName);
        }
        int[] limits = pattern.aisleRepetitions[repetitionIndex];
        if (repetitionValue < limits[0] || repetitionValue > limits[1]) {
            throw new IllegalArgumentException("Repetition value is outside pattern limits");
        }
        List<Integer> repetitions = new ArrayList<>(structure.repetitions());
        if (repetitions.size() != pattern.aisleRepetitions.length) {
            throw new IllegalArgumentException("Stored repetition vector no longer matches the pattern");
        }
        repetitions.set(repetitionIndex, repetitionValue);
        return updateStructure(definition, profile, structure.withRepetitions(repetitions));
    }

    private TerminalMachineProfile changeFlip(MultiblockMachineDefinition definition,
                                              TerminalMachineProfile profile) {
        if (booleanValue && !definition.isAllowFlip()) {
            throw new IllegalArgumentException("This multiblock definition does not allow flipped structures");
        }
        return updateStructure(definition, profile, requireStructure(profile).withFlipMode(booleanValue));
    }

    private TerminalMachineProfile changeTier(MultiblockMachineDefinition definition,
                                              TerminalMachineProfile profile) {
        List<ResourceLocation> choices = new MultiblockPlanResolver().tierChoiceOptions(definition, structureName)
                .get(tierGroup);
        if (choices == null || !choices.contains(tierChoice)) {
            throw new IllegalArgumentException("Tier choice does not belong to this structure predicate");
        }
        return updateStructure(definition, profile,
                requireStructure(profile).withTierChoice(tierGroup, tierChoice));
    }

    private TerminalMachineProfile updateStructure(MultiblockMachineDefinition definition,
                                                   TerminalMachineProfile profile,
                                                   TerminalStructureProfile structure) {
        if (!definition.getStructureNames().contains(structureName)) {
            throw new IllegalArgumentException("Unknown terminal structure: " + structureName);
        }
        return profile.withStructure(structureName, structure);
    }

    private TerminalStructureProfile requireStructure(TerminalMachineProfile profile) {
        TerminalStructureProfile structure = profile.structures().get(structureName);
        if (structure == null) {
            throw new IllegalArgumentException("Unknown terminal structure profile: " + structureName);
        }
        return structure;
    }

    private static TerminalProfileUpdate booleanUpdate(Type type, @Nullable String structureName, boolean value) {
        return new TerminalProfileUpdate(type, structureName, 0, 0, value, null, null, null);
    }

    private static void encode(RegistryFriendlyByteBuf buffer, TerminalProfileUpdate update) {
        buffer.writeEnum(update.type);
        switch (update.type) {
            case SHARED_REPLACE, SHARED_NO_HATCH, SHARED_USE_ME -> buffer.writeBoolean(update.booleanValue);
            case STRUCTURE_SELECTED, STRUCTURE_FLIP -> {
                buffer.writeUtf(update.structureName, MAX_STRUCTURE_NAME);
                buffer.writeBoolean(update.booleanValue);
            }
            case STRUCTURE_MODE -> {
                buffer.writeUtf(update.structureName, MAX_STRUCTURE_NAME);
                buffer.writeEnum(update.mode);
            }
            case REPETITION -> {
                buffer.writeUtf(update.structureName, MAX_STRUCTURE_NAME);
                buffer.writeVarInt(update.repetitionIndex);
                buffer.writeVarInt(update.repetitionValue);
            }
            case TIER -> {
                buffer.writeUtf(update.structureName, MAX_STRUCTURE_NAME);
                writeResourceLocation(buffer, update.tierGroup);
                writeResourceLocation(buffer, update.tierChoice);
            }
        }
    }

    private static TerminalProfileUpdate decode(RegistryFriendlyByteBuf buffer) {
        Type type = buffer.readEnum(Type.class);
        return switch (type) {
            case SHARED_REPLACE, SHARED_NO_HATCH, SHARED_USE_ME -> booleanUpdate(type, null, buffer.readBoolean());
            case STRUCTURE_SELECTED, STRUCTURE_FLIP -> booleanUpdate(type, buffer.readUtf(MAX_STRUCTURE_NAME),
                    buffer.readBoolean());
            case STRUCTURE_MODE -> structureMode(buffer.readUtf(MAX_STRUCTURE_NAME),
                    buffer.readEnum(AutoBuildMode.class));
            case REPETITION -> repetition(buffer.readUtf(MAX_STRUCTURE_NAME), buffer.readVarInt(),
                    buffer.readVarInt());
            case TIER -> tier(buffer.readUtf(MAX_STRUCTURE_NAME), readResourceLocation(buffer),
                    readResourceLocation(buffer));
        };
    }

    private static void writeResourceLocation(RegistryFriendlyByteBuf buffer, ResourceLocation value) {
        buffer.writeUtf(value.toString(), MAX_RESOURCE_LOCATION);
    }

    private static ResourceLocation readResourceLocation(RegistryFriendlyByteBuf buffer) {
        return ResourceLocation.parse(buffer.readUtf(MAX_RESOURCE_LOCATION));
    }

    public enum Type {
        SHARED_REPLACE,
        SHARED_NO_HATCH,
        SHARED_USE_ME,
        STRUCTURE_SELECTED,
        STRUCTURE_MODE,
        STRUCTURE_FLIP,
        REPETITION,
        TIER
    }
}
