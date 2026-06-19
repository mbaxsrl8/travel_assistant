package com.example.travelmemory.cache;

import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.example.travelmemory.config.CacheProperties;
import com.example.travelmemory.exception.CacheSerializationException;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class RedisPriceCacheRepository implements PriceCacheRepository {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisKeyBuilder keyBuilder;
    private final CacheProperties cacheProperties;

    public RedisPriceCacheRepository(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            RedisKeyBuilder keyBuilder,
            CacheProperties cacheProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.keyBuilder = keyBuilder;
        this.cacheProperties = cacheProperties;
    }

    @Override
    public void saveLatestFlightPrice(FlightPriceSnapshot snapshot) {
        save(keyBuilder.latestFlightPriceKey(snapshot), snapshot);
    }

    @Override
    public Optional<FlightPriceSnapshot> getLatestFlightPrice(FlightPriceQuery query) {
        return get(keyBuilder.latestFlightPriceKey(query), FlightPriceSnapshot.class);
    }

    @Override
    public void saveLatestHotelPrice(HotelPriceSnapshot snapshot) {
        save(keyBuilder.latestHotelPriceKey(snapshot), snapshot);
    }

    @Override
    public Optional<HotelPriceSnapshot> getLatestHotelPrice(HotelPriceQuery query) {
        return get(keyBuilder.latestHotelPriceKey(query), HotelPriceSnapshot.class);
    }

    private void save(String key, Object value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, cacheProperties.latestPriceTtl());
        } catch (JsonProcessingException ex) {
            throw new CacheSerializationException("Unable to serialize latest price snapshot", ex);
        }
    }

    private <T> Optional<T> get(String key, Class<T> valueType) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (JsonProcessingException ex) {
            throw new CacheSerializationException("Unable to deserialize latest price snapshot", ex);
        }
    }
}
