package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.Reservation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.SourceKind;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;

/**
 * Ordered material-request providers shared by inventory and optional storage integrations.
 */
public final class MaterialSources {

    private static final Object2ObjectMap<ResourceLocation, Registration> PROVIDERS = new Object2ObjectLinkedOpenHashMap<>();

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
    }

    public static boolean hasProvider(SourceKind kind) {
        return PROVIDERS.values().stream().anyMatch(registration -> registration.kind() == kind);
    }

    /**
     * Lets every selected provider fulfill its own portion of the same demand list, in registration priority order.
     * Only reservations are created here; callers retain responsibility for commit and rollback.
     */
    public static ObjectList<MaterialRequest.Result> request(ServerPlayer player, MaterialRequest request) {
        ObjectList<Registration> ordered = new ObjectArrayList<>(PROVIDERS.values());
        ordered.sort(Comparator.comparingInt(Registration::priority));
        ObjectList<MaterialRequest.Result> results = new ObjectArrayList<>(ordered.size());
        for (Registration registration : ordered) {
            if (request.includes(registration.kind())) {
                results.add(registration.provider().request(player, request));
            }
        }
        return results;
    }

    private record Registration(int priority, SourceKind kind, MaterialRequestProvider provider) {}

    public static MaterialSource itemHandler(IItemHandler handler) {
        return new PlayerInventoryMaterialSource(handler, ItemStack.EMPTY, MaterialSource.SourceKind.OTHER,
                null);
    }

    public static MaterialSource itemHandler(IItemHandler handler, ItemStack excludedContainer) {
        return new PlayerInventoryMaterialSource(handler, excludedContainer,
                MaterialSource.SourceKind.OTHER, null);
    }

    public static MaterialSource unavailable(BuildProblem problem) {
        return new UnavailableMaterialSource(problem);
    }

    private record UnavailableMaterialSource(BuildProblem problem) implements MaterialSource {

        @Override
        public SourceKind kind() {
            return SourceKind.UNAVAILABLE;
        }

        @Override
        public Session openSession() {
            return new UnavailableSession(problem);
        }

        @Override
        public BuildProblem unavailableProblem() {
            return problem;
        }
    }

    private record UnavailableSession(BuildProblem problem) implements MaterialSource.Session {

        @Override
        public BuildProblem problem() {
            return problem;
        }

        @Override
        public long available(MaterialKey key) {
            return 0;
        }

        @Override
        public @Nullable Reservation reserve(ObjectList<ItemStack> candidates) {
            return null;
        }

        @Override
        public @Nullable Reservation reserve(MaterialKey key, long amount) {
            return null;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return stack;
        }
    }
}
