package com.gregtechceu.gtceu.common.data.datacomponents;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

public record CoverConfigCopyData(Map<Direction, Entry> covers) {

    public static final ResourceLocation KEY = GTCEu.id("cover_config");

    public static final Codec<CoverConfigCopyData> CODEC = Codec.unboundedMap(Direction.CODEC, Entry.CODEC)
            .xmap(CoverConfigCopyData::new, CoverConfigCopyData::covers);

    static {
        FieldCodecs.register(CoverConfigCopyData.class, CODEC);
    }

    public CoverConfigCopyData {
        covers = Map.copyOf(covers);
    }

    public boolean isEmpty() {
        return covers.isEmpty();
    }

    public record Entry(ResourceLocation id, ItemStack attachItem, SyncFieldData config) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(Entry::id),
                ItemStack.OPTIONAL_CODEC.fieldOf("attach_item").forGetter(Entry::attachItem),
                SyncFieldData.CODEC.fieldOf("config").forGetter(Entry::config))
                .apply(instance, Entry::new));

        static {
            FieldCodecs.register(Entry.class, CODEC);
        }

        public Entry {
            attachItem = attachItem.copy();
        }
    }
}
