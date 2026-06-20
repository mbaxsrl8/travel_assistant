package com.example.travelmemory.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.travelmemory.cache.PriceCacheRepository;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;
import com.example.travelmemory.persistence.PriceHistoryRepository;

@Service
public class HotelPriceService {

    private final PriceCacheRepository cacheRepository;
    private final PriceHistoryRepository<HotelPriceSnapshot, HotelPriceQuery> historyRepository;

    public HotelPriceService(
            PriceCacheRepository cacheRepository,
            PriceHistoryRepository<HotelPriceSnapshot, HotelPriceQuery> historyRepository
    ) {
        this.cacheRepository = cacheRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public HotelPriceSnapshot savePrice(HotelPriceSnapshot snapshot) {
        PriceRequestValidator.validate(snapshot);
        cacheRepository.saveLatestHotelPrice(snapshot);
        historyRepository.save(snapshot);
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
