package com.gregtechceu.gtceu.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTIntInputElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.gui.fancy.TooltipsPanel;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldNormalizer;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncBoth;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;

import java.util.List;

public class ParallelHatchPartMachine extends TieredPartMachine
                                      implements IUIMachine, FancyPartUIProvider {

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

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        requireMatchingHolder(holder);
        return UI.of(createParallelInput());
    }

    /** Creates a new holder-scoped page for one surrounding multiblock UI opening. */
    @Override
    public IFancyUIProvider createFancyPage(Player player, MachineUIHolder holder) {
        requireMatchingHolder(holder);
        return new ParallelHatchPage(player, holder);
    }

    private UIElement createParallelInput() {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        root.addChild(new GTIntInputElement(0, 0, PAGE_WIDTH, PAGE_HEIGHT, this::getCurrentParallel,
                this::setCurrentParallelAndSync)
                .setMin(MIN_PARALLEL)
                .setMax(maxParallel));
        return root;
    }

    private void requireMatchingHolder(MachineUIHolder holder) {
        if (holder.getMachine() != this) {
            throw new IllegalArgumentException("Parallel Hatch page holder must resolve the opened machine.");
        }
    }

    private void setCurrentParallelAndSync(int value) {
        setCurrentParallel(value);
        sendServerSyncChanges();
    }

    /** Owns the contextual Parallel Hatch page state for exactly one menu opening. */
    private final class ParallelHatchPage implements IFancyUIProvider {

        private final MachineUIHolder holder;
        private final CombinedDirectionalFancyConfigurator directionalPage;

        private ParallelHatchPage(Player player, MachineUIHolder holder) {
            requireMatchingHolder(holder);
            this.holder = holder;
            directionalPage = new CombinedDirectionalFancyConfigurator(ParallelHatchPartMachine.this, player, holder);
        }

        @Override
        public UIElement createUIWidget(FancyMachineUIWidget shell) {
            if (holder.getMachine() != ParallelHatchPartMachine.this) {
                throw new IllegalStateException("Parallel Hatch page holder no longer resolves its opened machine.");
            }
            return createParallelInput();
        }

        @Override
        public IGuiTexture getTabIcon() {
            return GuiTextures.itemStack(getDefinition().getItem());
        }

        @Override
        public Component getTitle() {
            return Component.translatable(getDefinition().getDescriptionId());
        }

        @Override
        public int getPageWidth() {
            return PAGE_WIDTH;
        }

        @Override
        public int getPageHeight() {
            return PAGE_HEIGHT;
        }

        @Override
        public void attachSideTabs(TabsWidget tabs) {
            tabs.attachSubTab(directionalPage);
        }

        @Override
        public void attachTooltips(TooltipsPanel tooltipsPanel) {
            tooltipsPanel.attachTooltips(ParallelHatchPartMachine.this);
            getTraitHolder().getAllTraits().stream()
                    .filter(IFancyTooltip.class::isInstance)
                    .map(IFancyTooltip.class::cast)
                    .forEach(tooltipsPanel::attachTooltips);
        }

        @Override
        public List<Component> getTabTooltips() {
            return List.of(getTitle());
        }
    }

    @Override
    public boolean canShared(MultiblockControllerMachine controller, String structureName) {
        return false;
    }
}
