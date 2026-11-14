package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.objects.Object2LongLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.Nullable;

/**
 * Provides material transactions for automatic multiblock building.
 *
 * Implementations must not mutate backing storage while reserving. Mutation is only allowed from
 * {@link Reservation#commit()}, {@link Reservation#commitExact()}, {@link Reservation#rollback()} or
 * {@link Session#insert(ItemStack, boolean)} with {@code simulate == false}.
 */
public interface MaterialSource {

    /**
     * Opens a per-stage reservation session.
     */
    Session openSession();

    /**
     * Reads exact availability for the requested keys without changing backing storage.
     */
    default Availability availability(ObjectCollection<MaterialKey> keys) {
        long sourceBindingFingerprint = bindingFingerprint();
        Session session = openSession();
        BuildProblem sessionProblem = session.problem();
        if (sessionProblem != null) {
            return new Availability(Object2LongMaps.emptyMap(), sourceBindingFingerprint);
        }
        Object2LongMap<MaterialKey> availability = new Object2LongLinkedOpenHashMap<>();
        try {
            for (MaterialKey key : keys) {
                availability.put(key, session.available(key));
            }
        } catch (ArithmeticException exception) {
            GTCEu.LOGGER.error("Automatic-build material availability overflow", exception);
            return new Availability(Object2LongMaps.emptyMap(), sourceBindingFingerprint);
        }
        sessionProblem = session.problem();
        return new Availability(sessionProblem == null ? availability : Object2LongMaps.emptyMap(),
                sourceBindingFingerprint);
    }

    /**
     * Returns a server-local identity token for the exact storage binding represented by the next availability.
     * Implementations backed by a selected container should include its object identity here, never in display text.
     */
    default long bindingFingerprint() {
        return 0;
    }

    /**
     * Contributes the selected source identity and exact requested availability to a visible plan fingerprint.
     */
    default long planFingerprint(ObjectCollection<MaterialKey> keys) {
        Availability availability = availability(keys);
        return planFingerprint(keys, availability);
    }

    /**
     * Computes the source contribution from the exact availability already used by material allocation.
     */
    default long planFingerprint(ObjectCollection<MaterialKey> keys, Availability availability) {
        long result = 0xcbf29ce484222325L;
        result = fingerprint(result, availability.bindingFingerprint());
        for (MaterialKey key : keys) {
            result = fingerprint(result, key.hashCode());
            result = fingerprint(result, availability.available(key));
        }
        return result;
    }

    private static long fingerprint(long current, long value) {
        return (current ^ value) * 0x100000001b3L;
    }

    interface Session {

        /**
         * Reserves this source's contribution to an ordered demand list without extracting backing storage.
         * The shared request retains unfulfilled counts for later sources and records exact reservation ownership.
         */
        default MaterialRequest.Reservations request(MaterialRequest request) {
            return request.reserve(this);
        }

        /**
         * Reports a problem discovered while opening or traversing this source.
         */
        @Nullable
        default BuildProblem problem() {
            return null;
        }

        /**
         * Returns the unreserved amount currently available for an exact key.
         */
        long available(MaterialKey key);

        /**
         * Reserves one item matching any candidate stack, without mutating backing storage.
         */
        @Nullable
        Reservation reserve(ObjectList<ItemStack> candidates);

        /**
         * Reserves up to {@code amount} items for an exact key without mutating backing storage.
         */
        @Nullable
        Reservation reserve(MaterialKey key, long amount);

        /**
         * Inserts a returned drop into this source.
         *
         * @return the remaining stack that could not be inserted
         */
        ItemStack insert(ItemStack stack, boolean simulate);
    }

    interface Reservation {

        /**
         * A single item stack that should be used for placement.
         */
        ItemStack stack();

        /**
         * Returns the exact material represented by this reservation.
         */
        MaterialKey key();

        /**
         * Returns how many items were reserved without relying on the bounded ItemStack count.
         */
        long reservedAmount();

        /**
         * Commits this reservation to backing storage.
         */
        boolean commit();

        /**
         * Commits this reservation and reports the exact extracted amount.
         */
        CommitResult commitExact();

        /**
         * Restores material extracted by the last commit attempt.
         */
        RollbackResult rollback();

        /**
         * Returns unexpected stacks a generic source could not restore itself after a broken handler response.
         */
        ObjectList<ItemStack> rejectedStacks();
    }

    /**
     * Immutable availability values for one material source.
     */
    record Availability(Object2LongMap<MaterialKey> available, long bindingFingerprint) {

        public long available(MaterialKey key) {
            return available.getLong(key);
        }
    }

    /**
     * Exact outcome of a storage commit.
     */
    record CommitResult(long requested, long committed, @Nullable BuildProblem problem) {

        public boolean complete() {
            return requested == committed && problem == null;
        }
    }

    /**
     * Exact outcome of restoring a committed reservation.
     */
    record RollbackResult(long requested, long restored, @Nullable BuildProblem problem) {

        public boolean complete() {
            return requested == restored && problem == null;
        }
    }
}
