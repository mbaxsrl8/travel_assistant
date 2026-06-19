package com.example.travelmemory.persistence;

import java.io.IOException;
import java.util.Map;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

final class HBaseColumnSupport {

    static final byte[] FAMILY = Bytes.toBytes("p");

    static final byte[] ORIGIN = Bytes.toBytes("origin");
    static final byte[] DESTINATION = Bytes.toBytes("destination");
    static final byte[] DEPARTURE_DATE = Bytes.toBytes("departureDate");
    static final byte[] RETURN_DATE = Bytes.toBytes("returnDate");
    static final byte[] CARRIER = Bytes.toBytes("carrier");

    static final byte[] CITY = Bytes.toBytes("city");
    static final byte[] HOTEL_NAME = Bytes.toBytes("hotelName");
    static final byte[] CHECK_IN_DATE = Bytes.toBytes("checkInDate");
    static final byte[] CHECK_OUT_DATE = Bytes.toBytes("checkOutDate");
    static final byte[] ROOM_TYPE = Bytes.toBytes("roomType");

    static final byte[] PRICE = Bytes.toBytes("price");
    static final byte[] CURRENCY = Bytes.toBytes("currency");
    static final byte[] SOURCE = Bytes.toBytes("source");
    static final byte[] CAPTURED_AT = Bytes.toBytes("capturedAt");
    static final byte[] METADATA = Bytes.toBytes("metadata");

    private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {
    };

    private HBaseColumnSupport() {
    }

    static void add(Put put, byte[] qualifier, Object value) {
        if (value != null) {
            put.addColumn(FAMILY, qualifier, Bytes.toBytes(value.toString()));
        }
    }

    static void addMetadata(Put put, ObjectMapper objectMapper, Map<String, String> metadata)
            throws IOException {
        Map<String, String> value = metadata == null ? Map.of() : metadata;
        add(put, METADATA, objectMapper.writeValueAsString(value));
    }

    static String required(Result result, byte[] qualifier) throws IOException {
        String value = optional(result, qualifier);
        if (value == null) {
            throw new IOException("Missing HBase column p:" + Bytes.toString(qualifier));
        }
        return value;
    }

    static String optional(Result result, byte[] qualifier) {
        byte[] value = result.getValue(FAMILY, qualifier);
        return value == null ? null : Bytes.toString(value);
    }

    static Map<String, String> metadata(Result result, ObjectMapper objectMapper) throws IOException {
        String json = optional(result, METADATA);
        return json == null || json.isBlank()
                ? Map.of()
                : objectMapper.readValue(json, METADATA_TYPE);
    }
}
