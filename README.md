# Travel Price Memory

Travel Price Memory is a Java 21/Spring Boot service that records flight and hotel price snapshots. HBase is the durable source of truth for price history; Redis keeps the latest snapshot for low-latency reads.

## How it works

- Writes are validated, persisted to HBase, and then cached in Redis.
- A failed HBase write fails the request and leaves Redis unchanged.
- A failed Redis update is logged after the durable write succeeds.
- Latest-price reads use Redis first, fall back to HBase on a miss, and refill the cache.
- History reads query HBase and return snapshots newest first.

## Requirements

- Java 21
- Maven 3.9+
- Docker Desktop with Docker Compose

## Run locally

Start Redis and standalone HBase:

```shell
docker compose up --build -d
docker compose ps
```

When both containers are healthy, start the API:

```shell
mvn spring-boot:run
```

The service listens on `http://localhost:8080`. Redis uses port `6379`, HBase ZooKeeper uses `2181`, and the HBase master UI is available at `http://localhost:16010`.

Store and retrieve a flight price:

```shell
curl -X POST http://localhost:8080/api/v1/flights/prices \
  -H "Content-Type: application/json" \
  -d '{"origin":"LAX","destination":"NRT","departureDate":"2026-09-01","returnDate":"2026-09-14","carrier":"JL","price":715.50,"currency":"USD","source":"amadeus","capturedAt":"2026-06-20T08:00:00Z","metadata":{"fareClass":"economy"}}'

curl "http://localhost:8080/api/v1/flights/prices/latest?origin=LAX&destination=NRT&departureDate=2026-09-01&returnDate=2026-09-14"
```

A ready-to-import Postman collection is at `docs/postman/travel-price-memory.postman_collection.json`.

## Test

```shell
mvn test
```

The unit suite covers key construction, Redis serialization and TTL behavior, HBase writes and history ordering, service failure semantics and cache fallback, and request validation boundaries.

## Configuration

Defaults live in `src/main/resources/application.yml`:

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |
| `travel.cache.latest-price-ttl` | `15m` | Latest-price cache lifetime |
| `travel.hbase.quorum` | `localhost` | HBase ZooKeeper quorum |
| `travel.hbase.client-port` | `2181` | ZooKeeper client port |
| `travel.hbase.flight-table` | `travel_flight_prices` | Flight history table |
| `travel.hbase.hotel-table` | `travel_hotel_prices` | Hotel history table |
| `travel.hbase.salt-buckets` | `16` | Row-key salt bucket count |

## Documentation

- [API design](docs/api-design.md)
- [HBase schema](docs/hbase-schema.md)
- [Architecture](docs/architecture.md)
- [Local development](docs/local-development.md)
- [Interview notes](docs/interview-notes.md)
- [Implementation roadmap](docs/implementation-roadmap.md)

MCP, booking, authentication, alerts, and external travel-provider integration are intentionally outside version 1.
