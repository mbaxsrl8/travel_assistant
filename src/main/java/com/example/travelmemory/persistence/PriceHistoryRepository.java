package com.example.travelmemory.persistence;

import java.util.List;

public interface PriceHistoryRepository<T, Q> {

    void save(T snapshot);

    List<T> findHistory(Q query);
}
