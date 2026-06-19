package com.example.travelmemory.cache;

import java.util.Optional;

import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;

public interface PriceCacheRepository {

    void saveLatestFlightPrice(FlightPriceSnapshot snapshot);

    Optional<FlightPriceSnapshot> getLatestFlightPrice(FlightPriceQuery query);

    void saveLatestHotelPrice(HotelPriceSnapshot snapshot);

    Optional<HotelPriceSnapshot> getLatestHotelPrice(HotelPriceQuery query);
}
