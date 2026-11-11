package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Comparator;

/**
 * Ordered material-request providers shared by inventory and optional storage integrations.
 */
public final class MaterialSources {

    private MaterialSources() {}

    /** Registered providers in allocation order; returned materials use the reverse order. */
    public static ObjectList<Registration> getProviders() {
        return GTRegistries.MATERIAL_SOURCES.stream().sorted(Comparator.comparingInt(Registration::priority))
                .collect(ObjectImmutableList.toList());
    }

    /**
     * Lets every registered provider fulfill its own portion of the same demand list, in priority order.
     * Only reservations are created here; callers retain responsibility for commit and rollback.
     */
    public static ObjectList<MaterialRequest.Result> request(ServerPlayer player, MaterialRequest request) {
        ObjectList<MaterialRequest.Result> results = new ObjectArrayList<>();
        if (request.getBindings() == null) {
            for (Registration registration : getProviders()) {
                ResourceLocation id = GTRegistries.MATERIAL_SOURCES.getKey(registration);
                request.select(id, null);
                results.add(registration.provider().request(player, request));
            }
        } else {
            for (MaterialRequest.Result binding : request.getBindings()) {
                request.select(binding.providerId(), binding);
                results.add(
                        GTRegistries.MATERIAL_SOURCES.get(binding.providerId()).provider().request(player, request));
            }
        }
        return results;
    }

    /** A registered provider; lower priorities supply materials first and receive returned materials last. */
    public record Registration(int priority, MaterialRequestProvider provider) {}
}
