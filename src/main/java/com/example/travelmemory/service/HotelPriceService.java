package com.example.travelmemory.service;

import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import com.example.travelmemory.cache.PriceCacheRepository;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;
import com.example.travelmemory.persistence.PriceHistoryRepository;

@Service
public class HotelPriceService {

    private static final Log log = LogFactory.getLog(HotelPriceService.class);

    private final PriceCacheRepository cacheRepository;
    private final PriceHistoryRepository<HotelPriceSnapshot, HotelPriceQuery> historyRepository;

    public HotelPriceService(
            PriceCacheRepository cacheRepository,
            PriceHistoryRepository<HotelPriceSnapshot, HotelPriceQuery> historyRepository
    ) {
        this.cacheRepository = cacheRepository;
        this.historyRepository = historyRepository;
    }

    public HotelPriceSnapshot savePrice(HotelPriceSnapshot snapshot) {
        PriceRequestValidator.validate(snapshot);

        // 1. Persist first: HBase is the source of truth
        historyRepository.save(snapshot);

        // 2. Cache second: Redis is only an optimization
        try {
            cacheRepository.saveLatestHotelPrice(snapshot);
        } catch (Exception e) {
            log.warn("Failed to update latest hotel price cache. snapshot=" + snapshot, e);
        }

        return snapshot;
    }

    public Optional<HotelPriceSnapshot> getLatestPrice(HotelPriceQuery query) {
        PriceRequestValidator.validate(query);

        Optional<HotelPriceSnapshot> cached = cacheRepository.getLatestHotelPrice(query);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<HotelPriceSnapshot> latest = historyRepository.findHistory(query).stream().findFirst();
        latest.ifPresent(cacheRepository::saveLatestHotelPrice);
        return latest;
    }

    public List<HotelPriceSnapshot> getPriceHistory(HotelPriceQuery query) {
        PriceRequestValidator.validate(query);
        return historyRepository.findHistory(query);
    }
}
