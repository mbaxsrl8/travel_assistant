# Local Development

## Prerequisites

- Docker Desktop with Docker Compose
- Java 21

## Start Redis and HBase

From the repository root, run:

```shell
docker compose up --build -d
docker compose ps
```

The first HBase build downloads the Apache HBase 2.5.8 binary distribution. The
HBase container starts in standalone mode and creates these tables automatically:

- `travel_flight_prices`
- `travel_hotel_prices`

Both tables use the `p` column family expected by the repositories.

The application defaults in `application.yml` connect to the exposed local
services without additional configuration:

| Service | Local endpoint |
| --- | --- |
| Redis | `localhost:6379` |
| HBase ZooKeeper | `localhost:2181` |
| HBase master UI | `http://localhost:16010` |
| HBase region server UI | `http://localhost:16030` |

Start the application after both containers report healthy:

```shell
mvn spring-boot:run
```

## Stop the Environment

Stop the containers while preserving local data:

```shell
docker compose down
```

To also remove Redis and HBase data:

```shell
docker compose down --volumes
```
