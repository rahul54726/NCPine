package com.rahul.cinebook.booking_service.controller;

import com.rahul.cinebook.booking_service.scheduler.ShowtimeSeatSyncScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final ShowtimeSeatSyncScheduler showtimeSeatSyncScheduler;

    @GetMapping("/sync-status")
    public ShowtimeSeatSyncScheduler.SyncStatus getSyncStatus() {
        return showtimeSeatSyncScheduler.getSyncStatus();
    }

    @PostMapping("/sync-now")
    public ShowtimeSeatSyncScheduler.SyncStatus runSyncNow() {
        showtimeSeatSyncScheduler.syncSeatsFromMovieCatalog();
        return showtimeSeatSyncScheduler.getSyncStatus();
    }
}
