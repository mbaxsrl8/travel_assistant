package com.example.travelmemory.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.example.travelmemory.config.HBaseProperties;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceSnapshot;

class HBaseRowKeyBuilderTest {

    private final HBaseRowKeyBuilder keyBuilder = new HBaseRowKeyBuilder(properties(16));

    @Test
    void buildsDeterministicSaltedFlightRowKey() {
        FlightPriceSnapshot snapshot = flightSnapshot("2026-06-19T08:00:00Z");

        String rowKey = keyBuilder.flightRowKey(snapshot);

        assertThat(rowKey)
                .matches("\\d{2}#LAX-NRT#2026-09-01#\\d{19}")
                .isEqualTo(keyBuilder.flightRowKey(snapshot));
    }

    @Test
    void buildsNormalizedHotelRowKey() {
        HotelPriceSnapshot snapshot = new HotelPriceSnapshot(
                "New York",
                "The Plaza Hotel",
                LocalDate.parse("2026-10-01"),
                LocalDate.parse("2026-10-04"),
                "King",
                new BigDecimal("450.00"),
                "USD",
                "booking",
                Instant.parse("2026-06-19T08:00:00Z"),
                Map.of());

        assertThat(keyBuilder.hotelRowKey(snapshot))
                .matches("\\d{2}#new-york#the-plaza-hotel#2026-10-01#\\d{19}");
    }

    @Test
    void createsOneFlightScanPrefixPerSaltBucket() {
        FlightPriceQuery query = new FlightPriceQuery(
                "LAX",
                "NRT",
                LocalDate.parse("2026-09-01"),
                null);

        assertThat(keyBuilder.flightScanPrefixes(query))
                .hasSize(16)
                .contains("00#LAX-NRT#2026-09-01#", "15#LAX-NRT#2026-09-01#");
    }

    @Test
    void newerSnapshotsHaveSmallerReverseTimestamps() {
        Instant older = Instant.parse("2026-06-19T08:00:00Z");
        Instant newer = Instant.parse("2026-06-19T09:00:00Z");

        assertThat(keyBuilder.reverseTimestamp(newer))
                .isLessThan(keyBuilder.reverseTimestamp(older));
    }

    private static FlightPriceSnapshot flightSnapshot(String capturedAt) {
        return new FlightPriceSnapshot(
                "LAX",
                "NRT",
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-14"),
                "JL",
                new BigDecimal("742.35"),
                "USD",
                "amadeus",
                Instant.parse(capturedAt),
                Map.of("fareClass", "economy"));
    }

    private static HBaseProperties properties(int saltBuckets) {
        return new HBaseProperties(
                "localhost",
                2181,
                "/hbase",
                "travel_flight_prices",
                "travel_hotel_prices",
                saltBuckets);
    }
}
