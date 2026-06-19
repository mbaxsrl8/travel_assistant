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

import com.example.travelmemory.cache.PriceCacheRepository;
import com.example.travelmemory.cache.RedisKeyBuilder;
import com.example.travelmemory.api.response.PriceHistoryResponse;
import com.example.travelmemory.api.response.PriceWriteResponse;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;

@RestController
@RequestMapping("/api/v1/flights/prices")
public class FlightPriceController {

    private final PriceCacheRepository cacheRepository;
    private final RedisKeyBuilder keyBuilder;

    public FlightPriceController(PriceCacheRepository cacheRepository, RedisKeyBuilder keyBuilder) {
        this.cacheRepository = cacheRepository;
        this.keyBuilder = keyBuilder;
    }

    @PostMapping
    public ResponseEntity<PriceWriteResponse> saveLatestPrice(@RequestBody FlightPriceSnapshot snapshot) {
        cacheRepository.saveLatestFlightPrice(snapshot);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PriceWriteResponse("cached", keyBuilder.latestFlightPriceKey(snapshot)));
    }

    @GetMapping("/latest")
    public ResponseEntity<FlightPriceSnapshot> getLatestPrice(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate returnDate
    ) {
        FlightPriceQuery query = new FlightPriceQuery(origin, destination, departureDate, returnDate);
        return ResponseEntity.of(cacheRepository.getLatestFlightPrice(query));
    }

    @GetMapping("/history")
    public ResponseEntity<PriceHistoryResponse<FlightPriceSnapshot>> getPriceHistory() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
