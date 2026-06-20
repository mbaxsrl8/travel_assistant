package com.example.travelmemory.service;

import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.example.travelmemory.cache.PriceCacheRepository;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.persistence.PriceHistoryRepository;

@Service
public class FlightPriceService {

    private static final Log log = LogFactory.getLog(FlightPriceService.class);

    private final PriceCacheRepository cacheRepository;
    private final PriceHistoryRepository<FlightPriceSnapshot, FlightPriceQuery> historyRepository;

    public FlightPriceService(
            PriceCacheRepository cacheRepository,
            PriceHistoryRepository<FlightPriceSnapshot, FlightPriceQuery> historyRepository
    ) {
        this.cacheRepository = cacheRepository;
        this.historyRepository = historyRepository;
    }

    public void savePrice(FlightPriceSnapshot snapshot) {
        PriceRequestValidator.validate(snapshot);

        // 1. Persist first: HBase is the source of truth
        historyRepository.save(snapshot);

        // 2. Cache second: Redis is only an optimization
        try {
            cacheRepository.saveLatestFlightPrice(snapshot);
        } catch (Exception e) {
            log.warn("Failed to update latest flight price cache. snapshot=" + snapshot, e);
        }

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
