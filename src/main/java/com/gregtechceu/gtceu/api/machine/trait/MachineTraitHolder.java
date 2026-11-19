package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;
import java.util.Optional;

public final class MachineTraitHolder {

    public static final ContextualFieldCodec<MachineTraitHolder> CODEC = new ContextualFieldCodec<>() {

        @Override
        public Codec<MachineTraitHolder> codec(Context<MachineTraitHolder> context) {
            return SyncFieldData.CODEC.xmap(data -> {
                MachineTraitHolder holder = context.currentValue();
                holder.deserializeSyncFieldData(context.lookup(), data, context.isClientSync(),
                        context.parseExplicitNull(), context.isClientFullSyncUpdate(), false);
                return holder;
            }, holder -> holder.serializeSyncFieldData(context.lookup(), context.isClientSync(),
                    context.isClientFullSyncUpdate(), context.readOnly()));
        }

        @Override
        public boolean shouldSyncField(MachineTraitHolder holder, Context<MachineTraitHolder> context, boolean fullSync,
                                       boolean manuallyDirty) {
            return !context.isClientSync() ? fullSync || manuallyDirty :
                    holder.scanAndMarkClientChanges(context.lookup(), fullSync || manuallyDirty);
        }
    };

    static {
        FieldCodecs.registerContextual(MachineTraitHolder.class, CODEC);
    }

    private final MetaMachine machine;
    private final List<MachineTrait> traits;
    private final List<MachineTrait> syncTraits;
    private final Map<MachineTraitType<?>, List<MachineTrait>> traitsByType;

    private final Map<String, MachineTrait> traitsToSave;

    public MachineTraitHolder(MetaMachine machine) {
        this.machine = machine;
        this.traits = new ObjectArrayList<>();
        this.syncTraits = new ObjectArrayList<>();
        this.traitsByType = new Object2ObjectOpenHashMap<>();
        this.traitsToSave = new Object2ObjectOpenHashMap<>();
    }

    /**
     * @return An unmodifiable list of all traits attached to this machine.
     */
    public @Unmodifiable List<MachineTrait> getAllTraits() {
        return Collections.unmodifiableList(traits);
    }

    /**
     * Attaches a trait to this machine, with the default trait callback priority of 1.
     *
     * @param trait The trait to attach
     * @return The attached trait
     */
    public <T extends MachineTrait> T attachTrait(T trait) {
        return attachTrait(trait, 1);
    }

    /**
     * Attaches a trait to this machine.
     *
     * @param trait            The trait to attach
     * @param callbackPriority The trait's callback priority. Traits with a higher priority will have their events fired
     *                         first, which may prevent traits with a lower priority from handling some events.
     * @return The attached trait
     */
    public <T extends MachineTrait> T attachTrait(T trait, int callbackPriority) {
        trait.setTraitPriority(callbackPriority);

        var traitType = trait.getTraitType();

        var list = traitsByType.computeIfAbsent(traitType, $ -> new ObjectArrayList<>(1));
        if (!traitType.allowsMultipleInstances() && !list.isEmpty()) {
            throw new IllegalArgumentException("Attempted to add multiple traits of type: " + trait.getClass());
        }

        list.add(trait);
        list.sort(Comparator.comparingInt(MachineTrait::getTraitPriority).reversed());
        traits.add(trait);
        traits.sort(Comparator.comparingInt(MachineTrait::getTraitPriority).reversed());
        syncTraits.add(trait);
        machine.getSyncDataHolder().attachRPC(trait);

        trait.setMachine(machine);
        return trait;
    }

    /**
     * Returns traits in immutable attachment order for stable client-to-server sync target identities.
     */
    public @UnmodifiableView List<MachineTrait> getSyncTraits() {
        return Collections.unmodifiableList(syncTraits);
    }

    /**
     * Registers a trait with data to be saved or synced to the client.
     * Do not register a persistent trait and also store that trait as a syncable machine field, otherwise the trait
     * data will be duplicated. Use only one sync method.
     *
     * @param traitName Unique identifier for this trait.
     * @param trait     The trait to register
     */
    public MachineTraitHolder registerPersistentTrait(String traitName, MachineTrait trait) {
        if (trait.getMachine() != machine) throw new IllegalArgumentException("Trait does not belong to this machine.");
        if (traitsToSave.containsKey(traitName))
            throw new IllegalArgumentException("Attempted to register duplicate trait save key \"" + traitName + "\"");
        traitsToSave.put(traitName, trait);
        return this;
    }

    /**
     * Gets a trait registered by {@code registerPersistentTrait}
     *
     * @param traitName the unique identifier for the trait
     * @return the trait, or null if not present
     */
    @SuppressWarnings("unchecked")
    public @Nullable <T extends MachineTrait> T getPersistentTrait(String traitName) {
        MachineTrait trait = traitsToSave.get(traitName);
        return trait == null ? null : (T) trait;
    }

