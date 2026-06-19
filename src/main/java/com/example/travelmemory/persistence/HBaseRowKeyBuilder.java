package com.example.travelmemory.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;

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

    public String flightRowKey(FlightPriceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Flight price snapshot is required");
        Instant capturedAt = Objects.requireNonNull(snapshot.capturedAt(), "capturedAt is required");
        String identity = flightIdentity(
                snapshot.origin(),
                snapshot.destination(),
                snapshot.departureDate());
        return rowKey(identity, capturedAt);
    }

    public String hotelRowKey(HotelPriceSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "Hotel price snapshot is required");
        Instant capturedAt = Objects.requireNonNull(snapshot.capturedAt(), "capturedAt is required");
        String identity = hotelIdentity(
                snapshot.city(),
                snapshot.hotelName(),
                snapshot.checkInDate());
        return rowKey(identity, capturedAt);
    }

    public List<String> flightScanPrefixes(FlightPriceQuery query) {
        Objects.requireNonNull(query, "Flight price query is required");
        return scanPrefixes(flightIdentity(
                query.origin(),
                query.destination(),
                query.departureDate()));
    }

    public List<String> hotelScanPrefixes(HotelPriceQuery query) {
        Objects.requireNonNull(query, "Hotel price query is required");
        return scanPrefixes(hotelIdentity(
                query.city(),
                query.hotelName(),
                query.checkInDate()));
    }

    public long reverseTimestamp(Instant capturedAt) {
        return Long.MAX_VALUE - Objects.requireNonNull(capturedAt, "capturedAt is required").toEpochMilli();
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

    private String flightIdentity(String origin, String destination, LocalDate departureDate) {
        return normalizeAirport(origin)
                + "-"
                + normalizeAirport(destination)
                + "#"
                + requiredDate(departureDate, "departureDate");
    }

    private String hotelIdentity(String city, String hotelName, LocalDate checkInDate) {
        return normalizeText(city)
                + "#"
                + normalizeText(hotelName)
                + "#"
                + requiredDate(checkInDate, "checkInDate");
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

    private LocalDate requiredDate(LocalDate value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " is required");
    }
}
