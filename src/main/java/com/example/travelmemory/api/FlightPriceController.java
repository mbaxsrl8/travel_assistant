package com.example.travelmemory.api;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.travelmemory.cache.RedisKeyBuilder;
import com.example.travelmemory.api.response.PriceHistoryResponse;
import com.example.travelmemory.api.response.PriceWriteResponse;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.service.FlightPriceService;

@RestController
@RequestMapping("/api/v1/flights/prices")
public class FlightPriceController {

    private final FlightPriceService priceService;
    private final RedisKeyBuilder keyBuilder;

    public FlightPriceController(FlightPriceService priceService, RedisKeyBuilder keyBuilder) {
        this.priceService = priceService;
        this.keyBuilder = keyBuilder;
    }

    @PostMapping
    public ResponseEntity<PriceWriteResponse> saveLatestPrice(@RequestBody FlightPriceSnapshot snapshot) {
        priceService.savePrice(snapshot);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PriceWriteResponse("saved", keyBuilder.latestFlightPriceKey(snapshot)));
    }

    @GetMapping("/latest")
    public ResponseEntity<FlightPriceSnapshot> getLatestPrice(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate
    ) {
        FlightPriceQuery query = new FlightPriceQuery(origin, destination, departureDate, returnDate);
        return ResponseEntity.of(priceService.getLatestPrice(query));
    }

    @GetMapping("/history")
    public PriceHistoryResponse<FlightPriceSnapshot> getPriceHistory(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate
    ) {
        FlightPriceQuery query = new FlightPriceQuery(origin, destination, departureDate, returnDate);
        return new PriceHistoryResponse<>(priceService.getPriceHistory(query));
    }
}
