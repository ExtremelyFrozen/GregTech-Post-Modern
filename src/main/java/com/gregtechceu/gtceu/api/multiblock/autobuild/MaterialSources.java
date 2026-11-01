package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.SourceKind;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

import java.util.Comparator;

/**
 * Ordered material-request providers shared by inventory and optional storage integrations.
 */
public final class MaterialSources {

    private static final Object2ObjectMap<ResourceLocation, Registration> PROVIDERS = new Object2ObjectLinkedOpenHashMap<>();
    private static final ObjectList<ResourceLocation> ORDERED = new ObjectArrayList<>();

    static {
        register(GTCEu.id("player_inventory"), 100, SourceKind.PLAYER, PlayerInventoryMaterialSource::request);
    }

    private MaterialSources() {}

    /**
     * Registers a provider during common initialization; lower priorities are allocated first.
     * Equal priorities retain registration order, and duplicate identifiers are rejected.
     */
    public static void register(ResourceLocation id, int priority, SourceKind kind,
                                MaterialRequestProvider provider) {
        if (PROVIDERS.putIfAbsent(id, new Registration(priority, kind, provider)) != null) {
            throw new IllegalStateException("Material request provider already registered: " + id);
        }
        ORDERED.add(id);
        ORDERED.sort(Comparator.comparingInt(key -> PROVIDERS.get(key).priority()));
    }

    /**
     * Lets every selected provider fulfill its own portion of the same demand list, in registration priority order.
     * Only reservations are created here; callers retain responsibility for commit and rollback.
     */
    public static ObjectList<MaterialRequest.Result> request(ServerPlayer player, MaterialRequest request) {
        ObjectList<MaterialRequest.Result> results = new ObjectArrayList<>();
        if (request.getBindings() == null) {
            for (ResourceLocation id : ORDERED) {
                Registration registration = PROVIDERS.get(id);
                if (request.includes(registration.kind())) {
                    request.select(id, null);
                    results.add(registration.provider().request(player, request));
                }
            }
        } else {
            for (MaterialRequest.Result binding : request.getBindings()) {
                request.select(binding.providerId(), binding);
                results.add(PROVIDERS.get(binding.providerId()).provider().request(player, request));
            }
        }
        return results;
    }

    private record Registration(int priority, SourceKind kind, MaterialRequestProvider provider) {}

}
