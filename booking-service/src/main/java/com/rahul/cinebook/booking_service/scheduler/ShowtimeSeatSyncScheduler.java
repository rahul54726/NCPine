package com.rahul.cinebook.booking_service.scheduler;

import com.rahul.cinebook.booking_service.entity.Seat;
import com.rahul.cinebook.booking_service.enums.SeatStatus;
import com.rahul.cinebook.booking_service.repository.SeatRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class ShowtimeSeatSyncScheduler {

    private final SeatRepo seatRepo;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${movies.service.url:http://movies-service:8085}")
    private String moviesServiceUrl;
    private volatile Instant lastRunAt;
    private volatile Instant lastSuccessAt;
    private volatile String lastError;
    private final AtomicInteger lastCatalogShowCount = new AtomicInteger(0);
    private final AtomicInteger lastSyncedShowCount = new AtomicInteger(0);
    private final AtomicInteger totalSyncedShowCount = new AtomicInteger(0);

    @Scheduled(
            initialDelayString = "${catalog.sync.initial-delay-ms:15000}",
            fixedDelayString = "${catalog.sync.fixed-delay-ms:30000}"
    )
    public void syncSeatsFromMovieCatalog() {
        lastRunAt = Instant.now();
        try {
            CatalogMovieEntry[] catalog = restTemplate.getForObject(
                    moviesServiceUrl + "/catalog/movies",
                    CatalogMovieEntry[].class
            );

            if (catalog == null || catalog.length == 0) {
                lastCatalogShowCount.set(0);
                lastSyncedShowCount.set(0);
                lastSuccessAt = Instant.now();
                lastError = null;
                return;
            }

            Set<String> showIds = new HashSet<>();
            for (CatalogMovieEntry entry : catalog) {
                if (entry == null || entry.showtimeId == null || entry.showtimeId.isBlank()) {
                    continue;
                }
                showIds.add(entry.showtimeId);
            }

            if (showIds.isEmpty()) {
                lastCatalogShowCount.set(0);
                lastSyncedShowCount.set(0);
                lastSuccessAt = Instant.now();
                lastError = null;
                return;
            }

            int syncedShows = 0;
            for (String showId : showIds) {
                if (!seatRepo.findByShowTimeId(showId).isEmpty()) {
                    continue;
                }

                List<Seat> seats = buildDefaultSeats(showId);
                seatRepo.saveAll(seats);
                syncedShows++;
            }

            if (syncedShows > 0) {
                log.info("Synced seats for {} new showtimes from movies catalog", syncedShows);
            }
            lastCatalogShowCount.set(showIds.size());
            lastSyncedShowCount.set(syncedShows);
            totalSyncedShowCount.addAndGet(syncedShows);
            lastSuccessAt = Instant.now();
            lastError = null;
        } catch (Exception ex) {
            lastError = ex.getMessage();
            log.warn("Showtime-seat sync skipped this cycle: {}", ex.getMessage());
        }
    }

    public SyncStatus getSyncStatus() {
        return new SyncStatus(
                lastRunAt,
                lastSuccessAt,
                lastCatalogShowCount.get(),
                lastSyncedShowCount.get(),
                totalSyncedShowCount.get(),
                lastError
        );
    }

    private List<Seat> buildDefaultSeats(String showId) {
        List<Seat> seats = new ArrayList<>();
        for (String row : List.of("A", "B", "C", "D")) {
            for (int i = 1; i <= 10; i++) {
                Seat seat = new Seat();
                seat.setShowTimeId(showId);
                seat.setSeatNumber(row + i);
                seat.setPrice(250.0);
                seat.setStatus(SeatStatus.AVAILABLE);
                seats.add(seat);
            }
        }
        return seats;
    }

    static class CatalogMovieEntry {
        public String id;
        public String showtimeId;
    }

    public record SyncStatus(
            Instant lastRunAt,
            Instant lastSuccessAt,
            int lastCatalogShowCount,
            int lastSyncedShowCount,
            int totalSyncedShowCount,
            String lastError
    ) {}
}
