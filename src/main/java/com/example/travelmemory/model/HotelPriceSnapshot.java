package com.example.travelmemory.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record HotelPriceSnapshot(
        String city,
        String hotelName,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        String roomType,
        BigDecimal price,
        String currency,
        String source,
        Instant capturedAt,
        Map<String, String> metadata
) {
    public HotelPriceQuery toQuery() {
        return new HotelPriceQuery(city, hotelName, checkInDate, checkOutDate);
    }
}
