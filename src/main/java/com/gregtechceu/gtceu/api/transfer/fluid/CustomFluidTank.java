package com.gregtechceu.gtceu.api.transfer.fluid;

import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class CustomFluidTank extends FluidTank implements IFluidHandlerModifiable {

    @Getter
    @Setter
    protected @NotNull Runnable onContentsChanged = () -> {};

    public CustomFluidTank(int capacity) {
        this(capacity, e -> true);
    }

    public CustomFluidTank(int capacity, Predicate<FluidStack> validator) {
        super(capacity, validator);
    }

    public CustomFluidTank(FluidStack stack) {
        super(stack.getAmount());
        setFluid(stack);
    }

    @Override
    protected void onContentsChanged() {
        onContentsChanged.run();
    }

    @Override
    public void setFluidInTank(int tank, FluidStack stack) {
        setFluid(stack);
    }

    @Override
    public void setFluid(FluidStack stack) {
        super.setFluid(stack);
        this.onContentsChanged();
    }

    public static Codec<CustomFluidTank> codec(ContextualFieldCodec.Context<CustomFluidTank> context) {
        return context.currentValue().codec();
    }

    public Codec<CustomFluidTank> codec() {
        return FluidStack.OPTIONAL_CODEC.xmap(stack -> {
            setFluid(stack);
            return this;
        }, tank -> tank.getFluid().copy());
    }
}
