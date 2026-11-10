package com.smartparking.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Snapshot collection health summary computed from real
 * occupancy_snapshots rows and the configured
 * parking.analytics.* properties. No timestamps or counts are
 * fabricated: every field is derived from stored data or config.
 *
 * @param collectionEnabled     parking.analytics.snapshot-enabled
 * @param snapshotIntervalMs    parking.analytics.snapshot-interval-ms
 * @param zoneId                parking.analytics.zone-id
 * @param evaluatedAt           evaluation instant in the reporting zone
 * @param earliestSnapshotAt    oldest captured_at across all floors (null when none)
 * @param latestSnapshotAt      newest captured_at across all floors (null when none)
 * @param trackedFloorCount     distinct floors present in the table
 * @param snapshotsLast24h      snapshot rows captured in the last 24h
 *                              (per-floor rows, not deduplicated buckets)
 * @param overdue               latest snapshot is older than one full
 *                              expected interval behind the current bucket
 * @param status                ACTIVE | DELAYED | NO_SNAPSHOTS | DISABLED
 * @param expectedIntervals     expected (floor, bucket) pairs between each
 *                              floor's first and last observed snapshot
 * @param observedIntervals     observed (floor, bucket) pairs on the grid
 * @param missingIntervalCount  expected pairs with no snapshot row
 * @param missingIntervals      distinct bucket timestamps with at least one
 *                              missing floor, capped at 24, oldest first
 */
public record OccupancyHealthDTO(
        boolean collectionEnabled,
        long snapshotIntervalMs,
        String zoneId,
        LocalDateTime evaluatedAt,
        LocalDateTime earliestSnapshotAt,
        LocalDateTime latestSnapshotAt,
        long trackedFloorCount,
        long snapshotsLast24h,
        boolean overdue,
        String status,
        long expectedIntervals,
        long observedIntervals,
        long missingIntervalCount,
        List<LocalDateTime> missingIntervals) {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DELAYED = "DELAYED";
    public static final String STATUS_NO_SNAPSHOTS = "NO_SNAPSHOTS";
    public static final String STATUS_DISABLED = "DISABLED";
}
