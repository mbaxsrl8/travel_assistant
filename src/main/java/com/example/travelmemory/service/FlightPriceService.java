package com.example.travelmemory.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.travelmemory.cache.PriceCacheRepository;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.persistence.PriceHistoryRepository;

@Service
public class FlightPriceService {

    private final PriceCacheRepository cacheRepository;
    private final PriceHistoryRepository<FlightPriceSnapshot, FlightPriceQuery> historyRepository;

    public FlightPriceService(
            PriceCacheRepository cacheRepository,
            PriceHistoryRepository<FlightPriceSnapshot, FlightPriceQuery> historyRepository
    ) {
        this.cacheRepository = cacheRepository;
        this.historyRepository = historyRepository;
    }

    public FlightPriceSnapshot savePrice(FlightPriceSnapshot snapshot) {
        PriceRequestValidator.validate(snapshot);
        cacheRepository.saveLatestFlightPrice(snapshot);
        historyRepository.save(snapshot);
        return snapshot;
    }

    public Optional<FlightPriceSnapshot> getLatestPrice(FlightPriceQuery query) {
        PriceRequestValidator.validate(query);

        Optional<FlightPriceSnapshot> cached = cacheRepository.getLatestFlightPrice(query);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<FlightPriceSnapshot> latest = historyRepository.findHistory(query).stream().findFirst();
        latest.ifPresent(cacheRepository::saveLatestFlightPrice);
        return latest;
    }

    public List<FlightPriceSnapshot> getPriceHistory(FlightPriceQuery query) {
        PriceRequestValidator.validate(query);
        return historyRepository.findHistory(query);
    }
}
