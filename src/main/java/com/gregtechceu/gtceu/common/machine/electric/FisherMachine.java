package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IWorkable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTItemSlotElement;
import com.gregtechceu.gtceu.api.gui.element.GTToggleButtonElement;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncBoth;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.machine.trait.AutoOutputTrait;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.ISubscription;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

/**
 * @author h3tr
 * @date 2023/7/13
 * @implNote FisherMachine
 */
public class FisherMachine extends TieredEnergyMachine
                           implements IFancyUIMachine, IWorkable {

    private static final int SLOT_SIZE = 18;
    private static final int TEMPLATE_PADDING = 8;
    private static final int OUTPUT_SLOT_X_OFFSET = 24;
    private static final int TEMPLATE_SLOT_OFFSET = 4;
    private static final int TEMPLATE_CONTROL_COLUMN_WIDTH = 20;
    private static final int ENERGY_BAR_WIDTH = 18;
    private static final int ENERGY_BAR_HEIGHT = 60;
    private static final int ENERGY_GROUP_WIDTH = 18;
    private static final int ENERGY_GROUP_HEIGHT = 80;
    private static final int ENERGY_GROUP_X = 3;
    private static final int BATTERY_SLOT_X = 0;
    private static final int BATTERY_SLOT_Y = 61;
    private static final int PAGE_MIN_WIDTH = 172;
    private static final int PAGE_ENERGY_TEMPLATE_GAP = 4;
    private static final int BAIT_SLOT_X = 4;
    private static final int JUNK_BUTTON_X = 4;
    private static final int JUNK_BUTTON_BOTTOM_MARGIN = 4;

    @SaveField
    protected final NotifiableItemStackHandler cache;
    @Getter
    @Setter
    @SaveField
    protected boolean allowInputFromOutputSideItems;
    @SaveField
    protected final NotifiableItemStackHandler baitHandler;

    @Getter
    @SaveField
    protected final CustomItemStackHandler chargerInventory;
    @Nullable
    protected TickableSubscription batterySubs, fishingSubs;
    @Nullable
    protected ISubscription energySubs, baitSubs;
    private final long energyPerTick;

    private final int inventorySize;

    @Getter
    public final int maxProgress;

    @Getter
    @SaveField
    private int progress = 0;

    @Getter
    @SaveField
    @SyncToClient
    private boolean isWorkingEnabled = true;

    @Getter
    @SaveField
    private boolean active = false;
    public static final int WATER_CHECK_SIZE = 5;
    private static final ItemStack fishingRod = new ItemStack(Items.FISHING_ROD);
    private boolean hasWater = false;

    @Getter
    @SaveField
    @SyncBoth
    protected boolean junkEnabled = true;
    @SaveField
    @SyncToClient
    public final AutoOutputTrait autoOutput;

    public FisherMachine(BlockEntityCreationInfo info, int tier) {
        super(info, tier);
        this.inventorySize = (tier + 1) * (tier + 1);
        this.maxProgress = calcMaxProgress(tier);
        this.energyPerTick = GTValues.V[tier - 1];
        this.cache = attachTrait(new NotifiableItemStackHandler(inventorySize, IO.BOTH, IO.OUT));

        this.baitHandler = attachTrait(new NotifiableItemStackHandler(1, IO.BOTH, IO.IN));
        baitHandler.setFilter(item -> item.is(Items.STRING));

        this.chargerInventory = new CustomItemStackHandler();
        chargerInventory.setFilter(item -> GTCapabilityHelper.getElectricItem(item) != null ||
                (ConfigHolder.INSTANCE.compat.energy.nativeEUToFE &&
                        GTCapabilityHelper.getForgeEnergyItem(item) != null));

        autoOutput = attachTrait(AutoOutputTrait.ofItems(cache));
        environmentalExplosionTrait.setEnableEnvironmentalExplosions(false);
    }

    public void setWorkingEnabled(boolean enabled) {
        isWorkingEnabled = enabled;
    }

    public void setJunkEnabled(boolean enabled) {
        junkEnabled = enabled;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (isRemote()) return;
        energySubs = energyContainer.addChangedListener(() -> {
            this.updateBatterySubscription();
            this.updateFishingUpdateSubscription();
        });
        baitSubs = baitHandler.addChangedListener(this::updateFishingUpdateSubscription);
        chargerInventory.setOnContentsChanged(this::updateBatterySubscription);
        this.updateFishingUpdateSubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (energySubs != null) {
            energySubs.unsubscribe();
            energySubs = null;
        }
        if (baitSubs != null) {
            baitSubs.unsubscribe();
            baitSubs = null;
        }
    }

    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        chargerInventory.dropInventoryInWorld(getLevel(), getBlockPos());
    }

    public static int calcMaxProgress(int tier) {
        return (int) (800.0 - 170 * ((double) tier - 1.0) + (((double) Math.max(0, tier - 4) / 0.012)));
    }

    //////////////////////////////////////
    // ********* Logic **********//
    //////////////////////////////////////

    public void updateFishingUpdateSubscription() {
        if (drainEnergy(true) && this.baitHandler.getStackInSlot(0).is(Items.STRING) && isWorkingEnabled) {
            fishingSubs = subscribeServerTick(fishingSubs, this::fishingUpdate);
            active = true;
            return;
        } else if (fishingSubs != null) {
            fishingSubs.unsubscribe();
            fishingSubs = null;
            active = false;
        }
        progress = 0;
    }

    private void updateHasWater() {
        for (int x = 0; x < WATER_CHECK_SIZE; x++)
            for (int z = 0; z < WATER_CHECK_SIZE; z++) {
                BlockPos waterCheckPos = getBlockPos().below().offset(x - WATER_CHECK_SIZE / 2, 0,
                        z - WATER_CHECK_SIZE / 2);
                if (!getLevel().getBlockState(waterCheckPos).getFluidState().is(Fluids.WATER)) {
                    hasWater = false;
                    return;
                }
            }
        hasWater = true;
    }

    public void fishingUpdate() {
        if (this.getOffsetTimer() % maxProgress == 0L)
            updateHasWater();

        if (!hasWater) return;

        drainEnergy(false);
        if (progress >= maxProgress) {
            var lootTableRegistry = getLevel().registryAccess().registryOrThrow(Registries.LOOT_TABLE);
            LootTable lootTable = lootTableRegistry.get(BuiltInLootTables.FISHING);
            if (!this.junkEnabled) {
                lootTable = lootTableRegistry.get(BuiltInLootTables.FISHING_FISH);
            }

            FishingHook simulatedHook = new FishingHook(EntityType.FISHING_BOBBER, getLevel()) {

                public boolean isOpenWaterFishing() {
                    return true;
                }
            };

            LootParams lootContext = new LootParams.Builder((ServerLevel) getLevel())
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, simulatedHook)
                    .withParameter(LootContextParams.TOOL, fishingRod)
                    .withParameter(LootContextParams.ORIGIN,
                            new Vec3(getBlockPos().getX(), getBlockPos().getY(), getBlockPos().getZ()))
                    .create(LootContextParamSets.FISHING);

            NonNullList<ItemStack> generatedLoot = NonNullList.create();
            generatedLoot.addAll(lootTable.getRandomItems(lootContext));

            boolean useBait = false;
            for (ItemStack itemStack : generatedLoot)
                useBait |= tryFillCache(itemStack);

            if (useBait && junkEnabled)
                this.baitHandler.storage.extractItem(0, 1, false);
            else if (useBait)
                this.baitHandler.storage.extractItem(0, 2, false);
            updateFishingUpdateSubscription();
            progress = -1;
        }
        progress++;
    }

    private boolean tryFillCache(ItemStack stack) {
        for (int i = 0; i < cache.getSlots(); i++) {
            if (cache.insertItemInternal(i, stack, false).getCount() < stack.getCount()) {
                return true;
            }
        }
        return false;
    }

    public boolean drainEnergy(boolean simulate) {
        long resultEnergy = energyContainer.getEnergyStored() - energyPerTick;
        if (resultEnergy >= 0L && resultEnergy <= energyContainer.getEnergyCapacity()) {
            if (!simulate)
                energyContainer.removeEnergy(energyPerTick);
            return true;
        }
        return false;
    }

    protected void updateBatterySubscription() {
        if (energyContainer.dischargeOrRechargeEnergyContainers(chargerInventory, 0, true))
            batterySubs = subscribeServerTick(batterySubs, this::chargeBattery);
        else if (batterySubs != null) {
            batterySubs.unsubscribe();
            batterySubs = null;
        }
    }

    protected void chargeBattery() {
        if (!energyContainer.dischargeOrRechargeEnergyContainers(chargerInventory, 0, false))
            updateBatterySubscription();
    }

    //////////////////////////////////////
    // ********** GUI ***********//
    //////////////////////////////////////

    @Override
    public UIElement createUIWidget(FancyMachineUIWidget shell) {
        UIElement root = UITemplate.setBounds(new UIElement(), 0, 0,
                getPageWidth(), getPageHeight());

        UIElement energyGroup = UITemplate.setBounds(new UIElement(), ENERGY_GROUP_X, getEnergyGroupY(),
                getEnergyGroupWidth(), getEnergyGroupHeight());
        energyGroup.addChild(createEnergyBar());
        energyGroup.addChild(createBatterySlot(BATTERY_SLOT_X, BATTERY_SLOT_Y));
        root.addChild(energyGroup);

        UIElement template = UITemplate.setBounds(new UIElement(), getTemplateX(), getTemplateY(),
                getTemplateWidth(), getTemplateHeight());
        template.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND_INVERSE));

        int rowSize = getTemplateRowSize();
        for (int y = 0; y < rowSize; y++) {
            for (int x = 0; x < rowSize; x++) {
                int index = y * rowSize + x;
                template.addChild(createCacheSlot(index, OUTPUT_SLOT_X_OFFSET + x * SLOT_SIZE,
                        TEMPLATE_SLOT_OFFSET + y * SLOT_SIZE));
            }
        }

        template.addChild(createBaitSlot(BAIT_SLOT_X, getBaitSlotY()));
        template.addChild(createJunkButton(JUNK_BUTTON_X, getJunkButtonY()));
        root.addChild(template);
        return root;
    }

    @Override
    public int getPageWidth() {
        return Math.max(getEnergyGroupWidth() + getTemplateWidth() +
                PAGE_ENERGY_TEMPLATE_GAP + TEMPLATE_PADDING, PAGE_MIN_WIDTH);
    }

    @Override
    public int getPageHeight() {
        return Math.max(getTemplateHeight() + TEMPLATE_PADDING,
                getEnergyGroupHeight() + TEMPLATE_PADDING);
    }

    private GTItemSlotElement createCacheSlot(int index, int x, int y) {
        GTItemSlotElement slot = new GTItemSlotElement(cache.storage, index)
                .setCanTakeItems(true)
                .setCanPutItems(false)
                .setBackgroundTexture(GuiTextures.SLOT);
        return UITemplate.setBounds(slot, x, y, SLOT_SIZE, SLOT_SIZE);
    }

    private GTItemSlotElement createBaitSlot(int x, int y) {
        GTItemSlotElement slot = new GTItemSlotElement(baitHandler.storage, 0)
                .setCanTakeItems(true)
                .setCanPutItems(true)
                .setBackgroundTexture(GuiTextures.group(GuiTextures.SLOT, GuiTextures.STRING_SLOT_OVERLAY));
        return UITemplate.setBounds(slot, x, y, SLOT_SIZE, SLOT_SIZE);
    }

    private GTItemSlotElement createBatterySlot(int x, int y) {
        GTItemSlotElement slot = new GTItemSlotElement(chargerInventory, 0)
                .setCanPutItems(true)
                .setCanTakeItems(true)
                .setBackgroundTexture(GuiTextures.group(GuiTextures.SLOT, GuiTextures.CHARGER_OVERLAY))
                .setOnAddedTooltips((slotElement, tooltips) -> tooltips.addAll(
                        LangHandler.getMultiLang("gtpm.gui.charger_slot.tooltip",
                                GTValues.VNF[getTier()], GTValues.VNF[getTier()])));
        return UITemplate.setBounds(slot, x, y, SLOT_SIZE, SLOT_SIZE);
    }

    GTToggleButtonElement createJunkButton(int x, int y) {
        GTToggleButtonElement button = new GTToggleButtonElement(x, y, SLOT_SIZE, SLOT_SIZE,
                GuiTextures.itemStack(Items.NAME_TAG).scale(0.9F), this::isJunkEnabled,
                this::requestJunkEnabled)
                .setShouldUseBaseBackground();
        button.style(style -> style.tooltips(LangHandler.getMultiLang("gtpm.gui.fisher_mode.tooltip",
                GTValues.VNF[getTier()], GTValues.VNF[getTier()]).toArray(Component[]::new)));
        return button;
    }

    private void requestJunkEnabled(boolean enabled) {
        setJunkEnabled(enabled);
        if (isRemote()) {
            sendServerSyncChanges();
        }
    }

    private int getEnergyGroupWidth() {
        return ENERGY_GROUP_WIDTH;
    }

    private int getEnergyGroupHeight() {
        return ENERGY_GROUP_HEIGHT;
    }

    private int getEnergyGroupY() {
        return (getPageHeight() - getEnergyGroupHeight()) / 2;
    }

    private int getTemplateX() {
        return (getPageWidth() - getEnergyGroupWidth() - PAGE_ENERGY_TEMPLATE_GAP -
                getTemplateWidth()) / 2 + 2 + getEnergyGroupWidth() + 2;
    }

    private int getTemplateY() {
        return (getPageHeight() - getTemplateHeight()) / 2;
    }

    private int getTemplateWidth() {
        return getTemplateRowSize() * SLOT_SIZE + TEMPLATE_PADDING + TEMPLATE_CONTROL_COLUMN_WIDTH;
    }

    private int getTemplateHeight() {
        return getTemplateRowSize() * SLOT_SIZE + TEMPLATE_PADDING;
    }

    private int getTemplateRowSize() {
        return (int) Math.sqrt(inventorySize);
    }

    private int getBaitSlotY() {
        return (getTemplateHeight() - SLOT_SIZE) / 2;
    }

    private int getJunkButtonY() {
        return getTemplateHeight() - SLOT_SIZE - JUNK_BUTTON_BOTTOM_MARGIN;
    }
}
