package com.example.travelmemory.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.HotelPriceQuery;

class RedisKeyBuilderTest {

    private final RedisKeyBuilder keyBuilder = new RedisKeyBuilder();

    @Test
    void buildsLatestFlightPriceKey() {
        FlightPriceQuery query = new FlightPriceQuery(
                "LAX",
                "NRT",
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-14"));

        assertThat(keyBuilder.latestFlightPriceKey(query))
                .isEqualTo("flight:latest:lax:nrt:2026-09-01:2026-09-14");
    }

    @Test
    void buildsLatestHotelPriceKey() {
        HotelPriceQuery query = new HotelPriceQuery(
                "Tokyo",
                "Park Hotel",
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-05"));

        assertThat(keyBuilder.latestHotelPriceKey(query))
                .isEqualTo("hotel:latest:tokyo:park-hotel:2026-09-01:2026-09-05");
    }
}
