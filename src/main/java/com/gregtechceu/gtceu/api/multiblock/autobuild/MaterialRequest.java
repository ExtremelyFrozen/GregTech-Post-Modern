package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.Reservation;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.Session;
import com.gregtechceu.gtceu.api.multiblock.autobuild.MaterialSource.SourceKind;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Ordered exact demands shared by material providers during one server-thread allocation.
 *
 * <p>Only reservation changes the remaining counts. Storage is unchanged until a caller commits the returned
 * reservations, and each reservation retains its original source for rollback. The excluded container is kept by
 * reference so neither its contents nor the terminal that opened the menu can become a material source.</p>
 */
public final class MaterialRequest {

    @Getter
    private final ObjectList<Demand> demands;
    private final LongList remaining;
    @Getter
    private final ItemStack excludedContainer;
    private final Predicate<SourceKind> includedSources;

    public MaterialRequest(ObjectList<Demand> demands, ItemStack excludedContainer,
                           Predicate<SourceKind> includedSources) {
        this.demands = demands;
        this.remaining = new LongArrayList(demands.size());
        for (Demand demand : demands) {
            remaining.add(demand.amount());
        }
        this.excludedContainer = excludedContainer;
        this.includedSources = includedSources;
    }

    public boolean includes(SourceKind kind) {
        return includedSources.test(kind);
    }

    public long remaining(int demandIndex) {
        return remaining.getLong(demandIndex);
    }

    /** Returns the first still-unfulfilled demand, or -1 when every exact count was reserved. */
    public int firstUnfulfilled() {
        for (int index = 0; index < remaining.size(); index++) {
            if (remaining.getLong(index) > 0) return index;
        }
        return -1;
    }

    /** Binds one concrete source, then reserves only its contribution without extracting backing items. */
    public Result fulfill(MaterialSource source) {
        if (demands.isEmpty()) {
            return new Result(source, new Reservations(ObjectList.of(), null));
        }
        return new Result(source, source.openSession().request(this));
    }

    /** Called by a bound source session; allocation is performed once in the original demand order. */
    Reservations reserve(Session session) {
        ObjectList<Allocation> allocations = new ObjectArrayList<>();
        BuildProblem problem = session.problem();
        if (problem != null) {
            return new Reservations(allocations, problem);
        }
        for (int index = 0; index < demands.size(); index++) {
            long amount = remaining.getLong(index);
            if (amount == 0) continue;
            MaterialKey key = demands.get(index).key();
            Reservation reservation = session.reserve(key, amount);
            if (reservation == null) continue;
            long reserved = reservation.reservedAmount();
            remaining.set(index, amount - reserved);
            allocations.add(new Allocation(index, reservation));
        }
        return new Reservations(allocations, session.problem());
    }

    /** One independent demand; its amount is exact and is not restricted by an ItemStack count. */
    public record Demand(MaterialKey key, long amount) {}

    /** Identifies which original demand owns a reservation and therefore its eventual refund. */
    public record Allocation(int demandIndex, Reservation reservation) {}

    public record Reservations(ObjectList<Allocation> allocations, @Nullable BuildProblem problem) {}

    /** The selected raw source is reused by preview and execution; no provider dispatch wrapper is involved. */
    public record Result(MaterialSource source, Reservations materials) {}
}
