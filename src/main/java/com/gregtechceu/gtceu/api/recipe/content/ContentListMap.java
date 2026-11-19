package com.gregtechceu.gtceu.api.recipe.content;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;

import net.minecraft.network.RegistryFriendlyByteBuf;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ContentListMap {

    public static final Codec<ContentListMap> CODEC = RecipeCapability.CODEC
            .xmap(ContentListMap::new, ContentListMap::asContentMap);

    static {
        FieldCodecs.register(ContentListMap.class, CODEC);
    }

    private final Reference2ObjectArrayMap<RecipeCapability<?>, List<Content>> contentsMap;

    public ContentListMap() {
        contentsMap = new Reference2ObjectArrayMap<>();
    }

    public ContentListMap(Map<RecipeCapability<?>, List<Content>> contentsMap) {
        this.contentsMap = new Reference2ObjectArrayMap<>(contentsMap);
    }

    private ContentListMap(int capacity) {
        contentsMap = new Reference2ObjectArrayMap<>(capacity);
    }

    public static ContentListMap copyOf(Map<RecipeCapability<?>, List<Content>> contentsMap) {
        ContentListMap copy = new ContentListMap(contentsMap.size());
        Iterator<? extends Map.Entry<RecipeCapability<?>, List<Content>>> entries = contentsMap instanceof Reference2ObjectMap<RecipeCapability<?>, List<Content>> map ?
                Reference2ObjectMaps.fastIterator(map) : contentsMap.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            RecipeCapability<?> capability = entry.getKey();
            List<Content> contents = entry.getValue();
            ObjectList<Content> copied = new ObjectArrayList<>(contents.size());
            for (Content content : contents) {
                copied.add(content.copy(capability));
            }
            copy.contentsMap.put(capability, copied);
        }
        return copy;
    }

    public <T> List<Content> get(RecipeCapability<T> capability) {
        return contentsMap.get(capability);
    }

    public <T> void put(RecipeCapability<T> capability, List<Content> contents) {
        contentsMap.put(capability, contents);
    }

    public void putAll(ContentListMap other) {
        contentsMap.putAll(other.contentsMap);
    }

    public <T> List<Content> computeIfAbsent(RecipeCapability<T> capability,
                                             Function<RecipeCapability<T>, List<Content>> function) {
        Function<RecipeCapability<?>, List<Content>> mappingFunction = ignored -> function.apply(capability);
        return contentsMap.computeIfAbsent(capability, mappingFunction);
    }

    public void remove(RecipeCapability<?> capability) {
        contentsMap.remove(capability);
    }

    public void clear() {
        contentsMap.clear();
    }

    public boolean containsKey(RecipeCapability<?> capability) {
        return contentsMap.containsKey(capability);
    }

    public ReferenceSet<RecipeCapability<?>> keySet() {
        return contentsMap.keySet();
    }

    public Reference2ObjectMap.FastEntrySet<RecipeCapability<?>, List<Content>> entrySet() {
        return contentsMap.reference2ObjectEntrySet();
    }

    public <T> List<Content> getOrDefault(RecipeCapability<T> capability, List<Content> fallback) {
        return contentsMap.getOrDefault(capability, fallback);
    }

    public void forEach(BiConsumer<RecipeCapability<?>, List<Content>> consumer) {
        var entries = fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

    public <T> void add(RecipeCapability<T> capability, Content content) {
        Function<RecipeCapability<?>, List<Content>> mappingFunction = ignored -> new ObjectArrayList<>();
        contentsMap.computeIfAbsent(capability, mappingFunction).add(content);
    }

    public <T> Content getFirst(RecipeCapability<T> capability) {
        List<Content> list = contentsMap.get(capability);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    public boolean isEmpty() {
        return contentsMap.isEmpty();
    }

    public int size() {
        return contentsMap.size();
    }

    public int sizeOf(RecipeCapability<?> capability) {
        List<Content> contents = contentsMap.get(capability);
        return contents == null ? 0 : contents.size();
    }

    public ObjectIterator<Reference2ObjectMap.Entry<RecipeCapability<?>, List<Content>>> fastIterator() {
        return contentsMap.reference2ObjectEntrySet().fastIterator();
    }

    public Reference2ObjectMap<RecipeCapability<?>, List<Content>> asContentMap() {
        return contentsMap;
    }

    public void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(contentsMap.size());
        var entries = fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            RecipeCapability<?> capability = entry.getKey();
            buf.writeResourceLocation(GTRegistries.RECIPE_CAPABILITIES.getKey(capability));
            List<Content> contents = entry.getValue();
            buf.writeVarInt(contents.size());
            for (Content content : contents) {
                capability.serializer.toNetworkContent(buf, content);
            }
        }
    }

    public static ContentListMap fromNetwork(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        ContentListMap map = new ContentListMap(size);
        for (int i = 0; i < size; i++) {
            RecipeCapability<?> capability = GTRegistries.RECIPE_CAPABILITIES.get(buf.readResourceLocation());
            if (capability == null) {
                throw new IllegalArgumentException("Unknown recipe capability in recipe content map");
            }
            int contentSize = buf.readVarInt();
            ObjectList<Content> contents = new ObjectArrayList<>(contentSize);
            for (int j = 0; j < contentSize; j++) {
                contents.add(capability.serializer.fromNetworkContent(buf));
            }
            map.contentsMap.put(capability, contents);
        }
        return map;
    }

    public ContentListMap copy() {
        return copyOf(contentsMap);
    }

    public ContentListMap copy(ContentModifier modifier) {
        ContentListMap copy = new ContentListMap(contentsMap.size());
        var entries = fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            RecipeCapability<?> capability = entry.getKey();
            List<Content> contents = entry.getValue();
            ObjectList<Content> copied = new ObjectArrayList<>(contents.size());
            for (Content content : contents) {
                copied.add(content.copy(capability, modifier));
            }
            copy.contentsMap.put(capability, copied);
        }
        return copy;
    }

    public ContentListMap copyAndAppend(ContentListMap other) {
        ContentListMap copy = copy();
        copy.appendAll(other);
        return copy;
    }

    public void appendAll(ContentListMap other) {
        var entries = other.fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            RecipeCapability<?> capability = entry.getKey();
            List<Content> contents = entry.getValue();
            List<Content> list = get(capability);
            if (list == null) {
                put(capability, new ObjectArrayList<>(contents));
            } else {
                list.addAll(contents);
            }
        }
    }

    public ContentListMap copyWithModifier(ContentModifier modifier) {
        return copy(modifier);
    }

    public ContentListMap copyWithMultiplier(int multiplier) {
        return copy(ContentModifier.multiplier(multiplier));
    }

    public void multiply(int multiplier) {
        replaceContents(ContentModifier.multiplier(multiplier));
    }

    public void multiply(double multiplier) {
        replaceContents(ContentModifier.multiplier(multiplier));
    }

    private void replaceContents(ContentModifier modifier) {
        var entries = fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            RecipeCapability<?> capability = entry.getKey();
            entry.getValue().replaceAll(content -> content.copy(capability, modifier));
        }
    }

    public void forEachEntry(EntryConsumer consumer) {
        var entries = fastIterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            consumer.accept(entry.getKey(), entry.getValue());
        }
    }

    public interface EntryConsumer {

        <T> void accept(RecipeCapability<T> capability, List<Content> contents);
    }

    public interface TypedEntry {

        <T> void accept(EntryConsumer consumer);
    }

    public ObjectIterator<TypedEntry> iterator() {
        var iterator = fastIterator();
        return new ObjectIterator<>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public TypedEntry next() {
                var entry = iterator.next();
                RecipeCapability<?> capability = entry.getKey();
                List<Content> contents = entry.getValue();
                return new TypedEntry() {

                    @Override
                    public <T> void accept(EntryConsumer consumer) {
                        consumer.accept(capability, contents);
                    }
                };
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    public Reference2ObjectMap<RecipeCapability<?>, List<Content>> immutableView() {
        return Reference2ObjectMaps.unmodifiable(contentsMap);
    }
}
