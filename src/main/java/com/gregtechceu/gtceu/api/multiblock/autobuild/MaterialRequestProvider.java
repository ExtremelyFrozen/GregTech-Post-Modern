package com.gregtechceu.gtceu.api.multiblock.autobuild;

import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * Binds a player's storage and fulfills the remaining portion of one automatic-build material request.
 *
 * <p>
 * Providers are registered during common initialization and invoked in allocation order on the server thread.
 * A provider selects its own storage using the request context, reserves only material it actually supplies, and
 * leaves all other demand for later providers. It must not extract items before the returned reservations commit.
 * An empty demand list selects the same concrete source used by subsequent preview and execution stages.
 * Execution dispatches back to this registration with the real demand list and the original source/session;
 * providers use {@link MaterialRequest#fulfill(Supplier)} so storage is not selected again.
 * </p>
 */
@FunctionalInterface
public interface MaterialRequestProvider {

    /**
     * Selects or reuses this provider's source and reserves its contribution to the mutable demand list.
     *
     * @param player  server player whose storage and permissions apply; never null
     * @param request one server-thread request, whose unfulfilled counts may only decrease by returned reservations
     * @return the concrete selected source and its exact allocations or storage problem; never null
     */
    MaterialRequest.Result request(ServerPlayer player, MaterialRequest request);
}
