package com.gregtechceu.gtceu.integration.ae2.machine.trait;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.api.machine.trait.MachineTraitType;
import com.gregtechceu.gtceu.api.sync_system.ContextualFieldCodec;
import com.gregtechceu.gtceu.api.sync_system.FieldCodecs;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.integration.ae2.sync.GridNodeCodec;

import net.minecraft.core.Direction;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;
import com.mojang.serialization.Codec;

public class GridNodeHostTrait extends MachineTrait implements IGridConnectedBlockEntity {

    static {
        FieldCodecs.registerContextual(IManagedGridNode.class, new ContextualFieldCodec<IManagedGridNode>() {

            @Override
            public Codec<IManagedGridNode> codec(Context<IManagedGridNode> context) {
                IManagedGridNode node = context.currentValue();
                return GridNodeCodec.of(node == null ? ((GridNodeHostTrait) context.holder()).getMainNode() : node);
            }
        });
    }

    public static final MachineTraitType<GridNodeHostTrait> TYPE = new MachineTraitType<>(GridNodeHostTrait.class);

    @Override
    public MachineTraitType<GridNodeHostTrait> getTraitType() {
        return TYPE;
    }

    @SaveField
    private final IManagedGridNode proxy;

    public GridNodeHostTrait(MetaMachine machine) {
        super();
        this.proxy = GridHelper.createManagedNode(this, BlockEntityNodeListener.INSTANCE)
                .setInWorldNode(true)
                .setVisualRepresentation(machine.getDefinition().getItem());
    }

    public void init() {
        this.proxy.create(getLevel(), getBlockPos());
    }

    @Override
    public IManagedGridNode getMainNode() {
        return proxy;
    }

    @Override
    public void saveChanges() {}

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }
}
