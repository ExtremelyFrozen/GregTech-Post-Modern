package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.api.multiblock.autobuild.AutoBuildMaterialSource.Reservation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Factory methods for automatic multiblock material sources.
 */
public final class AutoBuildMaterialSources {

    private static @Nullable BiFunction<ServerPlayer, ItemStack, AutoBuildMaterialSource> meSourceFactory;

    private AutoBuildMaterialSources() {}

    public static AutoBuildMaterialSource playerInventory(Player player) {
        return new PlayerInventoryMaterialSource(player.getCapability(Capabilities.ItemHandler.ENTITY),
                ItemStack.EMPTY, AutoBuildMaterialSource.SourceKind.PLAYER, player);
    }

    public static AutoBuildMaterialSource playerInventory(Player player, ItemStack excludedContainer) {
        return new PlayerInventoryMaterialSource(player.getCapability(Capabilities.ItemHandler.ENTITY),
                excludedContainer, AutoBuildMaterialSource.SourceKind.PLAYER, player);
    }

    public static AutoBuildMaterialSource itemHandler(IItemHandler handler) {
        return new PlayerInventoryMaterialSource(handler, ItemStack.EMPTY, AutoBuildMaterialSource.SourceKind.OTHER,
                null);
    }

    public static AutoBuildMaterialSource itemHandler(IItemHandler handler, ItemStack excludedContainer) {
        return new PlayerInventoryMaterialSource(handler, excludedContainer,
                AutoBuildMaterialSource.SourceKind.OTHER, null);
    }

    public static AutoBuildMaterialSource unavailable(AutoBuildProblem problem) {
        return new UnavailableMaterialSource(problem);
    }

    public static void registerMESource(Function<ServerPlayer, AutoBuildMaterialSource> factory) {
        meSourceFactory = (player, excluded) -> factory.apply(player);
    }

    /**
     * Registers the AE source factory that can exclude the exact terminal used to open an automatic-build session.
     */
    public static void registerMESource(BiFunction<ServerPlayer, ItemStack, AutoBuildMaterialSource> factory) {
        meSourceFactory = factory;
    }

    public static AutoBuildMaterialSource me(ServerPlayer player) {
        if (meSourceFactory == null) {
            return unavailable(new AutoBuildProblem(AutoBuildProblem.Type.ME_UNAVAILABLE, null,
                    Component.translatable("gtpm.multiblock.autobuild.me_unavailable")));
        }
        return meSourceFactory.apply(player, ItemStack.EMPTY);
    }

    public static AutoBuildMaterialSource me(ServerPlayer player, ItemStack excludedTerminal) {
        if (meSourceFactory == null) {
            return unavailable(new AutoBuildProblem(AutoBuildProblem.Type.ME_UNAVAILABLE, null,
                    Component.translatable("gtpm.multiblock.autobuild.me_unavailable")));
        }
        return meSourceFactory.apply(player, excludedTerminal);
    }

    private record UnavailableMaterialSource(AutoBuildProblem problem) implements AutoBuildMaterialSource {

        @Override
        public SourceKind kind() {
            return SourceKind.UNAVAILABLE;
        }

        @Override
        public Session openSession() {
            return new UnavailableSession(problem);
        }

        @Override
        public AutoBuildProblem unavailableProblem() {
            return problem;
        }
    }

    private record UnavailableSession(AutoBuildProblem problem) implements AutoBuildMaterialSource.Session {

        @Override
        public AutoBuildProblem problem() {
            return problem;
        }

        @Override
        public @Nullable Reservation reserve(List<ItemStack> candidates) {
            return null;
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return stack;
        }
    }
}
