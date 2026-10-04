package com.gregtechceu.gtceu.api.multiblock.autobuild;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides material transactions for automatic multiblock building.
 *
 * Implementations must not mutate backing storage while reserving. Mutation is only allowed from
 * {@link Reservation#commit()}, {@link Reservation#commitExact()}, {@link Reservation#rollback()} or
 * {@link Session#insert(ItemStack, boolean)} with {@code simulate == false}.
 */
public interface AutoBuildMaterialSource {

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
    default AutoBuildProblem unavailableProblem() {
        return null;
    }

    /**
     * Reads exact availability for the requested keys without changing backing storage.
     */
    default Snapshot snapshot(Collection<AutoBuildItemKey> keys) {
        SourceDescriptor sourceDescriptor = descriptor();
        long sourceBindingFingerprint = bindingFingerprint();
        AutoBuildProblem problem = unavailableProblem();
        if (problem != null) {
            return new Snapshot(kind(), Map.of(), problem, sourceDescriptor, sourceBindingFingerprint);
        }
        Session session = openSession();
        AutoBuildProblem sessionProblem = session.problem();
        if (sessionProblem != null) {
            return new Snapshot(kind(), Map.of(), sessionProblem, sourceDescriptor, sourceBindingFingerprint);
        }
        Map<AutoBuildItemKey, Long> availability = new LinkedHashMap<>();
        try {
            for (AutoBuildItemKey key : keys) {
                availability.put(key, session.available(key));
            }
        } catch (ArithmeticException exception) {
            GTCEu.LOGGER.error("Automatic-build material availability overflow", exception);
            return new Snapshot(kind(), availability, countOverflow(), sourceDescriptor, sourceBindingFingerprint);
        }
        sessionProblem = session.problem();
        return new Snapshot(kind(), availability, sessionProblem, sourceDescriptor, sourceBindingFingerprint);
    }

    /**
     * Returns a server-local identity token for the exact storage binding represented by the next snapshot.
     * Implementations backed by a selected container should include its object identity here, never in display text.
     */
    default long bindingFingerprint() {
        return 0;
    }

    /**
     * Contributes the selected source identity and exact requested availability to a visible plan fingerprint.
     */
    default long planFingerprint(Collection<AutoBuildItemKey> keys) {
        Snapshot snapshot = snapshot(keys);
        return planFingerprint(keys, snapshot);
    }

    /**
     * Computes the source contribution from the exact snapshot already used by material allocation.
     */
    default long planFingerprint(Collection<AutoBuildItemKey> keys, Snapshot snapshot) {
        long result = 0xcbf29ce484222325L;
        result = fingerprint(result, snapshot.kind().ordinal());
        SourceDescriptor descriptor = snapshot.descriptor();
        if (descriptor != null) {
            result = fingerprint(result, descriptor.displayName().hashCode());
            result = fingerprint(result, descriptor.selectionIdentity().hashCode());
            result = fingerprint(result, descriptor.linkTarget() == null ? 0 : descriptor.linkTarget().hashCode());
        }
        result = fingerprint(result, snapshot.bindingFingerprint());
        if (snapshot.problem() != null) {
            result = fingerprint(result, snapshot.problem().type().ordinal());
        }
        for (AutoBuildItemKey key : keys) {
            result = fingerprint(result, key.hashCode());
            result = fingerprint(result, snapshot.available(key));
        }
        return result;
    }

    private static long fingerprint(long current, long value) {
        return (current ^ value) * 0x100000001b3L;
    }

    private static AutoBuildProblem countOverflow() {
        return new AutoBuildProblem(AutoBuildProblem.Type.COUNT_OVERFLOW, null,
                Component.translatable("gtpm.multiblock.autobuild.count_overflow"));
    }

    interface Session {

        /**
         * Reports a problem discovered while opening or traversing this source.
         */
        @Nullable
        default AutoBuildProblem problem() {
            return null;
        }

        /**
         * Returns the unreserved amount currently available for an exact key.
         *
         * <p>
         * The default keeps legacy addon sources binary-compatible; such sources do not participate in
         * availability displays until they implement this method.
         * </p>
         */
        default long available(AutoBuildItemKey key) {
            return 0;
        }

        /**
         * Reserves one item matching any candidate stack, without mutating backing storage.
         */
        @Nullable
        Reservation reserve(List<ItemStack> candidates);

        /**
         * Reserves up to {@code amount} items for an exact key without mutating backing storage.
         */
        @Nullable
        default Reservation reserve(AutoBuildItemKey key, long amount) {
            if (amount <= 0) {
                throw new IllegalArgumentException("Reserved material amount must be positive");
            }
            return amount == 1 ? reserve(List.of(key.prototype())) : null;
        }

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
        default AutoBuildItemKey key() {
            return AutoBuildItemKey.of(stack());
        }

        /**
         * Returns how many items were reserved without relying on the bounded ItemStack count.
         */
        default long reservedAmount() {
            return stack().getCount();
        }

        /**
         * Commits this reservation to backing storage.
         */
        boolean commit();

        /**
         * Commits this reservation and reports the exact extracted amount.
         */
        default CommitResult commitExact() {
            long requested = reservedAmount();
            return new CommitResult(requested, commit() ? requested : 0, null);
        }

        /**
         * Restores material extracted by the last commit attempt.
         */
        default RollbackResult rollback() {
            long requested = reservedAmount();
            return new RollbackResult(requested, 0, null);
        }

        /**
         * Returns unexpected stacks a generic source could not restore itself after a broken handler response.
         */
        default List<ItemStack> rejectedStacks() {
            return List.of();
        }
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

        public SourceDescriptor {
            if (displayName.isBlank() || displayName.length() > 256 ||
                    selectionIdentity.isBlank() || selectionIdentity.length() > 256) {
                throw new IllegalArgumentException("Material source display identity must be present and bounded");
            }
        }
    }

    /**
     * Immutable availability values for one material source.
     */
    record Snapshot(SourceKind kind, Map<AutoBuildItemKey, Long> available,
                    @Nullable AutoBuildProblem problem, @Nullable SourceDescriptor descriptor,
                    long bindingFingerprint) {

        public Snapshot(SourceKind kind, Map<AutoBuildItemKey, Long> available,
                        @Nullable AutoBuildProblem problem) {
            this(kind, available, problem, null, 0);
        }

        public Snapshot {
            if (descriptor != null && descriptor.kind() != kind) {
                throw new IllegalArgumentException("Material snapshot descriptor kind does not match its source");
            }
            LinkedHashMap<AutoBuildItemKey, Long> ordered = new LinkedHashMap<>();
            available.forEach((key, value) -> {
                if (value < 0) {
                    throw new IllegalArgumentException("Material snapshot values must be non-negative");
                }
                ordered.put(key, value);
            });
            available = Collections.unmodifiableMap(ordered);
        }

        public long available(AutoBuildItemKey key) {
            return available.getOrDefault(key, 0L);
        }
    }

    /**
     * Exact outcome of a storage commit.
     */
    record CommitResult(long requested, long committed, @Nullable AutoBuildProblem problem) {

        public CommitResult {
            if (requested < 0 || committed < 0 || committed > requested) {
                throw new IllegalArgumentException("Invalid material commit amounts");
            }
        }

        public boolean complete() {
            return requested == committed && problem == null;
        }
    }

    /**
     * Exact outcome of restoring a committed reservation.
     */
    record RollbackResult(long requested, long restored, @Nullable AutoBuildProblem problem) {

        public RollbackResult {
            if (requested < 0 || restored < 0 || restored > requested) {
                throw new IllegalArgumentException("Invalid material rollback amounts");
            }
        }

        public boolean complete() {
            return requested == restored && problem == null;
        }
    }
}
