package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.client.ProspectingMapClientHandler;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.tterrag.registrate.util.RegistrateDistExecutor;

/**
 * Dist-gated entry points for clientbound packet effects that need client-only classes.
 */
public final class ClientPacketHandlers {

    private ClientPacketHandlers() {}

    public static void handleProspectingMapData(SPacketProspectingMapData packet, IPayloadContext context) {
        RegistrateDistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ProspectingMapClientHandler.handle(packet, context));
    }
}
