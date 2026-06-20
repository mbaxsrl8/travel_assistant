# HBase Schema

## Tables

| Table | Purpose | Column family |
| --- | --- | --- |
| `travel_flight_prices` | Immutable flight price history | `p` |
| `travel_hotel_prices` | Immutable hotel price history | `p` |

The Docker entrypoint creates both tables if they do not exist. Table names and the number of salt buckets are configurable through `travel.hbase.*` properties.

## Row keys

Flight rows use:

```text
{salt}#{ORIGIN}-{DESTINATION}#{departureDate}#{reverseTimestamp}
```

Example:

```text
03#LAX-NRT#2026-09-01#9223370262105175807
```

Hotel rows use:

```text
{salt}#{city}#{hotelName}#{checkInDate}#{reverseTimestamp}
```

Example:

```text
07#tokyo#park-hotel#2026-09-01#9223370262105175807
```

Airport codes are normalized to uppercase. City and hotel values are lowercase; non-alphanumeric runs become hyphens. The salt is a deterministic hash of the series identity and capture timestamp. With the default 16 buckets, it ranges from `00` to `15`.

The final component is zero-padded to 19 digits and calculated as:

```text
Long.MAX_VALUE - capturedAt.toEpochMilli()
```

This makes newer snapshots sort before older snapshots within one salted series while the salt spreads writes across regions to reduce hot-spotting.

## Columns

All values are stored as UTF-8 strings under column family `p`.

| Qualifier | Flight | Hotel | Notes |
| --- | --- | --- | --- |
| `origin` | yes | — | Original airport value |
| `destination` | yes | — | Original airport value |
| `departureDate` | yes | — | ISO local date |
| `returnDate` | optional | — | ISO local date |
| `carrier` | optional | — | Carrier code/name |
| `city` | — | yes | Original city value |
| `hotelName` | — | yes | Original hotel value |
| `checkInDate` | — | yes | ISO local date |
| `checkOutDate` | — | yes | ISO local date |
| `roomType` | — | optional | Room description |
| `price` | yes | yes | Decimal string |
| `currency` | yes | yes | Currency code |
| `source` | yes | yes | Provider identifier |
| `capturedAt` | yes | yes | ISO-8601 instant |
| `metadata` | yes | yes | JSON object; defaults to `{}` |

## Query strategy

A history lookup creates one prefix scan per salt bucket. For example, a default flight lookup scans `00#LAX-NRT#2026-09-01#` through `15#LAX-NRT#2026-09-01#`. Results from all buckets are decoded, optionally filtered by return/check-out date, merged, and sorted by `capturedAt` descending.

This is a deliberate trade-off: salting distributes writes but turns one logical history query into `N` scans. The default of 16 buckets is suitable for the learning-scale workload and should be measured before production use.

## Operational considerations

- Row keys are idempotent for identical identity and millisecond capture time; the same snapshot overwrites the same row.
- `Scan` caching is set to 100 and block caching is disabled for history scans.
- Version 1 has no history limit, time-range bound, retention policy, secondary index, or pagination.
- Production deployments should pre-split regions by salt, define retention, bound queries, and monitor scan latency and table growth.
