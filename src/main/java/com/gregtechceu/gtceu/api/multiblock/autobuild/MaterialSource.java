package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
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
     * Describes how this source should be presented and ordered in material diagnostics.
     */
    default SourceKind kind() {
        return SourceKind.OTHER;
    }

    /** Returns optional, display-only identity for the concrete source selected by this operation. */
    @Nullable
    default SourceDescriptor descriptor() {
        return null;
    }

    /**
     * Opens a per-stage reservation session.
     */
    Session openSession();

    /**
     * Describes why this source cannot be used before a build starts.
     *
     * @return null when the source is available
     */
    @Nullable
    default BuildProblem unavailableProblem() {
        return null;
    }

    /**
     * Reads exact availability for the requested keys without changing backing storage.
     */
    default Availability availability(ObjectCollection<MaterialKey> keys) {
        SourceDescriptor sourceDescriptor = descriptor();
        long sourceBindingFingerprint = bindingFingerprint();
        BuildProblem problem = unavailableProblem();
        if (problem != null) {
            return new Availability(kind(), Object2LongMaps.emptyMap(), problem, sourceDescriptor,
                    sourceBindingFingerprint);
        }
        Session session = openSession();
        BuildProblem sessionProblem = session.problem();
        if (sessionProblem != null) {
            return new Availability(kind(), Object2LongMaps.emptyMap(), sessionProblem, sourceDescriptor,
                    sourceBindingFingerprint);
        }
        Object2LongMap<MaterialKey> availability = new Object2LongLinkedOpenHashMap<>();
        try {
            for (MaterialKey key : keys) {
                availability.put(key, session.available(key));
            }
        } catch (ArithmeticException exception) {
            GTCEu.LOGGER.error("Automatic-build material availability overflow", exception);
            return new Availability(kind(), availability, countOverflow(), sourceDescriptor, sourceBindingFingerprint);
        }
        sessionProblem = session.problem();
        return new Availability(kind(), availability, sessionProblem, sourceDescriptor, sourceBindingFingerprint);
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
        result = fingerprint(result, availability.kind().ordinal());
        SourceDescriptor descriptor = availability.descriptor();
        if (descriptor != null) {
            result = fingerprint(result, descriptor.displayName().hashCode());
            result = fingerprint(result, descriptor.selectionIdentity().hashCode());
            result = fingerprint(result, descriptor.linkTarget() == null ? 0 : descriptor.linkTarget().hashCode());
        }
        result = fingerprint(result, availability.bindingFingerprint());
        if (availability.problem() != null) {
            result = fingerprint(result, availability.problem().type().ordinal());
        }
        for (MaterialKey key : keys) {
            result = fingerprint(result, key.hashCode());
            result = fingerprint(result, availability.available(key));
        }
        return result;
    }

    private static long fingerprint(long current, long value) {
        return (current ^ value) * 0x100000001b3L;
    }

    private static BuildProblem countOverflow() {
        return new BuildProblem(BuildProblem.Type.COUNT_OVERFLOW, null,
                Component.translatable("gtpm.multiblock.autobuild.count_overflow"));
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

    enum SourceKind {
        ME,
        PLAYER,
        OTHER,
        UNAVAILABLE
    }

    /** Display-only source identity that never grants storage authority. */
    record SourceDescriptor(SourceKind kind, String displayName, String selectionIdentity,
                            @Nullable GlobalPos linkTarget) {

    }

    /**
     * Immutable availability values for one material source.
     */
    record Availability(SourceKind kind, Object2LongMap<MaterialKey> available,
                        @Nullable BuildProblem problem, @Nullable SourceDescriptor descriptor,
                        long bindingFingerprint) {

        public Availability(SourceKind kind, Object2LongMap<MaterialKey> available,
                            @Nullable BuildProblem problem) {
            this(kind, available, problem, null, 0);
        }

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
