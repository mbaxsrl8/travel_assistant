# Travel Price Memory System — Product Requirements

## 1. Project Goal

Build a backend service that stores and retrieves flight and hotel prices for a personal travel agent.

Version 1 uses:

* Java 21
* Spring Boot
* Redis as the low-latency cache
* HBase as the persistent historical storage layer
* REST APIs for ingestion and query

Future versions may add:

* Apache Geode implementation
* MCP server for AI agent access
* price alerting
* trend analysis
* recommendation logic

## 2. Core Use Case

A travel agent fetches live prices from external sources. This system stores those prices so the agent can later answer questions like:

* What was the latest price for LAX to Tokyo?
* Has this hotel become cheaper recently?
* What is the historical price trend for this route?
* Should I wait or book now?

## 3. Version 1 Scope

### In Scope

* Store latest flight price in Redis
* Store latest hotel price in Redis
* Persist price snapshots into HBase
* Query latest price from Redis
* Query historical prices from HBase
* Basic REST API
* Docker Compose local environment
* Unit tests and integration tests

### Out of Scope

* Real flight booking
* Real hotel booking
* Payment handling
* User authentication
* Real external travel API integration
* MCP server implementation
* Geode implementation

## 4. Main Entities

### Flight Price

Fields:

* origin airport code
* destination airport code
* departure date
* return date, optional
* carrier, optional
* price
* currency
* source
* captured timestamp

### Hotel Price

Fields:

* city
* hotel name
* check-in date
* check-out date
* room type, optional
* price
* currency
* source
* captured timestamp

## 5. Success Criteria

The project is successful when:

* A client can write flight and hotel prices through REST APIs
* Latest price can be retrieved quickly from Redis
* Historical price data can be retrieved from HBase
* Redis keys use TTL to avoid stale cache data
* HBase row keys are designed for time-series queries
* The codebase is modular enough to replace Redis with Geode later
* The project can be explained clearly in backend/distributed-system interviews
