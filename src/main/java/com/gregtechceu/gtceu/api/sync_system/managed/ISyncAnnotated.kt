package com.gregtechceu.gtceu.api.sync_system.managed

import com.gregtechceu.gtceu.api.sync_system.annotations.ClientFieldChangeListener
import com.gregtechceu.gtceu.api.sync_system.serialization.SyncCodec

import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.neoforged.neoforge.common.util.INBTSerializable

/**
 * Represents a class with fields that have sync annotations. <br></br>
 *
 * Saved fields use the same metadata as managed synchronization and provide native NBT serialization by default. <br></br>
 *
 * Value classes declare their Codec through `FieldCodecs.createCodec(factory)` and register it at the owning use site. <br></br>
 *
 * The factory defines initial values for fields omitted from stored data. Decoding updates mutable containers in place;
 * other saved fields must be writable. Codecs run on the caller's thread, and this interface provides no independent
 * network lifecycle or dirty tracking. <br></br>
 *
 * [ClientFieldChangeListener] does not work for fields in [ISyncAnnotated] classes.
 *
 *
 * A field of type `T` can be marked with sync annotations if:
 * <ul>
 * <li>`T` is primitive</li>
 * <li>`T` has a [com.gregtechceu.gtceu.api.sync_system.FieldCodecs] codec registered</li>
 * <li>`T` is an [ISyncAnnotated] class</li>
 * </ul>
 *
 * @see ISyncManaged
 */
interface ISyncAnnotated : INBTSerializable<CompoundTag> {
	override fun serializeNBT(provider: HolderLookup.Provider): CompoundTag = SyncCodec.serializeNBT(this, provider)

	override fun deserializeNBT(provider: HolderLookup.Provider, nbt: CompoundTag) = SyncCodec.deserializeNBT(this, provider, nbt)
}
