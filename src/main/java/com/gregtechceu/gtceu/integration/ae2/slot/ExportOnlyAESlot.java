package com.gregtechceu.gtceu.integration.ae2.slot;

import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec;
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec.Context;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;

import appeng.api.stacks.GenericStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * An export only slot to hold {@link appeng.api.stacks.GenericStack}
 */
public abstract class ExportOnlyAESlot implements IConfigurableSlot {

    static {
        ContextualFieldCodec<ExportOnlyAESlot> codec = ExportOnlyAESlot::codec;
        FieldCodecs.registerContextual(ExportOnlyAESlot.class, codec);
    }

    /** Replaces a complete decoded slot state without exposing a half-updated state to its listener. */
    public static <T extends ExportOnlyAESlot> Codec<T> codec(Context<T> context) {
        return RecordCodecBuilder.create(instance -> instance.group(
                GenericStack.CODEC.optionalFieldOf("config").forGetter((T slot) -> Optional.ofNullable(slot.config)),
                GenericStack.CODEC.optionalFieldOf("stock").forGetter((T slot) -> Optional.ofNullable(slot.stock)))
                .apply(instance, (config, stock) -> {
                    T slot = Objects.requireNonNull(context.currentValue(), "AE export slot must be initialized");
                    GenericStack requestedConfig = config.orElse(null);
                    GenericStack requestedStock = stock.orElse(null);
                    if (!Objects.equals(slot.config, requestedConfig) || !Objects.equals(slot.stock, requestedStock)) {
                        slot.config = requestedConfig;
                        slot.stock = requestedStock;
                        slot.onContentsChanged.run();
                    }
                    return slot;
                }));
    }

    @Getter
    @Setter
    protected Runnable onContentsChanged = () -> {};

    @Getter
    @Setter
    @Nullable
    protected GenericStack config;
    @Getter
    @Setter
    @Nullable
    protected GenericStack stock;

    public ExportOnlyAESlot(@Nullable GenericStack config, @Nullable GenericStack stock) {
        this.config = config;
        this.stock = stock;
    }

    public ExportOnlyAESlot() {
        this(null, null);
    }

    @Nullable
    public GenericStack requestStack() {
        if (this.stock != null && this.stock.amount() <= 0) {
            this.stock = null;
        }
        if (this.config == null || (this.stock != null && !this.config.what().matches(this.stock))) {
            return null;
        }
        if (this.stock == null) {
            return copy(this.config);
        }
        if (this.stock.amount() <= this.config.amount()) {
            return copy(this.config, this.config.amount() - this.stock.amount());
        }
        return null;
    }

    @Nullable
    public GenericStack exceedStack() {
        if (this.stock != null && this.stock.amount() <= 0) {
            this.stock = null;
        }
        if (this.config == null && this.stock != null) {
            return copy(this.stock);
        }
        if (this.config != null && this.stock != null) {
            if (this.config.what().matches(this.stock) && this.config.amount() < this.stock.amount()) {
                return copy(this.stock, this.stock.amount() - this.config.amount());
            }
            if (!this.config.what().matches(this.stock)) {
                return copy(this.stock);
            }
        }
        return null;
    }

    protected abstract void addStack(GenericStack stack);

    public static GenericStack copy(GenericStack stack) {
        return new GenericStack(stack.what(), stack.amount());
    }

    public static GenericStack copy(GenericStack stack, long amount) {
        return new GenericStack(stack.what(), amount);
    }
}
