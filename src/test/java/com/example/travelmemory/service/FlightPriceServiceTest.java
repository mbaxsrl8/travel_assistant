package com.example.travelmemory.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.travelmemory.cache.PriceCacheRepository;
import com.example.travelmemory.exception.InvalidPriceRequestException;
import com.example.travelmemory.model.FlightPriceQuery;
import com.example.travelmemory.model.FlightPriceSnapshot;
import com.example.travelmemory.persistence.PriceHistoryRepository;

class FlightPriceServiceTest {

    private PriceCacheRepository cacheRepository;
    private PriceHistoryRepository<FlightPriceSnapshot, FlightPriceQuery> historyRepository;
    private FlightPriceService service;

    @SuppressWarnings({"unused", "unchecked"})
    @BeforeEach
    void setUp() {
        cacheRepository = mock(PriceCacheRepository.class);
        historyRepository = mock(PriceHistoryRepository.class);
        service = new FlightPriceService(cacheRepository, historyRepository);
    }

    @Test
    void savesFlightPriceToHistoryBeforeCache() {
        FlightPriceSnapshot snapshot = snapshot("2026-06-20T08:00:00Z", "715.50");

        assertThat(service.savePrice(snapshot)).isSameAs(snapshot);

        InOrder writeOrder = inOrder(cacheRepository, historyRepository);
        writeOrder.verify(historyRepository).save(snapshot);
        writeOrder.verify(cacheRepository).saveLatestFlightPrice(snapshot);
    }

    @Test
    void doesNotUpdateCacheWhenSavingFlightHistoryFails() {
        FlightPriceSnapshot snapshot = snapshot("2026-06-20T08:00:00Z", "715.50");
        RuntimeException failure = new RuntimeException("HBase unavailable");
        doThrow(failure).when(historyRepository).save(snapshot);

        assertThatThrownBy(() -> service.savePrice(snapshot)).isSameAs(failure);

        verify(cacheRepository, never()).saveLatestFlightPrice(snapshot);
    }

    @Test
    void returnsSuccessWhenFlightCacheUpdateFailsAfterHistoryIsSaved() {
        FlightPriceSnapshot snapshot = snapshot("2026-06-20T08:00:00Z", "715.50");
        doThrow(new RuntimeException("Redis unavailable"))
                .when(cacheRepository).saveLatestFlightPrice(snapshot);

        assertThat(service.savePrice(snapshot)).isSameAs(snapshot);

        verify(historyRepository).save(snapshot);
    }

    @Test
    void returnsCachedLatestPriceWithoutQueryingHistory() {
        FlightPriceQuery query = query();
        FlightPriceSnapshot cached = snapshot("2026-06-20T08:00:00Z", "715.50");
        when(cacheRepository.getLatestFlightPrice(query)).thenReturn(Optional.of(cached));

        assertThat(service.getLatestPrice(query)).containsSame(cached);

        verifyNoInteractions(historyRepository);
    }

    @Test
    void fallsBackToHistoryAndRefillsCacheOnMiss() {
        FlightPriceQuery query = query();
        FlightPriceSnapshot latest = snapshot("2026-06-20T09:00:00Z", "700.00");
        FlightPriceSnapshot older = snapshot("2026-06-20T08:00:00Z", "715.50");
        when(cacheRepository.getLatestFlightPrice(query)).thenReturn(Optional.empty());
        when(historyRepository.findHistory(query)).thenReturn(List.of(latest, older));

        assertThat(service.getLatestPrice(query)).containsSame(latest);

        verify(cacheRepository).saveLatestFlightPrice(latest);
    }

    @Test
    void returnsEmptyAndDoesNotRefillCacheWhenNoPriceExists() {
        FlightPriceQuery query = query();
        when(cacheRepository.getLatestFlightPrice(query)).thenReturn(Optional.empty());
        when(historyRepository.findHistory(query)).thenReturn(List.of());

        assertThat(service.getLatestPrice(query)).isEmpty();

        verify(cacheRepository, never()).saveLatestFlightPrice(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsHistoryFromPersistentRepository() {
        FlightPriceQuery query = query();
        List<FlightPriceSnapshot> history = List.of(snapshot("2026-06-20T08:00:00Z", "715.50"));
        when(historyRepository.findHistory(query)).thenReturn(history);

        assertThat(service.getPriceHistory(query)).isSameAs(history);
    }

    @Test
    void rejectsInvalidSnapshotBeforeWriting() {
        FlightPriceSnapshot invalid = new FlightPriceSnapshot(
                "LAX", "NRT", LocalDate.of(2026, 9, 1), null,
                "JL", BigDecimal.ZERO, "USD", "provider", Instant.now(), Map.of());

        assertThatThrownBy(() -> service.savePrice(invalid))
                .isInstanceOf(InvalidPriceRequestException.class)
                .hasMessage("Price must be greater than zero");

        verifyNoInteractions(cacheRepository, historyRepository);
    }

    private FlightPriceQuery query() {
        return new FlightPriceQuery("LAX", "NRT", LocalDate.of(2026, 9, 1), null);
    }

    private FlightPriceSnapshot snapshot(String capturedAt, String price) {
        return new FlightPriceSnapshot(
                "LAX",
                "NRT",
                LocalDate.of(2026, 9, 1),
                null,
                "JL",
                new BigDecimal(price),
                "USD",
                "provider",
                Instant.parse(capturedAt),
                Map.of());
    }
}
