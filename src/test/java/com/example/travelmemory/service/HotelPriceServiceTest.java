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
import com.example.travelmemory.model.HotelPriceQuery;
import com.example.travelmemory.model.HotelPriceSnapshot;
import com.example.travelmemory.persistence.PriceHistoryRepository;

class HotelPriceServiceTest {

    private PriceCacheRepository cacheRepository;
    private PriceHistoryRepository<HotelPriceSnapshot, HotelPriceQuery> historyRepository;
    private HotelPriceService service;

    @SuppressWarnings({"unused", "unchecked"})
    @BeforeEach
    void setUp() {
        cacheRepository = mock(PriceCacheRepository.class);
        historyRepository = mock(PriceHistoryRepository.class);
        service = new HotelPriceService(cacheRepository, historyRepository);
    }

    @Test
    void savesHotelPriceToHistoryBeforeCache() {
        HotelPriceSnapshot snapshot = snapshot("2026-06-20T08:00:00Z", "220.00");

        assertThat(service.savePrice(snapshot)).isSameAs(snapshot);

        InOrder writeOrder = inOrder(cacheRepository, historyRepository);
        writeOrder.verify(historyRepository).save(snapshot);
        writeOrder.verify(cacheRepository).saveLatestHotelPrice(snapshot);
    }

    @Test
    void doesNotUpdateCacheWhenSavingHotelHistoryFails() {
        HotelPriceSnapshot snapshot = snapshot("2026-06-20T08:00:00Z", "220.00");
        RuntimeException failure = new RuntimeException("HBase unavailable");
        doThrow(failure).when(historyRepository).save(snapshot);

        assertThatThrownBy(() -> service.savePrice(snapshot)).isSameAs(failure);

        verify(cacheRepository, never()).saveLatestHotelPrice(snapshot);
    }

    @Test
    void returnsSuccessWhenHotelCacheUpdateFailsAfterHistoryIsSaved() {
        HotelPriceSnapshot snapshot = snapshot("2026-06-20T08:00:00Z", "220.00");
        doThrow(new RuntimeException("Redis unavailable"))
                .when(cacheRepository).saveLatestHotelPrice(snapshot);

        assertThat(service.savePrice(snapshot)).isSameAs(snapshot);

        verify(historyRepository).save(snapshot);
    }

    @Test
    void returnsCachedLatestPriceWithoutQueryingHistory() {
        HotelPriceQuery query = query();
        HotelPriceSnapshot cached = snapshot("2026-06-20T08:00:00Z", "220.00");
        when(cacheRepository.getLatestHotelPrice(query)).thenReturn(Optional.of(cached));

        assertThat(service.getLatestPrice(query)).containsSame(cached);

        verifyNoInteractions(historyRepository);
    }

    @Test
    void fallsBackToHistoryAndRefillsCacheOnMiss() {
        HotelPriceQuery query = query();
        HotelPriceSnapshot latest = snapshot("2026-06-20T09:00:00Z", "205.00");
        HotelPriceSnapshot older = snapshot("2026-06-20T08:00:00Z", "220.00");
        when(cacheRepository.getLatestHotelPrice(query)).thenReturn(Optional.empty());
        when(historyRepository.findHistory(query)).thenReturn(List.of(latest, older));

        assertThat(service.getLatestPrice(query)).containsSame(latest);

        verify(cacheRepository).saveLatestHotelPrice(latest);
    }

    @Test
    void returnsEmptyAndDoesNotRefillCacheWhenNoPriceExists() {
        HotelPriceQuery query = query();
        when(cacheRepository.getLatestHotelPrice(query)).thenReturn(Optional.empty());
        when(historyRepository.findHistory(query)).thenReturn(List.of());

        assertThat(service.getLatestPrice(query)).isEmpty();

        verify(cacheRepository, never()).saveLatestHotelPrice(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsHistoryFromPersistentRepository() {
        HotelPriceQuery query = query();
        List<HotelPriceSnapshot> history = List.of(snapshot("2026-06-20T08:00:00Z", "220.00"));
        when(historyRepository.findHistory(query)).thenReturn(history);

        assertThat(service.getPriceHistory(query)).isSameAs(history);
    }

    @Test
    void rejectsInvalidQueryBeforeReading() {
        HotelPriceQuery invalid = new HotelPriceQuery(
                "Tokyo", "Park Hotel", LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 5));

        assertThatThrownBy(() -> service.getLatestPrice(invalid))
                .isInstanceOf(InvalidPriceRequestException.class)
                .hasMessage("Check-out date must be after check-in date");

        verifyNoInteractions(cacheRepository, historyRepository);
    }

    private HotelPriceQuery query() {
        return new HotelPriceQuery(
                "Tokyo", "Park Hotel", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5));
    }

    private HotelPriceSnapshot snapshot(String capturedAt, String price) {
        return new HotelPriceSnapshot(
                "Tokyo",
                "Park Hotel",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 5),
                "King",
                new BigDecimal(price),
                "USD",
                "provider",
                Instant.parse(capturedAt),
                Map.of());
    }
}
