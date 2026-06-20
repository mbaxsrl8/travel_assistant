package com.example.travelmemory.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

import jakarta.annotation.Nonnull;

import org.springframework.stereotype.Component;

import com.example.travelmemory.config.HBaseProperties;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;

@Component
public class HBaseRowKeyBuilder {

    private static final int REVERSE_TIMESTAMP_WIDTH = 19;

    private final int saltBuckets;
    private final int saltWidth;

    public HBaseRowKeyBuilder(HBaseProperties properties) {
        this.saltBuckets = properties.saltBuckets();
        this.saltWidth = Math.max(2, Integer.toString(saltBuckets - 1).length());
    }

    public String flightRowKey(@Nonnull FlightPriceSnapshot snapshot) {
        Instant capturedAt = snapshot.capturedAt();
        String identity = flightIdentity(
                snapshot.origin(),
                snapshot.destination(),
                snapshot.departureDate());
        return rowKey(identity, capturedAt);
    }

    public String hotelRowKey(@Nonnull HotelPriceSnapshot snapshot) {
        Instant capturedAt = snapshot.capturedAt();
        String identity = hotelIdentity(
                snapshot.city(),
                snapshot.hotelName(),
                snapshot.checkInDate());
        return rowKey(identity, capturedAt);
    }

    public List<String> flightScanPrefixes(@Nonnull FlightPriceQuery query) {
        return scanPrefixes(flightIdentity(
                query.origin(),
                query.destination(),
                query.departureDate()));
    }

    public List<String> hotelScanPrefixes(@Nonnull HotelPriceQuery query) {
        return scanPrefixes(hotelIdentity(
                query.city(),
                query.hotelName(),
                query.checkInDate()));
    }

    public long reverseTimestamp(@Nonnull Instant capturedAt) {
        return Long.MAX_VALUE - capturedAt.toEpochMilli();
    }

    private String rowKey(String identity, Instant capturedAt) {
        int salt = Math.floorMod(
                Objects.hash(identity, capturedAt.toEpochMilli()),
                saltBuckets);
        return salt(salt)
                + "#"
                + identity
                + "#"
                + String.format(Locale.ROOT, "%0" + REVERSE_TIMESTAMP_WIDTH + "d", reverseTimestamp(capturedAt));
    }

    private List<String> scanPrefixes(String identity) {
        return IntStream.range(0, saltBuckets)
                .mapToObj(bucket -> salt(bucket) + "#" + identity + "#")
                .toList();
    }

    private String flightIdentity(String origin, String destination, @Nonnull LocalDate departureDate) {
        return normalizeAirport(origin)
                + "-"
                + normalizeAirport(destination)
                + "#"
                + departureDate;
    }

    private String hotelIdentity(String city, String hotelName, @Nonnull LocalDate checkInDate) {
        return normalizeText(city)
                + "#"
                + normalizeText(hotelName)
                + "#"
                + checkInDate;
    }

    private String salt(int bucket) {
        return String.format(Locale.ROOT, "%0" + saltWidth + "d", bucket);
    }

    private String normalizeAirport(String value) {
        return requiredText(value)
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String normalizeText(String value) {
        return requiredText(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String requiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Row key value must not be blank");
        }
        return value.trim();
    }
}
