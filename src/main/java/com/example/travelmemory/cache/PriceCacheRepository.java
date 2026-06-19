package com.example.travelmemory.cache;

import java.util.Optional;

import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;

/**
 * Stores and retrieves the most recently captured flight and hotel prices.
 *
 * <p>Implementations use the identifying fields in each snapshot or query to
 * address a cached price independently of its source-specific details.</p>
 */
public interface PriceCacheRepository {

    /**
     * Caches a flight price as the latest snapshot for its itinerary.
     *
     * @param snapshot flight price snapshot to cache
     */
    void saveLatestFlightPrice(FlightPriceSnapshot snapshot);

    /**
     * Retrieves the latest cached flight price matching an itinerary query.
     *
     * @param query identifying itinerary fields used to locate the cached price
     * @return the matching latest snapshot, or an empty optional when no price is cached
     */
    Optional<FlightPriceSnapshot> getLatestFlightPrice(FlightPriceQuery query);

    /**
     * Caches a hotel price as the latest snapshot for its stay.
     *
     * @param snapshot hotel price snapshot to cache
     */
    void saveLatestHotelPrice(HotelPriceSnapshot snapshot);

    /**
     * Retrieves the latest cached hotel price matching a stay query.
     *
     * @param query identifying stay fields used to locate the cached price
     * @return the matching latest snapshot, or an empty optional when no price is cached
     */
    Optional<HotelPriceSnapshot> getLatestHotelPrice(HotelPriceQuery query);
}
