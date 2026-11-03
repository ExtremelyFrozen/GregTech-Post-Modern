package com.gregtechceu.gtceu.api.misc.virtualregistry.entries;

import com.gregtechceu.gtceu.api.misc.virtualregistry.EntryTypes;
import com.gregtechceu.gtceu.api.misc.virtualregistry.VirtualEntry;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import org.jetbrains.annotations.NotNull;

public class VirtualTank extends VirtualEntry {

    public static final int DEFAULT_CAPACITY = 160_000; // 160B for per second transfer
    @NotNull
    @SaveField
    @SyncToClient
    private final CustomFluidTank fluidTank;
    @SaveField
    @SyncToClient
    private int capacity;

    public VirtualTank(int capacity) {
        this.capacity = capacity;
        fluidTank = new CustomFluidTank(this.capacity);
    }

    public VirtualTank() {
        this(DEFAULT_CAPACITY);
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    @Override
    protected void onStateDecoded() {
        super.onStateDecoded();
        fluidTank.setCapacity(capacity);
    }

    @Override
    public EntryTypes<VirtualTank> getType() {
        return EntryTypes.ENDER_FLUID;
    }

    public void setFluid(FluidStack fluid) {
        this.fluidTank.setFluid(fluid);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof VirtualTank other)) return false;
        return this.fluidTank == other.fluidTank;
    }

    @Override
    public void copyFrom(VirtualEntry source) {
        super.copyFrom(source);
        VirtualTank tank = (VirtualTank) source;
        this.capacity = tank.capacity;
        this.fluidTank.setCapacity(this.capacity);
        setFluid(tank.fluidTank.getFluid().copy());
    }

    @Override
    public boolean canRemove() {
        return super.canRemove() && this.fluidTank.isEmpty();
    }
}
