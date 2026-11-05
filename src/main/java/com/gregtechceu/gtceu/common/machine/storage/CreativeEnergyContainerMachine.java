package com.gregtechceu.gtceu.common.machine.storage;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.ILaserContainer;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTButtonElement;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTStringSelectorElement;
import com.gregtechceu.gtceu.api.gui.element.GTTextFieldElement;
import com.gregtechceu.gtceu.api.gui.factory.MachineUIHolder;
import com.gregtechceu.gtceu.api.gui.texture.IGuiTexture;
import com.gregtechceu.gtceu.api.machine.TieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IUIMachine;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldChangeListener;
import com.gregtechceu.gtceu.api.sync_system.annotations.ServerFieldNormalizer;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncBoth;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RPCMethod;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Arrays;

public class CreativeEnergyContainerMachine extends TieredMachine
                                            implements ILaserContainer, IUIMachine {

    @SaveField
    @SyncToClient
    private long voltage = 0;
    @SaveField
    @SyncBoth
    private int amps = 1;
    @SaveField
    @SyncToClient
    private int setTier = 0;
    @SaveField
    @SyncBoth
    private boolean active = false;
    @SaveField
    @SyncBoth
    private boolean source = true;
    @SaveField
    private long energyIOPerSec = 0;
    @SyncToClient
    private long lastAverageEnergyIOPerTick = 0;
    private long ampsReceived = 0;
    private boolean doExplosion = false;

    public CreativeEnergyContainerMachine(BlockEntityCreationInfo info) {
        super(info, GTValues.MAX);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////

    @Override
    public void onLoad() {
        super.onLoad();
        subscribeServerTick(this::updateEnergyTick);
    }

    //////////////////////////////////////
    // ********** MISC ***********//
    //////////////////////////////////////

    protected void updateEnergyTick() {
        if (getOffsetTimer() % 20 == 0) {
            this.setIOSpeed(energyIOPerSec / 20);
            energyIOPerSec = 0;
            if (doExplosion) {
                getLevel().explode(null, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5,
                        getBlockPos().getZ() + 0.5,
                        1, Level.ExplosionInteraction.NONE);
                doExplosion = false;
            }
        }
        ampsReceived = 0;
        if (!active || !source || voltage <= 0 || amps <= 0) return;
        long ampsUsed = 0;
        for (var facing : GTUtil.DIRECTIONS) {
            var opposite = facing.getOpposite();
            IEnergyContainer container = GTCapabilityHelper.getEnergyContainer(getLevel(),
                    getBlockPos().relative(facing),
                    opposite);
            // Try to get laser capability
            if (container == null)
                container = GTCapabilityHelper.getLaser(getLevel(), getBlockPos().relative(facing), opposite);

            if (container != null && container.inputsEnergy(opposite) && container.getEnergyCanBeInserted() > 0) {
                ampsUsed += container.acceptEnergyFromNetwork(opposite, voltage, amps - ampsUsed);
                if (ampsUsed >= amps) {
                    break;
                }
            }
        }
        energyIOPerSec += ampsUsed * voltage;
    }

    @Override
    public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
        if (source || !active || ampsReceived >= amps) {
            return 0;
        }
        if (voltage > this.voltage) {
            if (doExplosion)
                return 0;
            doExplosion = true;
            return Math.min(amperage, getInputAmperage() - ampsReceived);
        }
        long amperesAccepted = Math.min(amperage, getInputAmperage() - ampsReceived);
        if (amperesAccepted > 0) {
            ampsReceived += amperesAccepted;
            energyIOPerSec += amperesAccepted * voltage;
            return amperesAccepted;
        }
        return 0;
    }

    @Override
    public boolean inputsEnergy(Direction side) {
        return !source;
    }

    @Override
    public boolean outputsEnergy(Direction side) {
        return source;
    }

    @Override
    public long changeEnergy(long differenceAmount) {
        if (source || !active) {
            return 0;
        }
        energyIOPerSec += differenceAmount;
        return differenceAmount;
    }

    @Override
    public long getEnergyStored() {
        return 69;
    }

    @Override
    public long getEnergyCapacity() {
        return 420;
    }

    @Override
    public long getInputAmperage() {
        return source ? 0 : amps;
    }

    @Override
    public long getInputVoltage() {
        return source ? 0 : voltage;
    }

    @Override
    public long getOutputVoltage() {
        return source ? voltage : 0;
    }

    @Override
    public long getOutputAmperage() {
        return source ? amps : 0;
    }

    public void setIOSpeed(long energyIOPerSec) {
        if (this.lastAverageEnergyIOPerTick != energyIOPerSec) {
            this.lastAverageEnergyIOPerTick = energyIOPerSec;
            syncDataHolder.markClientSyncFieldDirty("lastAverageEnergyIOPerTick");
        }
    }

    @RPCMethod
    public void setCreativeEnergyVoltage(long voltage) {
        this.voltage = voltage;
        this.setTier = GTUtil.getTierByVoltage(voltage);
        syncDataHolder.markClientSyncFieldDirty("voltage");
        syncDataHolder.markClientSyncFieldDirty("setTier");
    }

    private void setAmps(int amps) {
        int normalizedAmps = normalizeAmps(amps);
        if (this.amps == normalizedAmps) {
            return;
        }
        this.amps = normalizedAmps;
    }

    @ServerFieldNormalizer(fieldName = "amps")
    private int normalizeAmps(int amps) {
        if (amps < 0) {
            throw new IllegalArgumentException("Creative energy amperage cannot be negative.");
        }
        return amps;
    }

    private void setActive(boolean active) {
        if (this.active == active) {
            return;
        }
        this.active = active;
    }

    private void setSource(boolean source) {
        if (this.source == source) {
            return;
        }
        this.source = source;
        applySourceMode(source);
    }

    @ServerFieldChangeListener(fieldName = "source")
    private void onSourceChanged(boolean oldSource, boolean newSource) {
        applySourceMode(newSource);
    }

    private void applySourceMode(boolean source) {
        if (source) {
            this.voltage = 0;
            this.amps = 0;
            this.setTier = 0;
        } else {
            this.voltage = GTValues.V[14];
            this.amps = Integer.MAX_VALUE;
            this.setTier = 14;
        }
    }

    @RPCMethod
    public void setCreativeEnergyTier(int tier) {
        setTier = tier;
        voltage = GTValues.VEX[tier];
        syncDataHolder.markClientSyncFieldDirty("setTier");
        syncDataHolder.markClientSyncFieldDirty("voltage");
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public boolean canCreateUI(Player player, MachineUIHolder holder) {
        return holder.getMachine() == this;
    }

    @Override
    public UI createUI(Player player, MachineUIHolder holder) {
        UIElement root = new UIElement();
        UITemplate.setBounds(root, 0, 0, 176, 166);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));
        root.addChild(createTierSelector(player, holder));
        root.addChild(createLabel(7, 32, 162, 10, Component.translatable("gtpm.creative.energy.voltage")));
        root.addChild(createVoltageField(player, holder));
        root.addChild(createLabel(7, 74, 162, 10, Component.translatable("gtpm.creative.energy.amperage")));
        root.addChild(createAmpsDecreaseButton());
        root.addChild(createAmpsField());
        root.addChild(createAmpsIncreaseButton());
        root.addChild(createAverageIOLabel());
        root.addChild(createActiveButton());
        root.addChild(createSourceButton());
        return UI.of(root);
    }

    private GTStringSelectorElement createTierSelector(Player player, MachineUIHolder holder) {
        return new GTStringSelectorElement(7, 7, 50, 20, Arrays.asList(GTValues.VNF),
                () -> GTValues.VNF[setTier], tierName -> setTier(player, holder, tierName));
    }

    private GTLabelElement createLabel(int x, int y, int width, int height, Component text) {
        GTLabelElement label = new GTLabelElement(x, y, width, height, text);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private GTTextFieldElement createVoltageField(Player player, MachineUIHolder holder) {
        GTTextFieldElement field = new GTTextFieldElement(9, 47, 152, 16) {

            @Override
            public void screenTick() {
                if (!isFocused()) {
                    setText(Long.toString(voltage), false);
                }
                super.screenTick();
            }
        };
        field.setNumbersOnlyLong(0L, Long.MAX_VALUE);
        field.setText(Long.toString(voltage), false);
        field.textFieldStyle(style -> style
                .textColor(0x404040)
                .textShadow(false));
        field.setTextResponder(value -> setVoltage(player, holder, value));
        return field;
    }

    private GTTextFieldElement createAmpsField() {
        GTTextFieldElement field = new GTTextFieldElement(31, 89, 114, 16) {

            @Override
            public void screenTick() {
                if (!isFocused()) {
                    setText(Integer.toString(amps), false);
                }
                super.screenTick();
            }
        };
        field.setNumbersOnlyInt(0, Integer.MAX_VALUE);
        field.setText(Integer.toString(amps), false);
        field.textFieldStyle(style -> style
                .textColor(0x404040)
                .textShadow(false));
        field.setTextResponder(this::setAmpsAndSync);
        return field;
    }

    private GTButtonElement createAmpsDecreaseButton() {
        return new GTButtonElement(7, 87, 20, 20,
                GuiTextures.group(GuiTextures.BUTTON, GuiTextures.text("-")),
                event -> setAmpsAndSync(Math.max(0, amps - 1)));
    }

    private GTButtonElement createAmpsIncreaseButton() {
        return new GTButtonElement(149, 87, 20, 20,
                GuiTextures.group(GuiTextures.BUTTON, GuiTextures.text("+")),
                event -> {
                    if (amps < Integer.MAX_VALUE) {
                        setAmpsAndSync(amps + 1);
                    }
                });
    }

    private GTLabelElement createAverageIOLabel() {
        GTLabelElement label = new GTLabelElement(7, 110, 162, 10) {

            @Override
            public void screenTick() {
                setValue(createAverageIOText());
                super.screenTick();
            }
        };
        label.setValue(createAverageIOText());
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private Component createAverageIOText() {
        return Component.literal("Average Energy I/O per tick: " + lastAverageEnergyIOPerTick);
    }

    private GTButtonElement createActiveButton() {
        return new GTButtonElement(7, 139, 77, 20, createActiveButtonTexture(),
                event -> setActiveAndSync(!active)) {

            @Override
            public void screenTick() {
                setButtonTexture(createActiveButtonTexture());
                super.screenTick();
            }
        };
    }

    private GTButtonElement createSourceButton() {
        return new GTButtonElement(85, 139, 77, 20, createSourceButtonTexture(),
                event -> setSourceAndSync(!source)) {

            @Override
            public void screenTick() {
                setButtonTexture(createSourceButtonTexture());
                super.screenTick();
            }
        };
    }

    private IGuiTexture createActiveButtonTexture() {
        return GuiTextures.group(GuiTextures.BUTTON,
                GuiTextures.text(active ? "gtpm.creative.activity.on" : "gtpm.creative.activity.off"));
    }

    private IGuiTexture createSourceButtonTexture() {
        return GuiTextures.group(GuiTextures.BUTTON,
                GuiTextures.text(source ? "gtpm.creative.energy.source" : "gtpm.creative.energy.sink"));
    }

    private void setVoltage(Player player, MachineUIHolder holder, String value) {
        if (value.isEmpty()) {
            return;
        }
        long parsedValue;
        try {
            parsedValue = Long.parseLong(value);
        } catch (NumberFormatException e) {
            GTCEu.LOGGER.error("Invalid creative energy voltage input: {}", value, e);
            throw e;
        }
        setCreativeEnergyVoltage(parsedValue);
        if (player.level().isClientSide()) {
            holder.getMachine().rpcToServer("setCreativeEnergyVoltage", parsedValue);
        }
    }

    private void setAmpsAndSync(String value) {
        if (value.isEmpty()) {
            return;
        }
        int parsedValue;
        try {
            parsedValue = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            GTCEu.LOGGER.error("Invalid creative energy amperage input: {}", value, e);
            throw e;
        }
        setAmpsAndSync(parsedValue);
    }

    private void setAmpsAndSync(int value) {
        setAmps(value);
        sendServerSyncChanges();
    }

    private void setTier(Player player, MachineUIHolder holder, String tierName) {
        int tierIndex = getTierIndex(tierName);
        setCreativeEnergyTier(tierIndex);
        if (player.level().isClientSide()) {
            holder.getMachine().rpcToServer("setCreativeEnergyTier", tierIndex);
        }
    }

    private void setActiveAndSync(boolean active) {
        setActive(active);
        sendServerSyncChanges();
    }

    private void setSourceAndSync(boolean source) {
        setSource(source);
        sendServerSyncChanges();
    }

    private static int getTierIndex(String tierName) {
        for (int index = 0; index < GTValues.VNF.length; index++) {
            if (GTValues.VNF[index].equals(tierName)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Unknown creative energy voltage tier: " + tierName);
    }
}
