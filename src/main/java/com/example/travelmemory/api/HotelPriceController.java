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
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;

@RestController
@RequestMapping("/api/v1/hotels/prices")
public class HotelPriceController {

    private final PriceCacheRepository cacheRepository;
    private final RedisKeyBuilder keyBuilder;

    public HotelPriceController(PriceCacheRepository cacheRepository, RedisKeyBuilder keyBuilder) {
        this.cacheRepository = cacheRepository;
        this.keyBuilder = keyBuilder;
    }

    @PostMapping
    public ResponseEntity<PriceWriteResponse> saveLatestPrice(@RequestBody HotelPriceSnapshot snapshot) {
        cacheRepository.saveLatestHotelPrice(snapshot);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PriceWriteResponse("cached", keyBuilder.latestHotelPriceKey(snapshot)));
    }

    @GetMapping("/latest")
    public ResponseEntity<HotelPriceSnapshot> getLatestPrice(
            @RequestParam String city,
            @RequestParam String hotelName,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate
    ) {
        HotelPriceQuery query = new HotelPriceQuery(city, hotelName, checkInDate, checkOutDate);
        return ResponseEntity.of(cacheRepository.getLatestHotelPrice(query));
    }

    @GetMapping("/history")
    public ResponseEntity<PriceHistoryResponse<HotelPriceSnapshot>> getPriceHistory() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
