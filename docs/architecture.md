# Technical Architecture

## 1. High-Level Architecture

```text
Client / Travel Agent
        ↓
Spring Boot REST API
        ↓
Service Layer
        ↓
Cache Layer: Redis
        ↓
Persistence Layer: HBase
```

Version 1 uses a cache-aside design with durable-first writes:

```text
Write price:
API → Service → HBase historical snapshot → Redis latest price

Read latest price:
API → Redis
        ↓ cache miss
      HBase fallback

Read history:
API → HBase
```

## 2. Technology Choices

### Redis

Redis is used for fast lookup of the latest known price.

Use Redis for:

* latest flight price
* latest hotel price
* short TTL cache
* fast read path
* future rate limiting or temporary agent state

Redis should not be treated as the long-term source of truth.

### HBase

HBase is used for persistent historical price storage.

Use HBase for:

* price snapshots
* time-series queries
* route-level history
* hotel-level history
* future analytics

HBase is the source of truth for historical data.

## 3. Suggested Project Structure

```text
travel-price-memory/
├── README.md
├── docker-compose.yml
├── docs/
│   ├── product-requirements.md
│   ├── architecture.md
│   ├── api-design.md
│   ├── hbase-schema.md
│   └── roadmap.md
├── src/main/java/com/example/travelmemory/
│   ├── TravelMemoryApplication.java
│   ├── api/
│   ├── service/
│   ├── cache/
│   ├── persistence/
│   ├── model/
│   ├── config/
│   └── exception/
└── src/test/
```

## 4. Module Responsibilities

### api

REST controllers.

Example classes:

* `FlightPriceController`
* `HotelPriceController`

### service

Business logic.

Example classes:

* `FlightPriceService`
* `HotelPriceService`
* `PriceQueryService`

### cache

Redis operations.

Example classes:

* `PriceCacheRepository`
* `RedisKeyBuilder`

### persistence

HBase operations.

Example classes:

* `FlightPriceHBaseRepository`
* `HotelPriceHBaseRepository`
* `HBaseConnectionFactory`

### model

Domain objects and DTOs.

Example classes:

* `FlightPriceSnapshot`
* `HotelPriceSnapshot`
* `PriceHistoryResponse`

## 5. Redis Key Design

### Latest Flight Price

```text
flight:latest:{origin}:{destination}:{departureDate}:{returnDate}
```

Example:

```text
flight:latest:LAX:NRT:2026-09-01:2026-09-14
```

### Latest Hotel Price

```text
hotel:latest:{city}:{hotelName}:{checkInDate}:{checkOutDate}
```

Example:

```text
hotel:latest:tokyo:park-hotel:2026-09-01:2026-09-05
```

## 6. Redis TTL

Use TTL to prevent stale latest-price cache.

Recommended default:

```text
15 minutes
```

Make TTL configurable through application properties.

## 7. HBase Table Design

Use two tables:

```text
travel_flight_prices
travel_hotel_prices
```

### Flight Price Row Key

```text
{salt}#{origin}-{destination}#{departureDate}#{reverseTimestamp}
```

Example:

```text
03#LAX-NRT#2026-09-01#9998293847561
```

### Hotel Price Row Key

```text
{salt}#{city}#{hotelName}#{checkInDate}#{reverseTimestamp}
```

Example:

```text
07#tokyo#park-hotel#2026-09-01#9998293847561
```

## 8. Column Families

Use one column family:

```text
p
```

Columns:

```text
p:price
p:currency
p:source
p:capturedAt
p:metadata
```

## 9. Important Design Principles

* Redis is optimized for latest lookup
* HBase is optimized for historical queries
* Write operations persist to HBase before attempting to update Redis
* A failed HBase write must not update Redis
* A failed Redis update after a successful HBase write is logged and does not undo durability
* HBase writes should be idempotent
* Redis cache misses should fallback to HBase when possible
* Row keys should avoid hot partitions using salt
* Service layer should hide Redis/HBase details from controllers

## 10. Future Geode Compatibility

The cache layer should use an interface:

```java
public interface PriceCacheRepository {
    void saveLatestFlightPrice(FlightPriceSnapshot snapshot);
    Optional<FlightPriceSnapshot> getLatestFlightPrice(FlightPriceQuery query);

    void saveLatestHotelPrice(HotelPriceSnapshot snapshot);
    Optional<HotelPriceSnapshot> getLatestHotelPrice(HotelPriceQuery query);
}
```

Redis will be the first implementation.

Future implementation:

```java
GeodePriceCacheRepository
```

This allows the project to compare Redis cache-aside design with Geode event-driven data fabric design later.
