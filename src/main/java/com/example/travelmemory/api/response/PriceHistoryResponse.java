package com.example.travelmemory.api.response;

import java.util.List;

public record PriceHistoryResponse<T>(
        List<T> snapshots
) {
}
