package com.example.travelmemory.model;

import java.time.LocalDate;

public record HotelPriceQuery(
        String city,
        String hotelName,
        LocalDate checkInDate,
        LocalDate checkOutDate
) {
}
