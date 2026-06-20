# Interview Notes

## Thirty-second summary

Travel Price Memory is a Spring Boot service that keeps immutable flight and hotel price history in HBase and caches the latest snapshot in Redis. The service persists first because HBase is authoritative, treats Redis failures as recoverable, and uses cache-aside reads so misses refill from durable history. Salted reverse-timestamp HBase keys distribute writes while preserving newest-first ordering inside each bucket.

## Why two data stores?

Redis is excellent for a small, frequently read latest-value working set, TTLs, and low latency. It is not the long-term record. HBase is designed for high-volume sparse time-series data and prefix-based access patterns, so it owns history. Separating the roles prevents cache availability or eviction from becoming a durability problem.

## Why persist before caching?

Writing Redis first can expose a price that was never durably recorded if the later HBase write fails. Writing HBase first preserves the key invariant: anything acknowledged is durable. If Redis then fails, the application logs the failure and still succeeds; the next cache miss can reconstruct the latest value from HBase.

This is not an atomic distributed transaction. It is an intentionally asymmetric consistency model based on an authoritative store and a disposable cache.

## Why salted reverse timestamps?

An unsalted route/date prefix concentrates sequential writes in one HBase region. A salt spreads those writes across buckets. `Long.MAX_VALUE - epochMillis` places recent records first lexicographically within a bucket, making newest-first scans natural.

The cost is read amplification: history reads scan every salt bucket and merge results. Bucket count balances write distribution against query fan-out.

## Failure behavior

| Failure | Result |
| --- | --- |
| Request validation fails | No persistence or cache interaction |
| HBase write fails | Write fails; Redis is not updated |
| Redis update fails after HBase write | Write succeeds; warning is logged |
| Redis latest lookup misses | Query HBase, return newest snapshot, refill Redis |
| No snapshot exists | Latest endpoint returns 404 |

One nuance worth calling out: latest-read cache refills are not currently guarded against Redis failure, unlike write-path cache updates. A production hardening pass should make cache failures uniformly non-fatal.

## Testing story

Service tests mock Redis and HBase boundaries, which verifies orchestration independently of infrastructure. They cover ordering, failure behavior, cache hits, misses, refills, empty results, history delegation, and invalid input. Repository tests verify HBase serialization and newest-first history reconstruction. Dedicated tests cover Redis keys, Redis JSON/TTL behavior, HBase keys, salt prefixes, reverse timestamps, and every validation rule.

The next testing layer would use containers to verify the wire-level behavior against real Redis and HBase, including TTL expiry, multi-bucket scans, table configuration, and application startup.

## Design alternatives

- PostgreSQL is simpler for moderate history volume and flexible relational queries. HBase is educationally useful here because the access pattern is explicit and the row-key trade-offs matter.
- Redis Streams or Kafka could decouple ingestion, but they add operational complexity and eventual-consistency decisions that version 1 does not need.
- A transactional outbox could make cache refresh/retry reliable without a distributed transaction.
- Geode could replace the cache behind `PriceCacheRepository`; controllers and services would remain unchanged.

## Production hardening backlog

- Add a stable API error envelope and map validation errors to HTTP 400.
- Treat cache read/refill errors as misses and add metrics around them.
- Add pagination and capture-time ranges to history endpoints.
- Add authentication, authorization, rate limiting, and request size limits.
- Add retries/circuit breakers selectively; never retry non-idempotent work blindly.
- Add observability for cache hit rate, HBase latency, scan fan-out, and partial cache failures.
- Pre-split HBase regions and define backup, retention, and disaster-recovery policies.
- Add real-infrastructure integration and end-to-end tests.

## Future MCP boundary

An MCP server should call the service layer or REST API rather than Redis or HBase directly. That preserves validation, consistency semantics, and storage abstraction for tools such as `get_latest_flight_price` and `get_hotel_price_history`.
