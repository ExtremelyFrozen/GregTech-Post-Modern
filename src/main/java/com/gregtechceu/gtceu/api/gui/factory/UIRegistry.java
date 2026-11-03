package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.GTCEu;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.vfyjxf.taffy.style.TaffyPosition;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Owns the single GT menu transport and dispatches each opening to its registered owner factory. */
@NullMarked
public final class UIRegistry {

    public static final ResourceLocation MACHINE = GTCEu.id("machine");
    public static final ResourceLocation HELD_ITEM = GTCEu.id("held_item");
    public static final ResourceLocation COVER = GTCEu.id("cover");

    private static final Object2ObjectOpenHashMap<ResourceLocation, UIFactory> FACTORIES = new Object2ObjectOpenHashMap<>();
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(BuiltInRegistries.MENU,
            GTCEu.MOD_ID);

    public static final Supplier<MenuType<ModularUIContainerMenu>> MENU = MENUS.register("ui",
            () -> IMenuTypeExtension.create(UIRegistry::create));

    private UIRegistry() {}

    public static void init(IEventBus modBus) {
        register(MACHINE, MachineBlockUIHolder::read);
        register(HELD_ITEM, HeldItemUIHolderContext::read);
        register(COVER, CoverUIHolder::read);
        GTUIEditorMenu.register();
        MENUS.register(modBus);
    }

    /** Registers a client decoder during common initialization, including optional integration boundaries. */
    public static void register(ResourceLocation id, UIFactory factory) {
        if (FACTORIES.putIfAbsent(id, factory) != null) {
            throw new IllegalArgumentException("UI factory is already registered: " + id);
        }
    }

    /** Prefixes the owner's existing opening payload with its factory id without changing its menu lifecycle. */
    public static boolean open(ServerPlayer player, ResourceLocation id, MenuProvider owner) {
        if (!FACTORIES.containsKey(id)) {
            throw new IllegalArgumentException("Unknown UI factory: " + id);
        }
        return player.openMenu(new MenuProvider() {

            @Override
            public Component getDisplayName() {
                return owner.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player menuPlayer) {
                return owner.createMenu(containerId, inventory, menuPlayer);
            }

            @Override
            public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
                buffer.writeResourceLocation(id);
                owner.writeClientSideData(menu, buffer);
            }
        }).isPresent();
    }

    /** Keeps GT's supplied textures and sizes while allowing LDLib to center every menu root. */
    public static ModularUI createUI(UI ui, @Nullable Player player) {
        ui.getRootElement().getLayout().positionType(TaffyPosition.RELATIVE);
        return ModularUI.of(ui, player);
    }

    private static ModularUIContainerMenu create(int windowId, Inventory inventory, RegistryFriendlyByteBuf data) {
        return FACTORIES.get(data.readResourceLocation()).create(windowId, inventory, data);
    }
}
