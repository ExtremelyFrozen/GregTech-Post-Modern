package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.Reservation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Factory methods for automatic multiblock material sources.
 */
public final class MaterialSources {

    private static @Nullable BiFunction<ServerPlayer, ItemStack, MaterialSource> meSourceFactory;

    private MaterialSources() {}

    public static MaterialSource playerInventory(Player player) {
        return new PlayerInventoryMaterialSource(player.getCapability(Capabilities.ItemHandler.ENTITY),
                ItemStack.EMPTY, MaterialSource.SourceKind.PLAYER, player);
    }

    public static MaterialSource playerInventory(Player player, ItemStack excludedContainer) {
        return new PlayerInventoryMaterialSource(player.getCapability(Capabilities.ItemHandler.ENTITY),
                excludedContainer, MaterialSource.SourceKind.PLAYER, player);
    }

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

    public static void registerMESource(Function<ServerPlayer, MaterialSource> factory) {
        meSourceFactory = (player, excluded) -> factory.apply(player);
    }

    /**
     * Registers the AE source factory that can exclude the exact terminal used to open an automatic-build session.
     */
    public static void registerMESource(BiFunction<ServerPlayer, ItemStack, MaterialSource> factory) {
        meSourceFactory = factory;
    }

    public static MaterialSource me(ServerPlayer player) {
        if (meSourceFactory == null) {
            return unavailable(new BuildProblem(BuildProblem.Type.ME_UNAVAILABLE, null,
                    Component.translatable("gtpm.multiblock.autobuild.me_unavailable")));
        }
        return meSourceFactory.apply(player, ItemStack.EMPTY);
    }

    public static MaterialSource me(ServerPlayer player, ItemStack excludedTerminal) {
        if (meSourceFactory == null) {
            return unavailable(new BuildProblem(BuildProblem.Type.ME_UNAVAILABLE, null,
                    Component.translatable("gtpm.multiblock.autobuild.me_unavailable")));
        }
        return meSourceFactory.apply(player, excludedTerminal);
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
        public @Nullable Reservation reserve(ObjectList<ItemStack> candidates) {
            return null;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return stack;
        }
    }
}
