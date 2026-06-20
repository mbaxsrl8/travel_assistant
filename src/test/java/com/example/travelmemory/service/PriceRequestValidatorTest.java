package com.example.travelmemory.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.example.travelmemory.exception.InvalidPriceRequestException;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;

class PriceRequestValidatorTest {

    private static final LocalDate DEPARTURE_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate RETURN_DATE = LocalDate.of(2026, 9, 10);
    private static final LocalDate CHECK_IN_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate CHECK_OUT_DATE = LocalDate.of(2026, 9, 5);
    private static final Instant CAPTURED_AT = Instant.parse("2026-06-20T08:00:00Z");

    @Test
    void acceptsValidFlightQueryAndSnapshot() {
        assertThatCode(() -> PriceRequestValidator.validate(validFlightQuery()))
                .doesNotThrowAnyException();
        assertThatCode(() -> PriceRequestValidator.validate(validFlightSnapshot()))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsValidHotelQueryAndSnapshot() {
        assertThatCode(() -> PriceRequestValidator.validate(validHotelQuery()))
                .doesNotThrowAnyException();
        assertThatCode(() -> PriceRequestValidator.validate(validHotelSnapshot()))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFlightRequests")
    void rejectsInvalidFlightRequests(String description, Executable validation, String message) {
        assertThatThrownBy(validation::execute)
                .isInstanceOf(InvalidPriceRequestException.class)
                .hasMessage(message);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidHotelRequests")
    void rejectsInvalidHotelRequests(String description, Executable validation, String message) {
        assertThatThrownBy(validation::execute)
                .isInstanceOf(InvalidPriceRequestException.class)
                .hasMessage(message);
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> invalidFlightRequests() {
        return Stream.of(
                invalid("null flight snapshot",
                        () -> PriceRequestValidator.validate((FlightPriceSnapshot) null),
                        "Flight price snapshot is required"),
                invalid("null flight query",
                        () -> PriceRequestValidator.validate((FlightPriceQuery) null),
                        "Flight price query is required"),
                invalid("blank origin",
                        () -> PriceRequestValidator.validate(new FlightPriceQuery(
                                " ", "NRT", DEPARTURE_DATE, RETURN_DATE)),
                        "Origin is required"),
                invalid("blank destination",
                        () -> PriceRequestValidator.validate(new FlightPriceQuery(
                                "LAX", " ", DEPARTURE_DATE, RETURN_DATE)),
                        "Destination is required"),
                invalid("missing departure date",
                        () -> PriceRequestValidator.validate(new FlightPriceQuery(
                                "LAX", "NRT", null, RETURN_DATE)),
                        "Departure date is required"),
                invalid("return before departure",
                        () -> PriceRequestValidator.validate(new FlightPriceQuery(
                                "LAX", "NRT", DEPARTURE_DATE, DEPARTURE_DATE.minusDays(1))),
                        "Return date must not be before departure date"),
                invalid("missing price",
                        () -> PriceRequestValidator.validate(flightSnapshot(null, "USD", "provider", CAPTURED_AT)),
                        "Price must be greater than zero"),
                invalid("non-positive price",
                        () -> PriceRequestValidator.validate(flightSnapshot(
                                BigDecimal.ZERO, "USD", "provider", CAPTURED_AT)),
                        "Price must be greater than zero"),
                invalid("blank currency",
                        () -> PriceRequestValidator.validate(flightSnapshot(
                                new BigDecimal("715.50"), " ", "provider", CAPTURED_AT)),
                        "Currency is required"),
                invalid("blank source",
                        () -> PriceRequestValidator.validate(flightSnapshot(
                                new BigDecimal("715.50"), "USD", " ", CAPTURED_AT)),
                        "Source is required"),
                invalid("missing captured timestamp",
                        () -> PriceRequestValidator.validate(flightSnapshot(
                                new BigDecimal("715.50"), "USD", "provider", null)),
                        "Captured timestamp is required"));
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> invalidHotelRequests() {
        return Stream.of(
                invalid("null hotel snapshot",
                        () -> PriceRequestValidator.validate((HotelPriceSnapshot) null),
                        "Hotel price snapshot is required"),
                invalid("null hotel query",
                        () -> PriceRequestValidator.validate((HotelPriceQuery) null),
                        "Hotel price query is required"),
                invalid("blank city",
                        () -> PriceRequestValidator.validate(new HotelPriceQuery(
                                " ", "Park Hotel", CHECK_IN_DATE, CHECK_OUT_DATE)),
                        "City is required"),
                invalid("blank hotel name",
                        () -> PriceRequestValidator.validate(new HotelPriceQuery(
                                "Tokyo", " ", CHECK_IN_DATE, CHECK_OUT_DATE)),
                        "Hotel name is required"),
                invalid("missing check-in date",
                        () -> PriceRequestValidator.validate(new HotelPriceQuery(
                                "Tokyo", "Park Hotel", null, CHECK_OUT_DATE)),
                        "Check-in date is required"),
                invalid("missing check-out date",
                        () -> PriceRequestValidator.validate(new HotelPriceQuery(
                                "Tokyo", "Park Hotel", CHECK_IN_DATE, null)),
                        "Check-out date is required"),
                invalid("check-out not after check-in",
                        () -> PriceRequestValidator.validate(new HotelPriceQuery(
                                "Tokyo", "Park Hotel", CHECK_IN_DATE, CHECK_IN_DATE)),
                        "Check-out date must be after check-in date"),
                invalid("missing price",
                        () -> PriceRequestValidator.validate(hotelSnapshot(null, "USD", "provider", CAPTURED_AT)),
                        "Price must be greater than zero"),
                invalid("non-positive price",
                        () -> PriceRequestValidator.validate(hotelSnapshot(
                                BigDecimal.ZERO, "USD", "provider", CAPTURED_AT)),
                        "Price must be greater than zero"),
                invalid("blank currency",
                        () -> PriceRequestValidator.validate(hotelSnapshot(
                                new BigDecimal("220.00"), " ", "provider", CAPTURED_AT)),
                        "Currency is required"),
                invalid("blank source",
                        () -> PriceRequestValidator.validate(hotelSnapshot(
                                new BigDecimal("220.00"), "USD", " ", CAPTURED_AT)),
                        "Source is required"),
                invalid("missing captured timestamp",
                        () -> PriceRequestValidator.validate(hotelSnapshot(
                                new BigDecimal("220.00"), "USD", "provider", null)),
                        "Captured timestamp is required"));
    }

    private static Arguments invalid(String description, Executable validation, String message) {
        return Arguments.of(description, validation, message);
    }

    private static FlightPriceQuery validFlightQuery() {
        return new FlightPriceQuery("LAX", "NRT", DEPARTURE_DATE, RETURN_DATE);
    }

    private static FlightPriceSnapshot validFlightSnapshot() {
        return flightSnapshot(new BigDecimal("715.50"), "USD", "provider", CAPTURED_AT);
    }

    private static FlightPriceSnapshot flightSnapshot(
            BigDecimal price,
            String currency,
            String source,
            Instant capturedAt
    ) {
        return new FlightPriceSnapshot(
                "LAX", "NRT", DEPARTURE_DATE, RETURN_DATE, "JL",
                price, currency, source, capturedAt, Map.of());
    }

    private static HotelPriceQuery validHotelQuery() {
        return new HotelPriceQuery("Tokyo", "Park Hotel", CHECK_IN_DATE, CHECK_OUT_DATE);
    }

    private static HotelPriceSnapshot validHotelSnapshot() {
        return hotelSnapshot(new BigDecimal("220.00"), "USD", "provider", CAPTURED_AT);
    }

    private static HotelPriceSnapshot hotelSnapshot(
            BigDecimal price,
            String currency,
            String source,
            Instant capturedAt
    ) {
        return new HotelPriceSnapshot(
                "Tokyo", "Park Hotel", CHECK_IN_DATE, CHECK_OUT_DATE, "King",
                price, currency, source, capturedAt, Map.of());
    }
}
