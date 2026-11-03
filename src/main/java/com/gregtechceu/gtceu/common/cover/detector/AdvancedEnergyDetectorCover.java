package com.gregtechceu.gtceu.common.cover.detector;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.ConfigCopyHelper;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.IEnergyInfoProvider;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.UITemplate;
import com.gregtechceu.gtceu.api.gui.element.GTLabelElement;
import com.gregtechceu.gtceu.api.gui.element.GTLongInputElement;
import com.gregtechceu.gtceu.api.gui.element.GTToggleButtonElement;
import com.gregtechceu.gtceu.api.gui.factory.CoverUIHelper;
import com.gregtechceu.gtceu.api.gui.factory.CoverUIProvider;
import com.gregtechceu.gtceu.api.gui.factory.UICoverHolder;
import com.gregtechceu.gtceu.api.sync_system.SyncFieldData;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.sync_system.rpc.RpcRegistry;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;

import static com.gregtechceu.gtceu.utils.RedstoneUtil.computeLatchedRedstoneBetweenValues;

public class AdvancedEnergyDetectorCover extends EnergyDetectorCover implements CoverUIProvider {

    private static final int DEFAULT_MIN_PERCENT = 33;
    private static final int DEFAULT_MAX_PERCENT = 66;
    private static final BigInteger LONG_MAX_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

    static {
        RpcRegistry.register(AdvancedEnergyDetectorConfigActions.INSTANCE);
    }

    @SaveField
    @SyncToClient
    @Getter
    public long minValue, maxValue;

    @SaveField
    @SyncToClient
    @Getter
    private boolean usePercent;

    private @Nullable GTLongInputElement minValueInput;
    private @Nullable GTLongInputElement maxValueInput;

    public AdvancedEnergyDetectorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        this.minValue = DEFAULT_MIN_PERCENT;
        this.maxValue = DEFAULT_MAX_PERCENT;
        this.usePercent = true;
    }

    @Override
    protected void update() {
        if (coverHolder.getOffsetTimer() % 20 != 0) return;

        IEnergyInfoProvider energyInfoProvider = getEnergyInfoProvider();
        if (energyInfoProvider == null) return;

        IEnergyInfoProvider.EnergyInfo energyInfo = energyInfoProvider.getEnergyInfo();
        boolean isBigInt = energyInfoProvider.supportsBigIntEnergyValues();

        if (isBigInt) {
            if (usePercent) {
                if (energyInfo.capacity().compareTo(BigInteger.ZERO) > 0) {
                    float ratio = GTMath.ratio(energyInfo.stored(), energyInfo.capacity());
                    setRedstoneSignalOutput(computeLatchedRedstoneBetweenValues(ratio * 100, maxValue,
                            minValue, isInverted(), redstoneSignalOutput));
                } else {
                    setRedstoneSignalOutput(isInverted() ? 15 : 0);
                }
            } else {
                setRedstoneSignalOutput(computeLatchedRedstoneBetweenValues(energyInfo.stored(),
                        BigInteger.valueOf(this.maxValue), BigInteger.valueOf(this.minValue),
                        isInverted(), redstoneSignalOutput));
            }
        } else {
            if (usePercent) {
                if (energyInfo.capacity().longValue() > 0) {
                    float ratio = energyInfo.stored().floatValue() / energyInfo.capacity().floatValue();
                    setRedstoneSignalOutput(computeLatchedRedstoneBetweenValues(ratio * 100, maxValue,
                            minValue, isInverted(), redstoneSignalOutput));
                } else {
                    setRedstoneSignalOutput(isInverted() ? 15 : 0);
                }
            } else {
                setRedstoneSignalOutput(computeLatchedRedstoneBetweenValues(energyInfo.stored().longValue(),
                        this.maxValue, this.minValue,
                        isInverted(), redstoneSignalOutput));
            }
        }
    }

    public void setMinValue(long minValue) {
        long clamped = Math.max(minValue, 0L);
        if (this.minValue != clamped) {
            this.minValue = clamped;
        }
    }

    public void setMaxValue(long maxValue) {
        long clamped = Math.max(maxValue, 0L);
        if (this.maxValue != clamped) {
            this.maxValue = clamped;
        }
    }

    public void setUsePercent(boolean usePercent) {
        setUsePercent(usePercent, false);
    }

    private void setUsePercentFromUI(boolean usePercent) {
        setUsePercent(usePercent, true);
    }

    private void setUsePercent(boolean usePercent, boolean convertValues) {
        var wasPercent = this.usePercent;
        if (this.usePercent != usePercent) {
            this.usePercent = usePercent;
            if (convertValues) {
                convertMinMaxValues(wasPercent);
            }
        }

        initializeMinMaxInputs();
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public boolean canCreateUI(Player player, UICoverHolder holder) {
        return holder.getCover() == this;
    }

    @Override
    public UI createUI(Player player, UICoverHolder holder) {
        UIElement root = new UIElement();
        UITemplate.setBounds(root, 0, 0, 176, 187);
        root.style(style -> style.backgroundTexture(GuiTextures.BACKGROUND));

        root.addChild(createLabel(10, 5, 156, 10, "cover.advanced_energy_detector.label"));
        root.addChild(createLabel(10, 55, 25, 10, "cover.advanced_energy_detector.min"));
        root.addChild(createLabel(10, 80, 25, 10, "cover.advanced_energy_detector.max"));
        minValueInput = new GTLongInputElement(40, 50, 126, 20, this::getMinValue,
                value -> setMinValue(player, holder, value));
        maxValueInput = new GTLongInputElement(40, 75, 126, 20, this::getMaxValue,
                value -> setMaxValue(player, holder, value));
        initializeMinMaxInputs();
        root.addChild(minValueInput);
        root.addChild(maxValueInput);
        root.addChild(new GTToggleButtonElement(9, 20, 20, 20, GuiTextures.INVERT_REDSTONE_BUTTON,
                this::isInverted, inverted -> setInverted(player, holder, inverted))
                .isMultiLang()
                .setTooltipText("cover.advanced_energy_detector.invert"));
        root.addChild(new GTToggleButtonElement(147, 20, 20, 20, GuiTextures.ENERGY_DETECTOR_COVER_MODE_BUTTON,
                this::isUsePercent, usePercent -> setUsePercent(player, holder, usePercent))
                .isMultiLang()
                .setTooltipText("cover.advanced_energy_detector.use_percent"));
        root.addChild(UITemplate.bindPlayerInventory(player.getInventory(), GuiTextures.SLOT, 7, 105, true));
        return UI.of(root);
    }

    private GTLabelElement createLabel(int x, int y, int width, int height, String text) {
        GTLabelElement label = new GTLabelElement(x, y, width, height, text, true);
        label.textStyle(style -> style
                .textColor(0x404040)
                .textShadow(false)
                .textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER));
        return label;
    }

    private void setMinValue(Player player, UICoverHolder holder, long value) {
        setMinValue(value);
        sendConfigAction(player, holder);
    }

    private void setMaxValue(Player player, UICoverHolder holder, long value) {
        setMaxValue(value);
        sendConfigAction(player, holder);
    }

    private void setUsePercent(Player player, UICoverHolder holder, boolean usePercent) {
        setUsePercentFromUI(usePercent);
        sendConfigAction(player, holder);
    }

    private void setInverted(Player player, UICoverHolder holder, boolean inverted) {
        setInverted(inverted);
        sendConfigAction(player, holder);
    }

    private void sendConfigAction(Player player, UICoverHolder holder) {
        if (player.level().isClientSide()) {
            CoverUIHelper.sendAction(holder, AdvancedEnergyDetectorConfigActions.createSetConfigAction(
                    getMinValue(), getMaxValue(), isUsePercent(), isInverted()));
        }
    }

    private void initializeMinMaxInputs() {
        if (minValueInput == null || maxValueInput == null)
            return;

        long max = usePercent ? 100L : getEnergyCapacity();
        minValueInput.setMin(0L).setMax(max);
        maxValueInput.setMin(0L).setMax(max);
    }

    private void convertMinMaxValues(boolean wasPercent) {
        long energyCapacity = getEnergyCapacity();
        if (usePercent && !wasPercent) {
            setMinValue(energyToPercent(minValue, energyCapacity));
            setMaxValue(energyToPercent(maxValue, energyCapacity));
        } else if (!usePercent && wasPercent) {
            setMinValue(percentToEnergy(minValue, energyCapacity));
            setMaxValue(percentToEnergy(maxValue, energyCapacity));
        }
    }

    private long getEnergyCapacity() {
        IEnergyInfoProvider energyInfoProvider = getEnergyInfoProvider();
        if (energyInfoProvider == null) {
            GTCEu.LOGGER.error("Advanced energy detector cover has no energy info provider at {}.",
                    coverHolder.getBlockPos());
            throw new IllegalStateException("Advanced energy detector cover has no energy info provider.");
        }

        BigInteger energyCapacity = energyInfoProvider.getEnergyInfo().capacity();
        if (energyCapacity.compareTo(LONG_MAX_VALUE) > 0) {
            return Long.MAX_VALUE;
        }
        return energyCapacity.longValue();
    }

    private static long energyToPercent(long value, long energyCapacity) {
        if (energyCapacity <= 0) {
            return 0;
        }
        return GTMath.clamp((long) (((double) value / energyCapacity) * 100), 0, 100);
    }

    private static long percentToEnergy(long value, long energyCapacity) {
        if (energyCapacity <= 0) {
            return 0;
        }
        return GTMath.clamp((long) Math.ceil((value / 100.0) * energyCapacity), 0, energyCapacity);
    }

    @Override
    public DataComponentMap copyConfig(HolderLookup.Provider registries) {
        return ConfigCopyHelper.withFields(super.copyConfig(registries), fields -> fields
                .put(SyncFieldData.key("min"),
                        ConfigCopyHelper.longValue(minValue))
                .put(SyncFieldData.key("max"),
                        ConfigCopyHelper.longValue(maxValue))
                .put(SyncFieldData.key("percent"),
                        ConfigCopyHelper.booleanValue(usePercent)));
    }

    @Override
    public void pasteConfig(ServerPlayer player, HolderLookup.Provider registries, DataComponentMap config) {
        setMinValue(ConfigCopyHelper.getLong(config, "min"));
        setMaxValue(ConfigCopyHelper.getLong(config, "max"));
        setUsePercent(ConfigCopyHelper.getBoolean(config, "percent"));
        super.pasteConfig(player, registries, config);
    }
}
