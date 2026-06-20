package com.example.travelmemory.persistence;

import java.util.List;

import jakarta.annotation.Nonnull;

/**
 * Persists price snapshots and retrieves their historical values by query.
 *
 * @param <T> type of price snapshot stored by the repository
 * @param <Q> type of query used to identify a price history series
 */
public interface PriceHistoryRepository<T, Q> {

    /**
     * Persists a captured price snapshot in its historical series.
     *
     * @param snapshot price snapshot to persist
     */
    void save(@Nonnull T snapshot);

    /**
     * Retrieves all persisted snapshots matching the supplied query.
     *
     * @param query identifying fields used to select the historical series
     * @return matching snapshots ordered from newest to oldest, or an empty list when no history
     *         exists
     */
    List<T> findHistory(@Nonnull Q query);
}
