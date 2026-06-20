package com.example.travelmemory.persistence;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.travelmemory.config.HBaseProperties;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

class HBaseFlightPriceRepositoryTest {

    private Connection connection;
    private Table table;
    private HBaseFlightPriceRepository repository;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() throws IOException {
        connection = mock(Connection.class);
        table = mock(Table.class);
        HBaseProperties properties = properties();

        when(connection.getTable(TableName.valueOf(properties.flightTable())))
                .thenReturn(table);

        repository = new HBaseFlightPriceRepository(
                connection,
                new ObjectMapper().findAndRegisterModules(),
                new HBaseRowKeyBuilder(properties),
                properties);
    }

    @Test
    void savesCompleteFlightSnapshot() throws IOException {
        FlightPriceSnapshot snapshot = flightSnapshot("2026-06-19T09:00:00Z", "715.50");

        repository.save(snapshot);

        ArgumentCaptor<Put> putCaptor = ArgumentCaptor.forClass(Put.class);
        verify(table).put(putCaptor.capture());
        Put put = putCaptor.getValue();

        assertThat(Bytes.toString(put.getRow()))
                .matches("\\d{2}#LAX-NRT#2026-09-01#\\d{19}");
        assertThat(put.getFamilyCellMap().get(HBaseColumnSupport.FAMILY))
                .hasSize(10);
    }

    @Test
    void returnsFlightHistoryNewestFirst() throws IOException {
        Result older = flightResult("2026-06-19T08:00:00Z", "742.35");
        Result newer = flightResult("2026-06-19T09:00:00Z", "715.50");
        ResultScanner scanner = mock(ResultScanner.class);

        when(scanner.iterator()).thenReturn(List.of(older, newer).iterator());
        when(table.getScanner(any(Scan.class))).thenReturn(scanner);

        List<FlightPriceSnapshot> history = repository.findHistory(new FlightPriceQuery(
                "LAX",
                "NRT",
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-14")));

        assertThat(history)
                .extracting(FlightPriceSnapshot::capturedAt)
                .containsExactly(
                        Instant.parse("2026-06-19T09:00:00Z"),
                        Instant.parse("2026-06-19T08:00:00Z"));
    }

    private static Result flightResult(String capturedAt, String price) {
        Result result = mock(Result.class);
        value(result, HBaseColumnSupport.ORIGIN, "LAX");
        value(result, HBaseColumnSupport.DESTINATION, "NRT");
        value(result, HBaseColumnSupport.DEPARTURE_DATE, "2026-09-01");
        value(result, HBaseColumnSupport.RETURN_DATE, "2026-09-14");
        value(result, HBaseColumnSupport.CARRIER, "JL");
        value(result, HBaseColumnSupport.PRICE, price);
        value(result, HBaseColumnSupport.CURRENCY, "USD");
        value(result, HBaseColumnSupport.SOURCE, "amadeus");
        value(result, HBaseColumnSupport.CAPTURED_AT, capturedAt);
        value(result, HBaseColumnSupport.METADATA, "{\"fareClass\":\"economy\"}");
        return result;
    }

    private static void value(Result result, byte[] qualifier, String value) {
        when(result.getValue(HBaseColumnSupport.FAMILY, qualifier))
                .thenReturn(Bytes.toBytes(value));
    }

    private static FlightPriceSnapshot flightSnapshot(String capturedAt, String price) {
        return new FlightPriceSnapshot(
                "LAX",
                "NRT",
                LocalDate.parse("2026-09-01"),
                LocalDate.parse("2026-09-14"),
                "JL",
                new BigDecimal(price),
                "USD",
                "amadeus",
                Instant.parse(capturedAt),
                Map.of("fareClass", "economy"));
    }

    private static HBaseProperties properties() {
        return new HBaseProperties(
                "localhost",
                2181,
                "/hbase",
                "travel_flight_prices",
                "travel_hotel_prices",
                1);
    }
}
