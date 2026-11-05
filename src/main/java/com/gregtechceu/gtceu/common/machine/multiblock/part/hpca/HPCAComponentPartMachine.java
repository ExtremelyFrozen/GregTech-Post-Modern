package com.gregtechceu.gtceu.common.machine.multiblock.part.hpca;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.FancyPreviewPage;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.machine.trait.hpca.HPCAComponentTrait;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class HPCAComponentPartMachine extends MultiblockPartMachine implements FancyPartUIProvider {

    @Getter
    protected final HPCAComponentTrait hpcaComponentTrait;

    public HPCAComponentPartMachine(BlockEntityCreationInfo info,
                                    HPCAComponentTrait hpcaTrait) {
        super(info);
        this.hpcaComponentTrait = attachTrait(hpcaTrait);
    }

    public abstract boolean isAdvanced();

    public abstract IGuiTexture getComponentIcon();

    /** Creates the holder-scoped default preview used by the HPCA controller. */
    @Override
    public IFancyUIProvider createFancyPage(Player player, MachineUIHolder holder) {
        return new FancyPreviewPage(this, player, holder, null);
    }

    @Override
    public int getDefaultPaintingColor() {
        return 0xFFFFFF;
    }

    @Override
    public boolean canShared(MultiblockControllerMachine controller, String structureName) {
        return false;
    }

    @Override
    public void modifyDrops(List<ItemStack> drops) {
        for (int i = 0; i < drops.size(); ++i) {
            ItemStack drop = drops.get(i);
            if (drop.getItem() == this.getDefinition().getItem()) {
                if (hpcaComponentTrait.isDamaged()) {
                    if (isAdvanced()) {
                        drops.set(i, GTBlocks.ADVANCED_COMPUTER_CASING.asStack());
                    } else {
                        drops.set(i, GTBlocks.COMPUTER_CASING.asStack());
                    }
                }
                break;
            }
        }
    }
}
