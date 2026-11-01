package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

import com.lowdragmc.lowdraglib2.gui.factory.IContainerUIHolder;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Captures one exact machine instance and menu incarnation for a dynamic-slot LDLib2 opening.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DynamicItemSlotMachineUIHolder implements MachineUIHolder, MenuProvider, IContainerUIHolder {

    private final Player player;
    private final MetaMachine openedMachine;
    @Getter
    private final BlockPos pos;
    @Getter
    private final ResourceLocation machineDefinitionId;
    @Getter
    private final UUID menuSessionId;
    @Nullable
    private GTDynamicItemSlotContainerMenu serverMenu;
    @Nullable
    private ModularUI openedUI;
    private boolean closed;

    /**
     * Captures the authoritative server machine and creates an unpredictable menu incarnation.
     */
    public DynamicItemSlotMachineUIHolder(ServerPlayer player, MetaMachine machine) {
        this(player, machine, machine.getBlockPos(), machine.getDefinition().getId(), UUID.randomUUID());
    }

    private DynamicItemSlotMachineUIHolder(Player player, MetaMachine openedMachine, BlockPos pos,
                                           ResourceLocation machineDefinitionId, UUID menuSessionId) {
        if (!openedMachine.getBlockPos().equals(pos) ||
                !openedMachine.getDefinition().getId().equals(machineDefinitionId)) {
            throw new IllegalArgumentException("Dynamic item-slot machine holder identity does not match its machine.");
        }
        this.player = player;
        this.openedMachine = openedMachine;
        this.pos = pos;
        this.machineDefinitionId = machineDefinitionId;
        this.menuSessionId = menuSessionId;
    }

    /** Opens this exact machine session and publishes its manifest only after Vanilla installs the menu. */
    static boolean open(MetaMachine machine, ServerPlayer player) {
        if (!(machine instanceof DynamicItemSlotMachineUIProvider provider)) {
            return false;
        }
        DynamicItemSlotMachineUIHolder holder = new DynamicItemSlotMachineUIHolder(player, machine);
        if (!provider.canCreateUI(player, holder)) {
            return false;
        }
        try {
            if (!UIRegistry.open(player, UIRegistry.MACHINE, holder)) {
                holder.abortOpening(player);
                return false;
            }
            holder.publishInitialManifest(player);
            return true;
        } catch (RuntimeException exception) {
            try {
                holder.abortOpening(player);
            } catch (RuntimeException closeException) {
                exception.addSuppressed(closeException);
            }
            GTCEu.LOGGER.error("Failed to open dynamic item-slot machine menu at {} for {}",
                    machine.getBlockPos(), player.getGameProfile().getName(), exception);
            return false;
        }
    }

    static ModularUIContainerMenu read(int windowId, Inventory inventory, BlockPos pos, RegistryFriendlyByteBuf data) {
        ResourceLocation machineDefinitionId = data.readResourceLocation();
        UUID menuSessionId = data.readUUID();
        DynamicItemSlotMachineUIHolder holder = client(inventory.player, pos, machineDefinitionId, menuSessionId);
        return new GTDynamicItemSlotContainerMenu(UIRegistry.MENU.get(), windowId, inventory, holder);
    }

    /**
     * Reconstructs the client holder from server-owned opening identity.
     */
    public static DynamicItemSlotMachineUIHolder client(Player player, BlockPos pos,
                                                        ResourceLocation machineDefinitionId,
                                                        UUID menuSessionId) {
        Level level = player.level();
        if (!level.isLoaded(pos)) {
            throw new IllegalStateException("Dynamic item-slot machine position is not loaded on the client.");
        }
        MetaMachine machine = MetaMachine.getMachine(level, pos);
        if (machine == null || !machine.getDefinition().getId().equals(machineDefinitionId)) {
            throw new IllegalStateException("Dynamic item-slot machine opening no longer resolves its machine.");
        }
        return new DynamicItemSlotMachineUIHolder(player, machine, pos, machineDefinitionId, menuSessionId);
    }

    /**
     * Resolves only the exact block entity instance captured for this opening.
     */
    @Nullable
    @Override
    public MetaMachine getMachine() {
        Level level = player.level();
        if (!level.isLoaded(pos)) {
            return null;
        }
        MetaMachine currentMachine = MetaMachine.getMachine(level, pos);
        return currentMachine == openedMachine &&
                currentMachine.getDefinition().getId().equals(machineDefinitionId) ? currentMachine : null;
    }

    /**
     * Keeps the opening valid only while its player, machine instance, and provider contract still match.
     */
    @Override
    public boolean isStillValid(Player player) {
        MetaMachine machine = getMachine();
        return !closed && this.player == player &&
                machine instanceof DynamicItemSlotMachineUIProvider provider &&
                provider.canCreateUI(player, this);
    }

    @Override
    public Component getDisplayName() {
        return openedMachine.getDefinition().getBlock().getName();
    }

    /**
     * Creates exactly one authoritative server menu for this opening.
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (!(player instanceof ServerPlayer) || this.player != player || serverMenu != null) {
            throw new IllegalStateException("Dynamic item-slot holder can create exactly one server menu.");
        }
        requireValid("before menu creation");
        GTDynamicItemSlotContainerMenu menu = new GTDynamicItemSlotContainerMenu(
                UIRegistry.MENU.get(), containerId, playerInventory, this);
        serverMenu = menu;
        requireValid("after menu creation");
        return menu;
    }

    /**
     * Writes the exact machine and menu incarnation used to construct the server menu.
     */
    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        if (menu != serverMenu) {
            throw new IllegalStateException("Dynamic item-slot opening data belongs to another menu.");
        }
        buffer.writeBlockPos(pos);
        MachineUIProvider.BLOCK_STATE_CODEC.encode(buffer, openedMachine.getBlockState());
        buffer.writeResourceLocation(machineDefinitionId);
        buffer.writeUUID(menuSessionId);
    }

    /**
     * Builds the provider UI against this exact opening holder.
     */
    @Override
    public ModularUI createUI(Player player) {
        if (openedUI != null) {
            throw new IllegalStateException("Dynamic item-slot holder can create exactly one ModularUI.");
        }
        MetaMachine machine = getMachine();
        if (this.player != player || !(machine instanceof DynamicItemSlotMachineUIProvider provider) ||
                !provider.canCreateUI(player, this)) {
            throw new IllegalStateException("Dynamic item-slot machine rejected its opened menu holder.");
        }
        UI ui = provider.createUI(player, this);
        if (ui == null) {
            throw new IllegalStateException("Dynamic item-slot machine UI provider returned null.");
        }
        openedUI = UIRegistry.createUI(ui, player);
        return openedUI;
    }

    /** Publishes the first manifest only after Vanilla installs this exact server menu. */
    void publishInitialManifest(ServerPlayer player) {
        GTDynamicItemSlotContainerMenu menu = serverMenu;
        if (this.player != player || menu == null || player.containerMenu != menu) {
            throw new IllegalStateException("Dynamic item-slot initial manifest requires its installed server menu.");
        }
        if (!menu.publishManifest(player)) {
            throw new IllegalStateException("Dynamic item-slot menu rejected its initial manifest.");
        }
    }

    /** Closes an installed menu or terminates a partially created opening. */
    void abortOpening(ServerPlayer player) {
        if (this.player != player) {
            throw new IllegalArgumentException("Dynamic item-slot opening belongs to another player.");
        }
        GTDynamicItemSlotContainerMenu menu = serverMenu;
        if (menu != null && player.containerMenu == menu) {
            player.closeContainer();
            return;
        }
        if (menu != null) {
            menu.abortOpening(player);
        }
        close(player);
    }

    /** Permanently invalidates this opening after its menu is removed or aborted. */
    void close(Player player) {
        if (this.player != player) {
            throw new IllegalArgumentException("Dynamic item-slot opening belongs to another player.");
        }
        closed = true;
    }

    private void requireValid(String phase) {
        if (!isStillValid(player)) {
            throw new IllegalStateException("Dynamic item-slot machine opening became invalid " + phase + '.');
        }
    }
}
