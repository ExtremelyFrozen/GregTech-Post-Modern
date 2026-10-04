package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildBlockMap;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildProblem;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchRequest;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildBatchResult;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildMode;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildSharedOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureOptions;
import com.gregtechceu.gtceu.api.multiblock.autobuild.batch.AutoBuildStructureResult;
import com.gregtechceu.gtceu.api.multiblock.pattern.match.MultiBlockPattern;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapts the deprecated single-structure API to the unified batch pipeline.
 */
@NullMarked
final class MultiblockAutoBuild {

    private MultiblockAutoBuild() {}

    static AutoBuildResult execute(MultiblockControllerMachine controller, ServerPlayer player,
                                   AutoBuildRequest request) {
        MultiBlockPattern pattern;
        try {
            pattern = controller.getPattern(request.structureName());
        } catch (IllegalArgumentException exception) {
            return AutoBuildResult.failed(problem(AutoBuildProblem.Type.UNKNOWN_STRUCTURE,
                    "gtpm.multiblock.autobuild.unknown_structure", request.structureName()));
        }
        AutoBuildStructureOptions structureOptions = adaptOptions(pattern, request.structureName(),
                request.options());
        if (structureOptions == null) {
            return AutoBuildResult.failed(problem(AutoBuildProblem.Type.INVALID_OPTIONS,
                    "gtpm.multiblock.autobuild.invalid_legacy_options"));
        }
        AutoBuildOptions legacy = request.options();
        AutoBuildBatchRequest batchRequest = new AutoBuildBatchRequest(List.of(structureOptions),
                new AutoBuildSharedOptions(legacy.replaceMode(), legacy.noHatchMode(), legacy.useME()),
                request.materialSources());
        AutoBuildBatchResult batchResult = controller.autoBuildBatch(player, batchRequest);
        AutoBuildStructureResult result = batchResult.structures().getFirst();
        return new AutoBuildResult(result.success(), result.placed(), result.removed(), result.success() ? 1 : 0,
                result.problems());
    }

    private static @Nullable AutoBuildStructureOptions adaptOptions(MultiBlockPattern pattern, String structureName,
                                                                    AutoBuildOptions legacy) {
        ArrayList<Integer> repetitions = new ArrayList<>(pattern.aisleRepetitions.length);
        for (int[] limits : pattern.aisleRepetitions) {
            int requested = legacy.repeatCount() == 0 ? limits[0] : legacy.repeatCount();
            if (requested < limits[0] || requested > limits[1]) return null;
            repetitions.add(requested);
        }
        LinkedHashMap<ResourceLocation, ResourceLocation> tierChoices = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> selection : legacy.tierSelections().entrySet()) {
            Block[] blocks = AutoBuildBlockMap.categoryBlocks(selection.getKey());
            int tier = selection.getValue();
            if (blocks == null || tier < 1 || tier > blocks.length) return null;
            tierChoices.put(AutoBuildBlockMap.categoryId(selection.getKey()),
                    AutoBuildBlockMap.blockId(blocks[tier - 1]));
        }
        AutoBuildMode mode = legacy.demolitionMode() ? AutoBuildMode.DEMOLISH : AutoBuildMode.BUILD;
        return new AutoBuildStructureOptions(structureName, repetitions, tierChoices, legacy.flipMode(), mode);
    }

    private static AutoBuildProblem problem(AutoBuildProblem.Type type, String translationKey, Object... args) {
        return new AutoBuildProblem(type, null, Component.translatable(translationKey, args));
    }
}
