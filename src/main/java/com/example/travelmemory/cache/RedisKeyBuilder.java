package com.example.travelmemory.cache;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;

@Component
public class RedisKeyBuilder {

    private static final String NO_DATE = "none";

    public String latestFlightPriceKey(FlightPriceSnapshot snapshot) {
        return latestFlightPriceKey(snapshot.toQuery());
    }

    public String latestFlightPriceKey(FlightPriceQuery query) {
        return String.join(":",
                "flight",
                "latest",
                normalize(query.origin()),
                normalize(query.destination()),
                datePart(query.departureDate()),
                datePart(query.returnDate()));
    }

    public String latestHotelPriceKey(HotelPriceSnapshot snapshot) {
        return latestHotelPriceKey(snapshot.toQuery());
    }

    public String latestHotelPriceKey(HotelPriceQuery query) {
        return String.join(":",
                "hotel",
                "latest",
                normalize(query.city()),
                normalize(query.hotelName()),
                datePart(query.checkInDate()),
                datePart(query.checkOutDate()));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String datePart(LocalDate date) {
        return date == null ? NO_DATE : date.toString();
    }
}
