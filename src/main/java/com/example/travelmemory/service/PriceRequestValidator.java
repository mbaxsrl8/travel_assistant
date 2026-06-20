package com.example.travelmemory.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.example.travelmemory.exception.InvalidPriceRequestException;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;

final class PriceRequestValidator {

    private PriceRequestValidator() {
    }

    static void validate(FlightPriceSnapshot snapshot) {
        if (snapshot == null) {
            throw invalid("Flight price snapshot is required");
        }
        validate(snapshot.toQuery());
        requirePositive(snapshot.price(), "Price");
        requireText(snapshot.currency(), "Currency");
        requireText(snapshot.source(), "Source");
        requireTimestamp(snapshot.capturedAt());
    }

    static void validate(FlightPriceQuery query) {
        if (query == null) {
            throw invalid("Flight price query is required");
        }
        requireText(query.origin(), "Origin");
        requireText(query.destination(), "Destination");
        requireDate(query.departureDate(), "Departure date");
        if (query.returnDate() != null && query.returnDate().isBefore(query.departureDate())) {
            throw invalid("Return date must not be before departure date");
        }
    }

    static void validate(HotelPriceSnapshot snapshot) {
        if (snapshot == null) {
            throw invalid("Hotel price snapshot is required");
        }
        validate(snapshot.toQuery());
        requirePositive(snapshot.price(), "Price");
        requireText(snapshot.currency(), "Currency");
        requireText(snapshot.source(), "Source");
        requireTimestamp(snapshot.capturedAt());
    }

    static void validate(HotelPriceQuery query) {
        if (query == null) {
            throw invalid("Hotel price query is required");
        }
        requireText(query.city(), "City");
        requireText(query.hotelName(), "Hotel name");
        requireDate(query.checkInDate(), "Check-in date");
        requireDate(query.checkOutDate(), "Check-out date");
        if (!query.checkOutDate().isAfter(query.checkInDate())) {
            throw invalid("Check-out date must be after check-in date");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field + " is required");
        }
    }

    private static void requireDate(LocalDate value, String field) {
        if (value == null) {
            throw invalid(field + " is required");
        }
    }

    private static void requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw invalid(field + " must be greater than zero");
        }
    }

    private static void requireTimestamp(Instant capturedAt) {
        if (capturedAt == null) {
            throw invalid("Captured timestamp is required");
        }
    }

    private static InvalidPriceRequestException invalid(String message) {
        return new InvalidPriceRequestException(message);
    }
}