    /**
     * Gets the first trait (trait with highest priority) of a specified type
     *
     * @param type The trait type to get
     * @return The trait, or null if no traits of the given type are present.
     */
    public <T extends MachineTrait> @Nullable T getTrait(MachineTraitType<T> type) {
        List<MachineTrait> traitList = traitsByType.get(type);
        if (traitList == null || traitList.isEmpty()) return null;
        return type.castTrait(traitList.get(0));
    }

    /**
     * Gets the first trait (trait with highest priority) of a specified type
     *
     * @param type The trait type to get
     * @return An optional result containing the trait if present.
     */
    public <T extends MachineTrait> Optional<T> getTraitOptional(MachineTraitType<T> type) {
        return Optional.ofNullable(getTrait(type));
    }

    public SyncFieldData serializeSyncFieldData(HolderLookup.Provider lookup, boolean isClientSync, boolean fullSync,
                                                boolean readOnly) {
        SyncFieldData.Builder builder = SyncFieldData.builder();
        if (isClientSync) {
            for (int i = 0; i < traits.size(); i++) {
                var storage = traits.get(i).getSyncDataHolder();
                SyncFieldData traitData = readOnly ? storage.getClientData(lookup) :
                        storage.serializeToFieldData(lookup, true, fullSync);
                if (fullSync || !traitData.isEmpty()) {
                    builder.put(SyncFieldData.key(Integer.toString(i)), traitData);
                }
            }
        } else {
            traitsToSave.forEach((key, trait) -> builder.put(SyncFieldData.key(key),
                    trait.getSyncDataHolder().serializeToFieldData(lookup, false, fullSync)));
        }
        return builder.build();
    }

    public boolean scanAndMarkClientChanges(HolderLookup.Provider lookup, boolean fullSync) {
        boolean changed = false;
        for (MachineTrait trait : traits) {
            if (fullSync) {
                trait.getSyncDataHolder().resyncAllFields();
                changed = true;
            } else if (trait.getSyncDataHolder().scanAndMarkChanges(lookup)) {
                changed = true;
            }
        }
        return changed;
    }

    public void deserializeSyncFieldData(HolderLookup.Provider lookup, SyncFieldData data, boolean isClientSync) {
        deserializeSyncFieldData(lookup, data, isClientSync, false);
    }

    public void deserializeSyncFieldData(HolderLookup.Provider lookup, SyncFieldData data, boolean isClientSync,
                                         boolean parseExplicitNull) {
        deserializeSyncFieldData(lookup, data, isClientSync, parseExplicitNull, false);
    }

    public void deserializeSyncFieldData(HolderLookup.Provider lookup, SyncFieldData data, boolean isClientSync,
                                         boolean parseExplicitNull, boolean fullSync) {
        deserializeSyncFieldData(lookup, data, isClientSync, parseExplicitNull, fullSync, true);
    }

    public void deserializeSyncFieldData(HolderLookup.Provider lookup, SyncFieldData data, boolean isClientSync,
                                         boolean parseExplicitNull, boolean fullSync,
                                         boolean notifyUnchangedOnFullSync) {
        if (isClientSync) {
            for (Map.Entry<ResourceLocation, Optional<Dynamic<?>>> entry : data.fields().entrySet()) {
                String key = entry.getKey().getPath();
                int index;
                try {
                    index = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    GTCEu.LOGGER.warn("Attempted to deserialise syncable trait '{}', but it is not a trait index",
                            key);
                    continue;
                }
                if (index < 0 || index >= traits.size()) {
                    GTCEu.LOGGER.warn("Attempted to deserialise syncable trait '{}', but only {} traits are attached",
                            key, traits.size());
                    continue;
                }
                traits.get(index).getSyncDataHolder()
                        .deserializeFieldData(lookup, data.read(entry.getKey(), SyncFieldData.CODEC), true,
                                parseExplicitNull, fullSync, notifyUnchangedOnFullSync);
            }
            return;
        }

        for (Map.Entry<ResourceLocation, Optional<Dynamic<?>>> entry : data.fields().entrySet()) {
            String key = entry.getKey().getPath();
            var trait = getPersistentTrait(key);
            if (trait == null) {
                GTCEu.LOGGER.warn("Attempted to deserialise syncable trait '{}', but no syncable trait has that ID",
                        key);
                continue;
            }
            trait.getSyncDataHolder().deserializeFieldData(lookup, data.read(entry.getKey(), SyncFieldData.CODEC),
                    isClientSync, parseExplicitNull, fullSync, notifyUnchangedOnFullSync);
        }
    }

    /**
     * Get all traits with the specified type.
     *
     * @return An unmodifiable list containing all traits of the specified type.
     */
    @SuppressWarnings("unchecked")
    public <T extends MachineTrait> @UnmodifiableView List<T> getTraits(MachineTraitType<T> type) {
        List<T> traitList = (List<T>) traitsByType.get(type);
        if (traitList == null) return List.of();
        return Collections.unmodifiableList(traitList);
    }
}
