# API Design

The REST API accepts and queries immutable price snapshots. Dates use ISO `YYYY-MM-DD`, timestamps use ISO-8601 UTC values, and prices are JSON numbers backed by Java `BigDecimal`.

## Flight prices

### `POST /api/v1/flights/prices`

Persists a flight snapshot to HBase, then attempts to update Redis.

```json
{
  "origin": "LAX",
  "destination": "NRT",
  "departureDate": "2026-09-01",
  "returnDate": "2026-09-14",
  "carrier": "JL",
  "price": 715.50,
  "currency": "USD",
  "source": "amadeus",
  "capturedAt": "2026-06-20T08:00:00Z",
  "metadata": { "fareClass": "economy" }
}
```

`returnDate` and `carrier` may be `null`. A successful request returns `201 Created`:

```json
{
  "status": "saved",
  "cacheKey": "flight:latest:lax:nrt:2026-09-01:2026-09-14"
}
```

### `GET /api/v1/flights/prices/latest`

Required query parameters: `origin`, `destination`, and `departureDate`. `returnDate` is optional. Returns `200 OK` with a flight snapshot, or `404 Not Found` if neither Redis nor HBase has a match.

### `GET /api/v1/flights/prices/history`

Uses the same query parameters as the latest endpoint. Returns `200 OK` and snapshots ordered newest first:

```json
{
  "snapshots": [
    {
      "origin": "LAX",
      "destination": "NRT",
      "departureDate": "2026-09-01",
      "returnDate": "2026-09-14",
      "carrier": "JL",
      "price": 715.50,
      "currency": "USD",
      "source": "amadeus",
      "capturedAt": "2026-06-20T08:00:00Z",
      "metadata": { "fareClass": "economy" }
    }
  ]
}
```

## Hotel prices

### `POST /api/v1/hotels/prices`

Persists a hotel snapshot to HBase, then attempts to update Redis.

```json
{
  "city": "Tokyo",
  "hotelName": "Park Hotel",
  "checkInDate": "2026-09-01",
  "checkOutDate": "2026-09-05",
  "roomType": "King",
  "price": 220.00,
  "currency": "USD",
  "source": "booking",
  "capturedAt": "2026-06-20T08:00:00Z",
  "metadata": { "refundable": "true" }
}
```

`roomType` may be `null`. A successful request returns `201 Created`:

```json
{
  "status": "saved",
  "cacheKey": "hotel:latest:tokyo:park-hotel:2026-09-01:2026-09-05"
}
```

### `GET /api/v1/hotels/prices/latest`

Required query parameters: `city`, `hotelName`, `checkInDate`, and `checkOutDate`. Returns `200 OK` with a hotel snapshot, or `404 Not Found` when no match exists.

### `GET /api/v1/hotels/prices/history`

Uses the same four query parameters and returns `200 OK` with a `snapshots` array ordered newest first.

## Validation rules

- Flight origin, destination, departure date, price, currency, source, and captured timestamp are required.
- A flight return date, when supplied, cannot be before departure.
- Hotel city, hotel name, check-in, check-out, price, currency, source, and captured timestamp are required.
- Hotel check-out must be strictly after check-in.
- Every price must be greater than zero.

Validation failures raise `InvalidPriceRequestException`. Version 1 does not define a custom public error envelope; adding a controller advice that maps domain validation failures to a stable `400 Bad Request` contract is a recommended API-hardening step.

## Consistency semantics

HBase is authoritative. A persistence failure fails a write before Redis is touched. If HBase succeeds but Redis fails, the request remains successful because the durable snapshot exists. A later cache miss can recover the latest value from HBase.
