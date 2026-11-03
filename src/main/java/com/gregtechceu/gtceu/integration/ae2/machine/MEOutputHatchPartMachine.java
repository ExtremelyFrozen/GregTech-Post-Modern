package com.gregtechceu.gtceu.integration.ae2.machine;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTFluidSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTImageElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUI;
import com.gregtechceu.gtceu.api.gui.fancy.FancyTabs;
import com.gregtechceu.gtceu.api.gui.fancy.FancyTooltipsPanel;
import com.gregtechceu.gtceu.api.gui.fancy.FancyUIProvider;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyTooltip;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.DirectionalConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.WorkingEnabledConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.FancyPartUIProvider;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.integration.ae2.utils.KeyStorage;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualItemHeightMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.VirtualScrollerView;
import com.lowdragmc.lowdraglib2.integration.xei.IngredientIO;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEFluidKey;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MEOutputHatchPartMachine extends MEHatchPartMachine
                                      implements FancyPartUIProvider {

    private static final int PAGE_WIDTH = 170;
    private static final int PAGE_HEIGHT = 74;

    @SaveField
    @SyncToClient
    private KeyStorage internalBuffer; // Do not use KeyCounter, use our simple implementation

    public MEOutputHatchPartMachine(BlockEntityCreationInfo info) {
        super(info, IO.OUT);
    }

    /////////////////////////////////
    // ***** Machine LifeCycle ****//
    /////////////////////////////////

    @Override
    protected NotifiableFluidTank createTank(int initialCapacity, int slots) {
        this.internalBuffer = new KeyStorage();
        return new InaccessibleInfiniteTank(this);
    }

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        var grid = getMainNode().getGrid();
        if (grid != null && !internalBuffer.isEmpty()) {
            for (var entry : internalBuffer) {
                grid.getStorageService().getInventory().insert(entry.getKey(), entry.getLongValue(),
                        Actionable.MODULATE, actionSource);
            }
        }
    }

    /////////////////////////////////
    // ********** Sync ME *********//
    /////////////////////////////////

    @Override
    protected boolean shouldSubscribe() {
        return super.shouldSubscribe() && !internalBuffer.storage.isEmpty();
    }

    @Override
    protected void autoIO() {
        if (!this.shouldSyncME()) return;
        if (this.updateMEStatus()) {
            var grid = getMainNode().getGrid();
            if (grid != null && !internalBuffer.isEmpty()) {
                internalBuffer.insertInventory(grid.getStorageService().getInventory(), actionSource);
            }
            this.updateTankSubscription();
        }
    }

    ///////////////////////////////
    // ********** GUI ***********//
    ///////////////////////////////

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this &&
                holder.getMachineDefinitionId().equals(GTAEMachines.FLUID_EXPORT_HATCH_ME.getId()) &&
                getDefinition() == GTAEMachines.FLUID_EXPORT_HATCH_ME;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        FancyUIProvider page = createPage(player, holder);
        return UI.of(new FancyMachineUI(page, player.getInventory(), holder,
                page.getPageWidth(), page.getPageHeight()));
    }

    /** Creates one opening-scoped page after validating the exact fluid output hatch holder and definition. */
    FancyUIProvider createPage(Player player, MachineUIHolder holder) {
        return new MEFluidOutputHatchFancyPage(player, holder);
    }

    @Override
    public FancyUIProvider createFancyPage(Player player, MachineUIHolder holder) {
        return createPage(player, holder);
    }

    private UIElement createMainElement() {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        GTLabelElement status = new GTLabelElement(5, 0, 160, 10, networkStatus()) {

            @Override
            public void screenTick() {
                setValue(MEOutputHatchPartMachine.this.networkStatus());
                super.screenTick();
            }
        };
        status.setId("me_network_status");
        root.addChild(status);
        GTLabelElement title = new GTLabelElement(5, 10, 160, 10,
                Component.translatable("gtpm.gui.waiting_list"));
        title.setId("me_output_waiting_list_label");
        root.addChild(title);
        root.addChild(createWaitingList());
        return root;
    }

    private UIElement createWaitingList() {
        var view = new VirtualScrollerView<KeyStorage.Entry>() {

            private List<KeyStorage.Entry> displayed = List.of();

            @Override
            public void screenTick() {
                var current = internalBuffer.getEntries();
                if (current != displayed) {
                    displayed = current;
                    setItems(current);
                }
                super.screenTick();
            }
        };
        UITemplate.setBounds(view, 5, 20, 158, 54);
        view.scrollerStyle(style -> style.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER)
                .adaptiveWidth(false).adaptiveHeight(false).minScrollPixel(18).maxScrollPixel(18));
        view.virtualScrollerViewStyle(style -> style.itemHeightMode(VirtualItemHeightMode.FIXED)
                .estimatedItemHeight(18).overscanPixels(18));
        view.viewPort.layout(layout -> layout.paddingAll(0));
        view.viewPort.style(style -> style.backgroundTexture(IGuiTexture.EMPTY));
        view.setItemUIProvider(this::createWaitingRow);
        return view;
    }

    private UIElement createWaitingRow(KeyStorage.Entry entry) {
        AEFluidKey fluidKey = (AEFluidKey) entry.key();

        UIElement row = UITemplate.setBounds(new UIElement(), 0, 0, 158, 18);
        GTImageElement amountBackground = new GTImageElement(18, 0, 140, 18,
                GuiTextures.NUMBER_BACKGROUND);
        amountBackground.setAllowHitTest(false);
        row.addChild(amountBackground);

        GTFluidSlotElement fluid = new GTFluidSlotElement()
                .setFluid(fluidKey.toStack(1))
                .setCapacity(1)
                .setShowAmount(false)
                .setAllowClickFilled(false)
                .setAllowClickDrained(false)
                .setIngredientIO(IngredientIO.NONE)
                .setBackgroundTexture(GuiTextures.FLUID_SLOT)
                .setOnAddedTooltips((slot, tooltips) -> tooltips.add(
                        Component.literal(String.format("%,d mB", entry.amount()))));
        row.addChild(UITemplate.setBounds(fluid, 0, 0, 18, 18));

        GTLabelElement amount = new GTLabelElement(21, 0, 134, 18,
                Component.literal(String.format("x%,d", entry.amount())))
                .setTextColor(-1)
                .setTextShadow(true)
                .setTextAlignHorizontal(Horizontal.LEFT)
                .setTextAlignVertical(Vertical.CENTER);
        amount.setAllowHitTest(false);
        row.addChild(amount);
        return row;
    }

    private Component networkStatus() {
        return Component.translatable(isOnline ?
                "gtpm.gui.me_network.online" : "gtpm.gui.me_network.offline");
    }

    private class InaccessibleInfiniteTank extends NotifiableFluidTank {

        FluidStorageDelegate storage;

        public InaccessibleInfiniteTank(MetaMachine holder) {
            super(List.of(new FluidStorageDelegate()), IO.OUT, IO.NONE);
            internalBuffer.setOnContentsChanged(this::onContentsChanged);
            storage = (FluidStorageDelegate) getStorages()[0];
            allowSameFluids = true;
        }

        @Override
        public int getTanks() {
            return 128;
        }

        @Override
        public List<Object> getContents() {
            return Collections.emptyList();
        }

        @Override
        public double getTotalContentAmount() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public void setFluidInTank(int tank, FluidStack fluidStack) {
            // NO-OP
        }

        @Override
        public int getTankCapacity(int tank) {
            return storage.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public List<SizedFluidIngredient> handleRecipeInner(IO io, GTRecipe recipe, List<SizedFluidIngredient> left,
                                                            boolean simulate) {
            if (io != IO.OUT) return left;
            FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
            for (var it = left.listIterator(); it.hasNext();) {
                var ingredient = it.next();
                if (ingredient.ingredient().hasNoFluids()) {
                    it.remove();
                    continue;
                }

                var fluids = ingredient.getFluids();
                if (fluids.length == 0 || fluids[0].isEmpty()) {
                    it.remove();
                    continue;
                }
                FluidStack output = fluids[0];
                int remainingAmount = ingredient.amount() - storage.fill(output, action);

                if (remainingAmount > 0) it.set(new SizedFluidIngredient(ingredient.ingredient(), remainingAmount));
                else it.remove();
            }
            return left;
        }
    }

    private class FluidStorageDelegate extends CustomFluidTank {

        public FluidStorageDelegate() {
            super(0);
        }

        @Override
        public int getCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void setFluid(FluidStack fluid) {
            // NO-OP
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            var key = AEFluidKey.of(resource);
            int amount = resource.getAmount();
            int oldValue = GTMath.saturatedCast(internalBuffer.storage.getOrDefault(key, 0));
            int changeValue = Math.min(Integer.MAX_VALUE - oldValue, amount);
            if (changeValue > 0 && action.execute()) {
                internalBuffer.put(key, (long) oldValue + changeValue);
            }
            return changeValue;
        }

        @Override
        public boolean supportsFill(int tank) {
            return false;
        }

        @Override
        public boolean supportsDrain(int tank) {
            return false;
        }
    }

    /** One opening-scoped Fancy page shared by standalone and contextual part navigation. */
    private final class MEFluidOutputHatchFancyPage implements FancyUIProvider {

        private final Player player;
        private final MachineUIHolder holder;
        private final DirectionalConfigurator directionalPage;

        private MEFluidOutputHatchFancyPage(Player player, MachineUIHolder holder) {
            this.player = player;
            this.holder = holder;
            directionalPage = new DirectionalConfigurator(
                    MEOutputHatchPartMachine.this, player, holder);
        }

        @Override
        public UIElement createMainPage(FancyMachineUI shell) {
            return createMainElement();
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
        public void attachSideTabs(FancyTabs tabs) {
            tabs.attachSubTab(directionalPage);
        }

        @Override
        public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
            configuratorPanel.attachConfigurators(new WorkingEnabledConfigurator(
                    MEOutputHatchPartMachine.this, holder));
        }

        @Override
        public void attachTooltips(FancyTooltipsPanel tooltipsPanel) {
            tooltipsPanel.attachTooltips(MEOutputHatchPartMachine.this);
            for (var trait : getTraitHolder().getAllTraits()) {
                if (trait instanceof IFancyTooltip tooltip) {
                    tooltipsPanel.attachTooltips(tooltip);
                }
            }
        }

        @Override
        public List<Component> getTabTooltips() {
            return List.of(Component.translatable(getDefinition().getDescriptionId()));
        }

        @Override
        public PageGroupingData getPageGroupingData() {
            return new PageGroupingData("gtpm.multiblock.page_switcher.io.export", 2);
        }
    }
}
