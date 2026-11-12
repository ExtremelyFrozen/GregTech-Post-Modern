package com.gregtechceu.gtceu.integration.jade.provider;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
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
        ListTag structures = new ListTag();
        for (String name : machine.getDefinition().getStructureOrder()) {
            structures.add(StringTag.valueOf(name));
            String key = name + "/";
            boolean formed = machine.isStructureFormed(name);
            data.putBoolean(key + "formed", formed);
            if (formed) continue;
            ListTag issue = new ListTag();
            for (Component line : MultiblockStructureText.lines(machine.getMultiblockState(name)))
                issue.add(StringTag.valueOf(Component.Serializer.toJson(line, machine.getLevel().registryAccess())));
            data.put(key + "issue", issue);
        }
        data.put("structures", structures);
        return data;
    }

    @Override
    protected void addTooltip(CompoundTag data, ITooltip tooltip, Player player, BlockAccessor accessor,
                              BlockEntity blockEntity, IPluginConfig config) {
        ListTag structures = data.getList("structures", Tag.TAG_STRING);
        for (int index = 0; index < structures.size(); index++) {
            String name = structures.getString(index);
            String key = name + "/";
            boolean formed = data.getBoolean(key + "formed");
            Component status = Component
                    .translatable(formed ? "gtpm.top.valid_structure" : "gtpm.top.invalid_structure")
                    .withStyle(formed ? ChatFormatting.GREEN : ChatFormatting.RED);
            tooltip.add(structures.size() == 1 ? status : Component.literal(name + ": ").append(status));
            if (formed) continue;
            for (Tag serialized : data.getList(key + "issue", Tag.TAG_STRING)) {
                Component line = Component.Serializer.fromJson(serialized.getAsString(),
                        accessor.getLevel().registryAccess());
                if (line != null) tooltip.add(line.copy().withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
