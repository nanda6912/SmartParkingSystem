package com.smartparking.service;

import com.smartparking.entity.OccupancySnapshot;
import com.smartparking.enums.SlotStatus;
import com.smartparking.repository.OccupancySnapshotRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * Collects permanent historical occupancy snapshots from live
 * parking_slots statuses. Snapshots are quantized to a fixed
 * interval bucket so restarts and multiple application instances
 * cannot create duplicates for the same capture interval.
 */
@Service
public class OccupancySnapshotService {

    private static final Logger log = LoggerFactory.getLogger(OccupancySnapshotService.class);

    // Arbitrary stable key for the PostgreSQL advisory lock coordinating
    // snapshot collection across instances.
    private static final long SNAPSHOT_ADVISORY_LOCK_KEY = 0x53504F54504C4E46L; // "SPOTPLNF"

    @Value("${parking.analytics.snapshot-enabled:true}")
    private boolean snapshotEnabled;

    @Value("${parking.analytics.snapshot-interval-ms:900000}")
    private long snapshotIntervalMs;

    @Value("${parking.analytics.zone-id:Asia/Kolkata}")
    private String zoneIdProperty;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private OccupancySnapshotRepository occupancySnapshotRepository;

    /**
     * Scheduler/runner entry point: collects a snapshot and logs
     * failures without propagating them.
     */
    public void captureSafely() {
        if (!snapshotEnabled) {
            log.debug("Occupancy snapshot collection is disabled");
            return;
        }
        try {
            int written = captureOccupancySnapshot();
            log.info("Occupancy snapshot collection finished, {} floor snapshot(s) written", written);
        } catch (Exception e) {
            log.error("Occupancy snapshot collection failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Collects one snapshot row per floor for the current interval bucket.
     * Uses a transaction-scoped advisory lock so only one application
     * instance performs collection per interval; the unique
     * (floor, captured_at) constraint provides a second dedupe layer.
     *
     * @return number of snapshot rows written
     */
    @Transactional
    public int captureOccupancySnapshot() {
        if (!tryAcquireCollectionLock()) {
            log.debug("Another instance holds the occupancy snapshot lock; skipping collection");
            return 0;
        }

        ZoneId zone = ZoneId.of(zoneIdProperty);
        LocalDateTime bucket = currentBucket(zone);

        Map<Integer, FloorCounts> countsByFloor = aggregateSlotCounts();
        if (countsByFloor.isEmpty()) {
            log.info("No parking slots found; skipping occupancy snapshot for bucket {}", bucket);
            return 0;
        }

        int written = 0;
        for (Map.Entry<Integer, FloorCounts> entry : countsByFloor.entrySet()) {
            Integer floor = entry.getKey();
            FloorCounts counts = entry.getValue();

            if (occupancySnapshotRepository.existsByFloorAndCapturedAt(floor, bucket)) {
                log.debug("Snapshot already exists for floor {} at {}", floor, bucket);
                continue;
            }

            try {
                occupancySnapshotRepository.save(new OccupancySnapshot(
                        bucket,
                        floor,
                        counts.total(),
                        counts.occupied(),
                        counts.available(),
                        counts.locked()));
                written++;
            } catch (DataIntegrityViolationException e) {
                // Concurrent collector won the race for this bucket; dedupe is safe
                log.debug("Duplicate occupancy snapshot skipped for floor {} at {}", floor, bucket);
            }
        }
        return written;
    }

    /**
     * Returns the start of the current snapshot interval bucket in the
     * configured reporting zone, quantized on the epoch timeline so the
     * same bucket is produced regardless of restarts or instance count.
     */
    LocalDateTime currentBucket(ZoneId zone) {
        Duration interval = Duration.ofMillis(Math.max(1000L, snapshotIntervalMs));
        long intervalSeconds = Math.max(1L, interval.getSeconds());
        ZonedDateTime now = ZonedDateTime.now(zone);
        long bucketEpochSecond = (now.toEpochSecond() / intervalSeconds) * intervalSeconds;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(bucketEpochSecond), zone);
    }

    private Map<Integer, FloorCounts> aggregateSlotCounts() {
        Map<Integer, FloorCounts> countsByFloor = new TreeMap<>();
        for (Object[] row : parkingSlotRepository.countByFloorAndStatus()) {
            Integer floor = (Integer) row[0];
            SlotStatus status = (SlotStatus) row[1];
            long count = ((Number) row[2]).longValue();
            FloorCounts counts = countsByFloor.computeIfAbsent(floor, f -> new FloorCounts());
            counts.add(status, count);
        }
        return countsByFloor;
    }

    private boolean tryAcquireCollectionLock() {
        try {
            return occupancySnapshotRepository.tryAcquireSnapshotLock(SNAPSHOT_ADVISORY_LOCK_KEY);
        } catch (Exception e) {
            // Non-PostgreSQL environments (e.g. tests) may not support advisory
            // locks; proceed without coordination since the unique constraint
            // still prevents duplicate rows.
            log.warn("Occupancy snapshot advisory lock unavailable ({}); proceeding without it",
                    e.getMessage());
            return true;
        }
    }

    private static final class FloorCounts {
        private long occupied;
        private long available;
        private long locked;

        void add(SlotStatus status, long count) {
            switch (status) {
                case OCCUPIED -> occupied += count;
                case AVAILABLE -> available += count;
                case LOCKED -> locked += count;
            }
        }

        int total() {
            return (int) (occupied + available + locked);
        }

        int occupied() {
            return (int) occupied;
        }

        int available() {
            return (int) available;
        }

        int locked() {
            return (int) locked;
        }
    }
}
