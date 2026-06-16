package com.example.travelmemory.model;

import java.time.LocalDate;

public record FlightPriceQuery(
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate
) {
}
