package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError;
import com.gregtechceu.gtceu.client.renderer.StructureIssueOverlay;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Reads confirmed diagnostics only; looking at a controller never starts a structure scan. */
public class MultiblockStructureProvider extends MachineInfoProvider<MultiblockControllerMachine, CompoundTag> {

    public MultiblockStructureProvider() {
        super(GTCEu.id("multiblock_structure"), MultiblockControllerMachine.class);
    }

    @Override
    protected CompoundTag write(MultiblockControllerMachine machine) {
        CompoundTag data = new CompoundTag();
        var checks = MultiblockWorldSavedData.getOrCreate((ServerLevel) machine.getLevel());
        ListTag structures = new ListTag();
        for (String name : machine.getDefinition().getStructureOrder()) {
            structures.add(StringTag.valueOf(name));
            String key = name + "/";
            var state = machine.getMultiblockState(name);
            boolean formed = machine.isStructureFormed(name);
            data.putBoolean(key + "formed", formed);
            data.putBoolean(key + "checking", checks.isChecking(state));
            if (formed) continue;
            var result = state.getMatchResult();
            if (result != null && !result.getDiagnostics().isEmpty()) {
                var issue = result.getDiagnostics().getFirst();
                data.putString(key + "expected", issue.getExpected());
                data.putString(key + "path", issue.getPath());
                data.putInt(key + "difference", issue.getDifference());
                data.putString(key + "fact", issue.getMissingFact());
                if (issue.getActual() != null)
                    data.putString(key + "actual", issue.getActual().getBlock().getDescriptionId());
                data.putLong(key + "position", issue.getWorld().asLong());
            } else if (state.error instanceof PatternStringError error) {
                data.putString(key + "error", error.translateKey);
            }
        }
        data.put("structures", structures);
        return data;
    }

    @Override
    protected void addTooltip(CompoundTag data, ITooltip tooltip, Player player, BlockAccessor accessor,
                              BlockEntity blockEntity, IPluginConfig config) {
        ListTag structures = data.getList("structures", Tag.TAG_STRING);
        boolean highlighted = false;
        for (int index = 0; index < structures.size(); index++) {
            String name = structures.getString(index);
            String key = name + "/";
            boolean formed = data.getBoolean(key + "formed");
            Component status = Component
                    .translatable(formed ? "gtpm.top.valid_structure" : "gtpm.top.invalid_structure")
                    .withStyle(formed ? ChatFormatting.GREEN : ChatFormatting.RED);
            tooltip.add(structures.size() == 1 ? status : Component.literal(name + ": ").append(status));
            if (data.getBoolean(key + "checking")) tooltip
                    .add(Component.translatable("gtpm.multiblock.diagnosis.checking").withStyle(ChatFormatting.YELLOW));
            if (formed) continue;
            if (data.contains(key + "error"))
                tooltip.add(Component.translatable(data.getString(key + "error")).withStyle(ChatFormatting.GRAY));
            if (data.contains(key + "expected")) tooltip
                    .add(Component.translatable("gtpm.multiblock.diagnosis.expected",
                            Component.translatable(data.getString(key + "expected")))
                            .withStyle(ChatFormatting.GRAY));
            if (data.contains(key + "actual")) tooltip
                    .add(Component
                            .translatable("gtpm.multiblock.diagnosis.actual",
                                    Component.translatable(data.getString(key + "actual")))
                            .withStyle(ChatFormatting.GRAY));
            if (!data.getString(key + "fact").isEmpty())
                tooltip.add(Component.translatable("gtpm.multiblock.diagnosis.difference", data.getString(key + "fact"),
                        data.getInt(key + "difference")));
            if (player.isShiftKeyDown() && data.contains(key + "path"))
                tooltip.add(Component.literal(data.getString(key + "path")).withStyle(ChatFormatting.DARK_GRAY));
            if (!highlighted && data.contains(key + "position")) {
                long position = data.getLong(key + "position");
                tooltip.add(Component
                        .translatable("gtpm.multiblock.diagnosis.position", BlockPos.of(position).toShortString())
                        .withStyle(ChatFormatting.GRAY));
                StructureIssueOverlay.showIssue(accessor.getPosition(), position);
                highlighted = true;
            }
        }
        if (!highlighted) StructureIssueOverlay.clearIssue(accessor.getPosition());
    }
}
