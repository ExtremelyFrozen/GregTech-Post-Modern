package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldNormalizer;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncBoth;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.util.Mth;

import lombok.Getter;

public class ParallelHatchPartMachine extends TieredPartMachine {

    private static final int MIN_PARALLEL = 1;
    private static final int PAGE_WIDTH = 100;
    private static final int PAGE_HEIGHT = 20;

    private final int maxParallel;

    @SaveField
    @SyncBoth
    @Getter
    private int currentParallel = 1;

    public ParallelHatchPartMachine(BlockEntityCreationInfo info, int tier) {
        super(info, tier);
        this.maxParallel = (int) Math.pow(4, tier - GTValues.EV);
        this.currentParallel = maxParallel;
    }

    public void setCurrentParallel(int parallelAmount) {
        int normalizedParallel = normalizeCurrentParallel(parallelAmount);
        if (this.currentParallel == normalizedParallel) {
            return;
        }
        this.currentParallel = normalizedParallel;
        markControllerRecipesDirty();
    }

    @ServerFieldNormalizer(fieldName = "currentParallel")
    private int normalizeCurrentParallel(int parallelAmount) {
        return Mth.clamp(parallelAmount, MIN_PARALLEL, this.maxParallel);
    }

    @ServerFieldChangeListener(fieldName = "currentParallel")
    private void onCurrentParallelChanged(int oldParallel, int newParallel) {
        markControllerRecipesDirty();
    }

    protected void markControllerRecipesDirty() {
        for (MultiblockControllerMachine controller : this.getControllers()) {
            if (controller instanceof IRecipeLogicMachine rlm) {
                rlm.getRecipeLogic().markLastRecipeDirty();
            }
        }
    }

    /** Creates a new holder-scoped page for one surrounding multiblock UI opening. */

    private UIElement createParallelInput() {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        root.addChild(new GTIntInputElement(0, 0, PAGE_WIDTH, PAGE_HEIGHT, this::getCurrentParallel,
                this::setCurrentParallelAndSync)
                .setMin(MIN_PARALLEL)
                .setMax(maxParallel));
        return root;
    }

    private void setCurrentParallelAndSync(int value) {
        setCurrentParallel(value);
        sendServerSyncChanges();
    }

    /** Owns the contextual Parallel Hatch page state for exactly one menu opening. */

    @Override
    public boolean canShared(MultiblockControllerMachine controller, String structureName) {
        return false;
    }

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        MachineUIHolder holder = shell.getHolder(this);

        return createParallelInput();
    }

    @Override
    public int getPageWidth() {
        return PAGE_WIDTH;
    }

    @Override
    public int getPageHeight() {
        return PAGE_HEIGHT;
    }
}
