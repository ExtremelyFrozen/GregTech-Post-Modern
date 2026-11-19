package com.gregtechceu.gtceu.api.gui.sync;

import com.lowdragmc.lowdraglib2.syncdata.accessor.direct.IDirectAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.ref.DirectRef;
import com.lowdragmc.lowdraglib2.syncdata.ref.UniqueDirectRef;
import com.lowdragmc.lowdraglib2.syncdata.var.FieldVar;
import com.lowdragmc.lowdraglib2.syncdata.var.IVar;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.mojang.serialization.DynamicOps;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;

/**
 * Registers immutable, transient UI values with LDLib's native sync and RPC transport.
 * Register before creating the UI; values are replaced on the owning game thread and are never persisted.
 */
@NullMarked
@RequiredArgsConstructor
public final class StreamAccessor<T> implements IDirectAccessor<T> {

    private final Class<T> type;
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> codec;

    @Override
    public boolean test(Class<?> type) {
        return this.type == type;
    }

    @Override
    public void readDirectVarToStream(RegistryFriendlyByteBuf buffer, IVar<T> var) {
        codec.encode(buffer, var.value());
    }

    @Override
    public void writeDirectVarFromStream(RegistryFriendlyByteBuf buffer, IVar<T> var) {
        var.set(codec.decode(buffer));
    }

    @Override
    public DirectRef<T> createDirectRef(ManagedKey key, IVar<T> var) {
        return new UniqueDirectRef<>(var, key, this);
    }

    @Override
    public IVar<T> createDirectVar(ManagedKey key, Object holder) {
        return FieldVar.of(key, holder);
    }

    @Override
    public <D> D readDirectVar(DynamicOps<D> ops, IVar<T> var) {
        throw new UnsupportedOperationException("Transient UI values have no persistent representation");
    }

    @Override
    public <D> void writeDirectVar(DynamicOps<D> ops, IVar<T> var, D payload) {
        throw new UnsupportedOperationException("Transient UI values have no persistent representation");
    }
}
