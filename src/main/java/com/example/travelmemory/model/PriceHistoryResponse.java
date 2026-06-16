package com.example.travelmemory.model;

import java.util.List;

public record PriceHistoryResponse<T>(
        List<T> snapshots
) {
}
