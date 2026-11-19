package com.gregtechceu.gtceu.common.capability;

import com.gregtechceu.gtceu.api.capability.IMedicalConditionTracker;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.data.medicalcondition.Symptom;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.common.data.datacomponents.MedicalConditionTrackerData;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMaps;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MedicalConditionTracker implements IMedicalConditionTracker {

    private final Object2FloatOpenHashMap<MedicalCondition> medicalConditions = new Object2FloatOpenHashMap<>();
    private final ObjectSet<MedicalCondition> permanentConditions = new ObjectOpenHashSet<>();
    private Reference2ObjectMap<Symptom, ActiveSymptom> activeSymptoms = new Reference2ObjectOpenHashMap<>();
    private final Object2IntMap<Holder<MobEffect>> activeMobEffects = new Object2IntOpenHashMap<>();

    private final ObjectSet<MedicalCondition> flaggedForRemoval = new ObjectOpenHashSet<>();

    @Setter
    private int maxAirSupply = -1;

    @Getter
    private final Player player;

    public MedicalConditionTracker(Player player) {
        this.player = player;
    }

    @Override
    public Object2FloatMap<MedicalCondition> getMedicalConditions() {
        return medicalConditions;
    }

    @Override
    public void tick() {
        if (player.isCreative()) return;

        for (var entry : activeMobEffects.object2IntEntrySet()) {
            player.addEffect(new MobEffectInstance(entry.getKey(), 100, entry.getIntValue()));
        }

        if (player.level().getGameTime() % 20 == 0) { // apply idle progression every second
            for (MedicalCondition condition : medicalConditions.keySet()) {
                if (condition.idleProgressionType == MedicalCondition.IdleProgressionType.NONE) {
                    continue;
                }
                if (permanentConditions.contains(condition) &&
                        condition.idleProgressionType == MedicalCondition.IdleProgressionType.HEAL) {
                    // can't automatically heal permanent conditions.
                    continue;
                }
                int multiplier = (condition.idleProgressionType == MedicalCondition.IdleProgressionType.HEAL) ? -1 : 1;
                medicalConditions.replace(condition,
                        medicalConditions.getFloat(condition) + condition.idleProgressionRate * multiplier);
                evaluateMedicalCondition(condition);
            }
            if (!medicalConditions.isEmpty() || !flaggedForRemoval.isEmpty()) {
                updateActiveSymptoms(true);
            }
        }
    }

    public void progressCondition(@NotNull MedicalCondition condition, float strength) {
        if (player.isCreative()) return;

        medicalConditions.addTo(condition, strength);
        evaluateMedicalCondition(condition);
        updateActiveSymptoms(true);
    }

    private void updateActiveSymptoms(boolean tickSymptoms) {
        for (MedicalCondition condition : flaggedForRemoval) {
            medicalConditions.removeFloat(condition);
        }
        flaggedForRemoval.clear();

        var nextSymptoms = new Reference2ObjectOpenHashMap<Symptom, ActiveSymptom>(activeSymptoms.size());
        for (var entry : Object2FloatMaps.fastIterable(medicalConditions)) {
            MedicalCondition condition = entry.getKey();
            float progression = entry.getFloatValue();
            if (condition.canBePermanent && progression >= condition.maxProgression * 2) {
                // If condition has been applied for 2x the maximum time, make it permanent.
                permanentConditions.add(condition);
            }

            for (Symptom.ConfiguredSymptom configured : condition.symptoms) {
                int stage = calculateStage(condition, configured, progression);
                if (stage > 0) {
                    if (tickSymptoms) {
                        configured.symptom.tick(this, condition, configured, stage);
                    }
                    ActiveSymptom candidate = new ActiveSymptom(condition, configured, stage);
                    nextSymptoms.merge(configured.symptom, candidate,
                            (current, next) -> next.strength() > current.strength() ? next : current);
                }
            }
        }

        for (var entry : Reference2ObjectMaps.fastIterable(activeSymptoms)) {
            Symptom symptom = entry.getKey();
            ActiveSymptom previous = entry.getValue();
            if (!previous.equals(nextSymptoms.get(symptom))) {
                symptom.applyProgression(this, previous.condition(), previous.configured(), 0);
            }
        }

        for (var entry : Reference2ObjectMaps.fastIterable(nextSymptoms)) {
            Symptom symptom = entry.getKey();
            ActiveSymptom current = entry.getValue();
            if (!current.equals(activeSymptoms.get(symptom))) {
                symptom.applyProgression(this, current.condition(), current.configured(), current.stage());
            }
        }
        activeSymptoms = nextSymptoms;
    }

    @Override
    public void removeMedicalCondition(MedicalCondition condition) {
        flaggedForRemoval.add(condition);
        permanentConditions.remove(condition);
    }

    private static int calculateStage(MedicalCondition condition, Symptom.ConfiguredSymptom symptom, float progression) {
        float threshold = symptom.progressionThreshold * condition.maxProgression;
        if (progression >= condition.maxProgression) return symptom.stages;
        if (progression <= threshold) return 0;
        float fraction = (progression - threshold) / (condition.maxProgression - threshold);
        return (int) (fraction * symptom.stages);
    }

    private record ActiveSymptom(MedicalCondition condition, Symptom.ConfiguredSymptom configured, int stage) {

        private float strength() {
            return configured.relativeHarshness * stage;
        }
    }

    // removes MedicalConditions without progression
    private void evaluateMedicalCondition(MedicalCondition condition) {
        if (permanentConditions.contains(condition)) {
            return;
        }
        if (medicalConditions.getFloat(condition) <= 0) {
            removeMedicalCondition(condition);
        }
    }

    /**
     * called on antidote/cure consumption
     *
     * @param condition   MedicalCondition to heal
     * @param progression amount of progression to decrease
     */
    @Override
    public void heal(MedicalCondition condition, int progression) {
        if (progression >= medicalConditions.getOrDefault(condition, 0)) {
            removeMedicalCondition(condition);
        } else {
            medicalConditions.addTo(condition, -progression);
        }
        updateActiveSymptoms(false);
    }

    @Override
    public int getMaxAirSupply() {
        return maxAirSupply;
    }

    @Override
    public void setMobEffect(Holder<MobEffect> effect, int amplifier) {
        if (amplifier <= 0) {
            activeMobEffects.removeInt(effect);
        } else if (amplifier >= activeMobEffects.getOrDefault(effect, -1)) {
            activeMobEffects.put(effect, amplifier);
        }
    }

    public DataComponentMap exportComponents() {
        List<MedicalConditionTrackerData.Entry> conditions = new ArrayList<>(medicalConditions.size());
        for (var entry : medicalConditions.object2FloatEntrySet()) {
            conditions.add(new MedicalConditionTrackerData.Entry(entry.getKey(), entry.getFloatValue()));
        }

        MedicalConditionTrackerData data = new MedicalConditionTrackerData(conditions,
                new ArrayList<>(permanentConditions));
        if (data.isEmpty()) {
            return DataComponentMap.EMPTY;
        }
        return DataComponentMap.builder()
                .set(GTDataComponents.MEDICAL_CONDITION_TRACKER.get(), data)
                .build();
    }

    public void importComponents(DataComponentMap components) {
        medicalConditions.clear();
        permanentConditions.clear();
        flaggedForRemoval.clear();
        activeSymptoms.clear();
        activeMobEffects.clear();
        maxAirSupply = -1;

        MedicalConditionTrackerData data = components.getOrDefault(GTDataComponents.MEDICAL_CONDITION_TRACKER.get(),
                MedicalConditionTrackerData.EMPTY);
        for (MedicalConditionTrackerData.Entry entry : data.medicalConditions()) {
            medicalConditions.put(entry.condition(), entry.progression());
        }
        permanentConditions.addAll(data.permanentConditions());
    }
}
