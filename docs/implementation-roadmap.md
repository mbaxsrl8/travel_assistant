# Implementation Roadmap for Codex

## Phase 1 — Project Setup

Create a Java 21 Spring Boot project.

Required dependencies:

* Spring Web
* Spring Data Redis
* HBase client
* Jackson
* Lombok, optional
* JUnit 5
* Testcontainers, optional

Create initial packages:

```text
api
service
cache
persistence
model
config
exception
```

## Phase 2 — Domain Models

Create models:

```java
FlightPriceSnapshot
HotelPriceSnapshot
FlightPriceQuery
HotelPriceQuery
PriceHistoryResponse
```

Use `BigDecimal` for price.

Use `Instant` for captured timestamp.

## Phase 3 — REST API

Create endpoints:

```text
POST /api/v1/flights/prices
GET  /api/v1/flights/prices/latest
GET  /api/v1/flights/prices/history

POST /api/v1/hotels/prices
GET  /api/v1/hotels/prices/latest
GET  /api/v1/hotels/prices/history
```

## Phase 4 — Redis Cache Layer

Implement:

```java
PriceCacheRepository
RedisPriceCacheRepository
RedisKeyBuilder
```

Requirements:

* Save latest flight price
* Save latest hotel price
* Apply TTL
* Read latest flight price
* Read latest hotel price
* Serialize data as JSON

## Phase 5 — HBase Persistence Layer

Implement:

```java
PriceHistoryRepository
HBaseFlightPriceRepository
HBaseHotelPriceRepository
HBaseRowKeyBuilder
```

Requirements:

* Save flight price snapshot
* Save hotel price snapshot
* Query flight price history by route and date
* Query hotel price history by city, hotel, and check-in date
* Use salted row keys
* Use reverse timestamp for newest-first reads

## Phase 6 — Service Layer

Implement:

```java
FlightPriceService
HotelPriceService
```

Write flow:

```text
1. Validate request
2. Create price snapshot
3. Save latest price to Redis
4. Save historical snapshot to HBase
5. Return success response
```

Read latest flow:

```text
1. Try Redis
2. If cache hit, return cached value
3. If cache miss, query latest snapshot from HBase
4. Refill Redis
5. Return response
```

Read history flow:

```text
1. Query HBase
2. Return ordered historical snapshots
```

## Phase 7 — Local Development

Create `docker-compose.yml` for:

* Redis
* HBase or standalone HBase-compatible local setup

If HBase local setup is too heavy, first implement the repository interface and provide an in-memory fake implementation for tests.

## Phase 8 — Testing

Add tests for:

* Redis key generation
* HBase row key generation
* service write flow
* service read latest flow
* Redis cache miss fallback
* HBase history query
* invalid request handling

## Phase 9 — Documentation

Create:

```text
README.md
docs/api-design.md
docs/hbase-schema.md
docs/interview-notes.md
```

## Phase 10 — Future MCP Server

Do not implement MCP in version 1.

Prepare the design so a future MCP server can expose tools like:

```text
get_latest_flight_price
get_flight_price_history
get_latest_hotel_price
get_hotel_price_history
```

The MCP server should call the service layer or REST APIs instead of accessing Redis/HBase directly.
