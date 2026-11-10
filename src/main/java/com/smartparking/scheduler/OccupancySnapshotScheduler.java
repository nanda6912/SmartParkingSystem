package com.smartparking.scheduler;

import com.smartparking.service.OccupancySnapshotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically captures parking occupancy snapshots. The interval is
 * configurable via parking.analytics.snapshot-interval-ms (default
 * 900000 = 15 minutes). Failures are logged inside the service and
 * never interrupt the schedule.
 */
@Component
public class OccupancySnapshotScheduler {

    private static final Logger log = LoggerFactory.getLogger(OccupancySnapshotScheduler.class);

    @Autowired
    private OccupancySnapshotService occupancySnapshotService;

    @Scheduled(
            fixedDelayString = "${parking.analytics.snapshot-interval-ms:900000}",
            initialDelayString = "${parking.analytics.snapshot-initial-delay-ms:60000}")
    public void captureScheduledSnapshot() {
        log.debug("Scheduled occupancy snapshot triggered");
        occupancySnapshotService.captureSafely();
    }
}
