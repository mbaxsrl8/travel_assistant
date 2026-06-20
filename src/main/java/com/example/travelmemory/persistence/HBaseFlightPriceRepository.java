package com.example.travelmemory.persistence;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nonnull;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

import com.example.travelmemory.config.HBaseProperties;
import com.example.travelmemory.exception.PriceHistoryPersistenceException;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class HBaseFlightPriceRepository
        implements PriceHistoryRepository<FlightPriceSnapshot, FlightPriceQuery> {

    private static final int SCAN_CACHING = 100;

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final HBaseRowKeyBuilder rowKeyBuilder;
    private final TableName tableName;

    public HBaseFlightPriceRepository(
            @Lazy Connection connection,
            ObjectMapper objectMapper,
            HBaseRowKeyBuilder rowKeyBuilder,
            HBaseProperties properties
    ) {
        this.connection = connection;
        this.objectMapper = objectMapper;
        this.rowKeyBuilder = rowKeyBuilder;
        this.tableName = TableName.valueOf(properties.flightTable());
    }

    @Override
    public void save(@Nonnull FlightPriceSnapshot snapshot) {
        Put put = new Put(Bytes.toBytes(rowKeyBuilder.flightRowKey(snapshot)));

        HBaseColumnSupport.add(put, HBaseColumnSupport.ORIGIN, snapshot.origin());
        HBaseColumnSupport.add(put, HBaseColumnSupport.DESTINATION, snapshot.destination());
        HBaseColumnSupport.add(put, HBaseColumnSupport.DEPARTURE_DATE, snapshot.departureDate());
        HBaseColumnSupport.add(put, HBaseColumnSupport.RETURN_DATE, snapshot.returnDate());
        HBaseColumnSupport.add(put, HBaseColumnSupport.CARRIER, snapshot.carrier());
        HBaseColumnSupport.add(put, HBaseColumnSupport.PRICE, snapshot.price());
        HBaseColumnSupport.add(put, HBaseColumnSupport.CURRENCY, snapshot.currency());
        HBaseColumnSupport.add(put, HBaseColumnSupport.SOURCE, snapshot.source());
        HBaseColumnSupport.add(put, HBaseColumnSupport.CAPTURED_AT, snapshot.capturedAt());

        try {
            HBaseColumnSupport.addMetadata(put, objectMapper, snapshot.metadata());
            try (Table table = connection.getTable(tableName)) {
                table.put(put);
            }
        } catch (IOException ex) {
            throw new PriceHistoryPersistenceException(
                    "Unable to save flight price snapshot to HBase",
                    ex);
        }
    }

    @Override
    public List<FlightPriceSnapshot> findHistory(@Nonnull FlightPriceQuery query) {
        List<FlightPriceSnapshot> snapshots = new ArrayList<>();

        try (Table table = connection.getTable(tableName)) {
            for (String prefix : rowKeyBuilder.flightScanPrefixes(query)) {
                Scan scan = historyScan(prefix);
                try (ResultScanner scanner = table.getScanner(scan)) {
                    for (Result result : scanner) {
                        FlightPriceSnapshot snapshot = toSnapshot(result);
                        if (matchesReturnDate(snapshot, query)) {
                            snapshots.add(snapshot);
                        }
                    }
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new PriceHistoryPersistenceException(
                    "Unable to query flight price history from HBase",
                    ex);
        }

        snapshots.sort(Comparator.comparing(FlightPriceSnapshot::capturedAt).reversed());
        return List.copyOf(snapshots);
    }

    private Scan historyScan(String prefix) {
        return new Scan()
                .setStartStopRowForPrefixScan(Bytes.toBytes(prefix))
                .addFamily(HBaseColumnSupport.FAMILY)
                .setCaching(SCAN_CACHING)
                .setCacheBlocks(false);
    }

    private FlightPriceSnapshot toSnapshot(Result result) throws IOException {
        String returnDate = HBaseColumnSupport.optional(result, HBaseColumnSupport.RETURN_DATE);
        return new FlightPriceSnapshot(
                HBaseColumnSupport.required(result, HBaseColumnSupport.ORIGIN),
                HBaseColumnSupport.required(result, HBaseColumnSupport.DESTINATION),
                LocalDate.parse(HBaseColumnSupport.required(result, HBaseColumnSupport.DEPARTURE_DATE)),
                returnDate == null ? null : LocalDate.parse(returnDate),
                HBaseColumnSupport.optional(result, HBaseColumnSupport.CARRIER),
                new BigDecimal(HBaseColumnSupport.required(result, HBaseColumnSupport.PRICE)),
                HBaseColumnSupport.required(result, HBaseColumnSupport.CURRENCY),
                HBaseColumnSupport.required(result, HBaseColumnSupport.SOURCE),
                Instant.parse(HBaseColumnSupport.required(result, HBaseColumnSupport.CAPTURED_AT)),
                HBaseColumnSupport.metadata(result, objectMapper));
    }

    private boolean matchesReturnDate(FlightPriceSnapshot snapshot, FlightPriceQuery query) {
        return query.returnDate() == null || Objects.equals(query.returnDate(), snapshot.returnDate());
    }
}
