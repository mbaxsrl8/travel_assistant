package com.example.travelmemory.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record FlightPriceSnapshot(
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        String carrier,
        BigDecimal price,
        String currency,
        String source,
        Instant capturedAt,
        Map<String, String> metadata
) {
    public FlightPriceQuery toQuery() {
        return new FlightPriceQuery(origin, destination, departureDate, returnDate);
    }
}
