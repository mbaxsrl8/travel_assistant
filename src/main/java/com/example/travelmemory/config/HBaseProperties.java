package com.example.travelmemory.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "travel.hbase")
public record HBaseProperties(
        String quorum,
        int clientPort,
        String znodeParent,
        String flightTable,
        String hotelTable,
        int saltBuckets
) {
    public HBaseProperties {
        quorum = defaultIfBlank(quorum, "localhost");
        clientPort = clientPort > 0 ? clientPort : 2181;
        znodeParent = defaultIfBlank(znodeParent, "/hbase");
        flightTable = defaultIfBlank(flightTable, "travel_flight_prices");
        hotelTable = defaultIfBlank(hotelTable, "travel_hotel_prices");
        saltBuckets = saltBuckets > 0 ? saltBuckets : 16;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
