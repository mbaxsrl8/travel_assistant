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
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class HBaseHotelPriceRepository
        implements PriceHistoryRepository<HotelPriceSnapshot, HotelPriceQuery> {

    private static final int SCAN_CACHING = 100;

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final HBaseRowKeyBuilder rowKeyBuilder;
    private final TableName tableName;

    public HBaseHotelPriceRepository(
            @Lazy Connection connection,
            ObjectMapper objectMapper,
            HBaseRowKeyBuilder rowKeyBuilder,
            HBaseProperties properties
    ) {
        this.connection = connection;
        this.objectMapper = objectMapper;
        this.rowKeyBuilder = rowKeyBuilder;
        this.tableName = TableName.valueOf(properties.hotelTable());
    }

    @Override
    public void save(@Nonnull HotelPriceSnapshot snapshot) {
        Put put = new Put(Bytes.toBytes(rowKeyBuilder.hotelRowKey(snapshot)));

        HBaseColumnSupport.add(put, HBaseColumnSupport.CITY, snapshot.city());
        HBaseColumnSupport.add(put, HBaseColumnSupport.HOTEL_NAME, snapshot.hotelName());
        HBaseColumnSupport.add(put, HBaseColumnSupport.CHECK_IN_DATE, snapshot.checkInDate());
        HBaseColumnSupport.add(put, HBaseColumnSupport.CHECK_OUT_DATE, snapshot.checkOutDate());
        HBaseColumnSupport.add(put, HBaseColumnSupport.ROOM_TYPE, snapshot.roomType());
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
                    "Unable to save hotel price snapshot to HBase",
                    ex);
        }
    }

    @Override
    public List<HotelPriceSnapshot> findHistory(@Nonnull HotelPriceQuery query) {
        List<HotelPriceSnapshot> snapshots = new ArrayList<>();

        try (Table table = connection.getTable(tableName)) {
            for (String prefix : rowKeyBuilder.hotelScanPrefixes(query)) {
                Scan scan = historyScan(prefix);
                try (ResultScanner scanner = table.getScanner(scan)) {
                    for (Result result : scanner) {
                        HotelPriceSnapshot snapshot = toSnapshot(result);
                        if (matchesCheckOutDate(snapshot, query)) {
                            snapshots.add(snapshot);
                        }
                    }
                }
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw new PriceHistoryPersistenceException(
                    "Unable to query hotel price history from HBase",
                    ex);
        }

        snapshots.sort(Comparator.comparing(HotelPriceSnapshot::capturedAt).reversed());
        return List.copyOf(snapshots);
    }

    private Scan historyScan(String prefix) {
        return new Scan()
                .setStartStopRowForPrefixScan(Bytes.toBytes(prefix))
                .addFamily(HBaseColumnSupport.FAMILY)
                .setCaching(SCAN_CACHING)
                .setCacheBlocks(false);
    }

    private HotelPriceSnapshot toSnapshot(Result result) throws IOException {
        String checkOutDate = HBaseColumnSupport.optional(result, HBaseColumnSupport.CHECK_OUT_DATE);
        return new HotelPriceSnapshot(
                HBaseColumnSupport.required(result, HBaseColumnSupport.CITY),
                HBaseColumnSupport.required(result, HBaseColumnSupport.HOTEL_NAME),
                LocalDate.parse(HBaseColumnSupport.required(result, HBaseColumnSupport.CHECK_IN_DATE)),
                checkOutDate == null ? null : LocalDate.parse(checkOutDate),
                HBaseColumnSupport.optional(result, HBaseColumnSupport.ROOM_TYPE),
                new BigDecimal(HBaseColumnSupport.required(result, HBaseColumnSupport.PRICE)),
                HBaseColumnSupport.required(result, HBaseColumnSupport.CURRENCY),
                HBaseColumnSupport.required(result, HBaseColumnSupport.SOURCE),
                Instant.parse(HBaseColumnSupport.required(result, HBaseColumnSupport.CAPTURED_AT)),
                HBaseColumnSupport.metadata(result, objectMapper));
    }

    private boolean matchesCheckOutDate(HotelPriceSnapshot snapshot, HotelPriceQuery query) {
        return query.checkOutDate() == null || Objects.equals(query.checkOutDate(), snapshot.checkOutDate());
    }
}
