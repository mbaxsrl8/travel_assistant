package com.example.travelmemory.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.example.travelmemory.config.CacheProperties;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.model.HotelPriceSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.embedded.RedisServer;

class RedisPriceCacheRepositoryTest {

    private static RedisServer redisServer;
    private static int redisPort;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private RedisPriceCacheRepository repository;

    @BeforeAll
    static void startRedis() throws IOException {
        redisPort = findAvailablePort();
        redisServer = RedisServer.newRedisServer()
                .port(redisPort)
                .bind("127.0.0.1")
                .setting("save \"\"")
                .setting("appendonly no")
                .build();
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() throws IOException {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", redisPort);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }

        repository = repositoryWithTtl(Duration.ofMinutes(15));
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void savesAndReadsLatestFlightPrice() {
        FlightPriceSnapshot snapshot = flightSnapshot();

        repository.saveLatestFlightPrice(snapshot);

        assertThat(repository.getLatestFlightPrice(snapshot.toQuery()))
                .contains(snapshot);
    }

    @Test
    void savesAndReadsLatestHotelPrice() {
        HotelPriceSnapshot snapshot = hotelSnapshot();

        repository.saveLatestHotelPrice(snapshot);

        assertThat(repository.getLatestHotelPrice(snapshot.toQuery()))
                .contains(snapshot);
    }

    @Test
    void returnsEmptyWhenPriceIsMissing() {
        FlightPriceQuery query = new FlightPriceQuery(
                "JFK",
                "CDG",
                LocalDate.parse("2026-11-01"),
                LocalDate.parse("2026-11-08"));

        assertThat(repository.getLatestFlightPrice(query))
                .isEmpty();
    }

    @Test
    void appliesConfiguredTtlWhenSavingLatestPrice() {
        repository = repositoryWithTtl(Duration.ofSeconds(30));
        FlightPriceSnapshot snapshot = flightSnapshot();
        String key = new RedisKeyBuilder().latestFlightPriceKey(snapshot);

        repository.saveLatestFlightPrice(snapshot);

        assertThat(redisTemplate.getExpire(key))
                .isBetween(1L, 30L);
    }

    private RedisPriceCacheRepository repositoryWithTtl(Duration ttl) {
        return new RedisPriceCacheRepository(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules(),
                new RedisKeyBuilder(),
                new CacheProperties(ttl));
    }

    private static FlightPriceSnapshot flightSnapshot() {
        return new FlightPriceSnapshot(
                "LAX",
                "NRT",
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-14"),
                "JL",
                new BigDecimal("742.35"),
                "USD",
                "amadeus",
                Instant.parse("2026-06-18T14:30:00Z"),
                Map.of("fareClass", "economy", "stops", "0"));
    }

    private static HotelPriceSnapshot hotelSnapshot() {
        return new HotelPriceSnapshot(
                "Tokyo",
                "Park Hotel",
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-05"),
                "King",
                new BigDecimal("219.99"),
                "USD",
                "booking",
                Instant.parse("2026-06-18T14:35:00Z"),
                Map.of("refundable", "true", "breakfast", "included"));
    }

    private static int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
