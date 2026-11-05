package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.FancyPreviewPage;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.FancyActionMachine;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerList;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTDataComponents;
import com.gregtechceu.gtceu.integration.ae2.machine.trait.ProxySlotRecipeHandler;

import com.lowdragmc.lowdraglib2.gui.ui.UI;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MEPatternBufferProxyPartMachine extends TieredIOPartMachine
                                             implements IDataStickInteractable, IUIMachine,
                                             FancyPartUIProvider, FancyActionMachine {

    @Getter
    private final ProxySlotRecipeHandler proxySlotRecipeHandler;

    @SaveField
    @Getter
    @SyncToClient
    private @Nullable BlockPos bufferPos;

    private @Nullable MEPatternBufferPartMachine buffer = null;
    private boolean bufferResolved = false;

    public MEPatternBufferProxyPartMachine(BlockEntityCreationInfo info) {
        super(info, GTValues.LuV, IO.IN);
        proxySlotRecipeHandler = new ProxySlotRecipeHandler(this, MEPatternBufferPartMachine.MAX_PATTERN_COUNT);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) this.setBuffer(bufferPos);
    }

    @Override
    public List<RecipeHandlerList> getRecipeHandlers() {
        return proxySlotRecipeHandler.getProxySlotHandlers();
    }

    public void setBuffer(@Nullable BlockPos pos) {
        bufferPos = pos;
        replaceResolvedBuffer(resolveBuffer(pos));
    }

    @Nullable
    public MEPatternBufferPartMachine getBuffer() {
        MEPatternBufferPartMachine resolved = resolveBuffer(bufferPos);
        if (!bufferResolved || resolved != buffer) {
            replaceResolvedBuffer(resolved);
        }
        return buffer;
    }

    private @Nullable MEPatternBufferPartMachine resolveBuffer(@Nullable BlockPos pos) {
        var level = getLevel();
        if (level == null || pos == null) {
            return null;
        }
        if (MetaMachine.getMachine(level, pos) instanceof MEPatternBufferPartMachine machine) {
            return machine;
        }
        return null;
    }

    private void replaceResolvedBuffer(@Nullable MEPatternBufferPartMachine resolved) {
        MEPatternBufferPartMachine previous = buffer;
        if (previous == resolved && bufferResolved) {
            return;
        }
        if (previous != null) {
            previous.removeProxy(this);
        }
        proxySlotRecipeHandler.clearProxy();
        buffer = resolved;
        bufferResolved = true;
        if (resolved != null) {
            resolved.addProxy(this);
            if (!isRemote()) {
                proxySlotRecipeHandler.updateProxy(resolved);
            }
        }
    }

    @Override
    public boolean openUI(MetaMachine machine, ServerPlayer player) {
        var linked = getBuffer();
        return linked != null && linked.openUI(linked, player);
    }

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return getBuffer() != null;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        return getBuffer().createUI(player, holder);
    }

    /** Creates the proxy-local preview used by a surrounding multiblock without opening its linked buffer. */
    @Override
    public IFancyUIProvider createFancyPage(Player player, MachineUIHolder holder) {
        return new FancyPreviewPage(this, player, holder,
                new IFancyUIProvider.PageGroupingData(
                        "gtpm.multiblock.page_switcher.io.import", 1));
    }

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        setBuffer(null);
    }

    @Override
    public InteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        BlockPos bufferPos = dataStick.get(GTDataComponents.DATA_COPY_POS);
        if (bufferPos != null) {
            setBuffer(bufferPos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
