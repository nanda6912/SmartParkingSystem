package com.smartparking.config;

import com.smartparking.service.OccupancySnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Captures the first occupancy snapshot as soon as the application
 * context is ready, without waiting for the first scheduled interval.
 * Dedupe via the (floor, captured_at) bucket makes this safe even when
 * the application restarts within the same interval.
 */
@Component
public class OccupancySnapshotInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OccupancySnapshotInitializer.class);

    @Autowired
    private OccupancySnapshotService occupancySnapshotService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Capturing initial occupancy snapshot on startup");
        occupancySnapshotService.captureSafely();
    }
}
