package com.gregtechceu.gtceu.api.multiblock.pattern.model;

import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Contains an context used for storing temporary data
 * related to current check and shared between all predicates doing it
 */
public class PatternFacts {

    private final Map<String, Object> data = new Object2ObjectOpenHashMap<>();
    private final Object2IntOpenHashMap<String> counts = new Object2IntOpenHashMap<>();

    public void reset() {
        this.data.clear();
        this.counts.clear();
    }

    public void set(String key, Object value) {
        this.data.put(key, value);
    }

    public int getInt(String key) {
        return data.containsKey(key) ? (int) data.get(key) : 0;
    }

    public void increment(String key, int value) {
        set(key, getOrDefault(key, 0) + value);
    }

    /** Adds a fact occurrence to the canonical fact counters. */
    public void addFact(String fact) {
        if (fact == null || fact.isBlank()) {
            throw new IllegalArgumentException("Fact name must not be blank");
        }
        counts.addTo(fact, 1);
    }

    /** Adds all occurrences from another match without sharing mutable state. */
    public void merge(PatternFacts other) {
        other.counts.object2IntEntrySet().forEach(entry -> counts.addTo(entry.getKey(), entry.getIntValue()));
        data.putAll(other.data);
    }

    public int count(String fact) {
        return counts.getInt(fact);
    }

    public Map<String, Integer> counts() {
        var result = new Object2IntOpenHashMap<String>();
        result.putAll(counts);
        return Object2IntMaps.unmodifiable(result);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        return (T) data.getOrDefault(key, defaultValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> T getOrCreate(String key, Supplier<T> creator) {
        T result = get(key);
        if (result == null) {
            result = creator.get();
            set(key, result);
        }
        return result;
    }

    public <T> T getOrPut(String key, T initialValue) {
        T result = get(key);
        if (result == null) {
            result = initialValue;
            set(key, result);
        }
        return result;
    }

    public boolean containsKey(String key) {
        return data.containsKey(key);
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return data.entrySet();
    }
}
