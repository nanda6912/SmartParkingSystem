package com.smartparking.service;

import com.smartparking.entity.OccupancySnapshot;
import com.smartparking.repository.OccupancySnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;

/**
 * Persists individual occupancy snapshot rows in their own transaction.
 * A duplicate-key or failed insert for one floor can then be skipped
 * without rolling back sibling floors or poisoning the collector's
 * transaction (on PostgreSQL a constraint violation aborts the whole
 * transaction, so catching it inside a shared transaction cannot work).
 */
@Component
public class OccupancySnapshotWriter {

    private static final Logger log = LoggerFactory.getLogger(OccupancySnapshotWriter.class);

    @Autowired
    private OccupancySnapshotRepository occupancySnapshotRepository;

    /**
     * Inserts the snapshot unless a row already exists for the same
     * (floor, captured_at) bucket.
     *
     * @return true when a new row was written; false only for genuine
     *         duplicate-bucket conflicts
     * @throws DataIntegrityViolationException for unrelated integrity
     *         failures, which are logged with context and propagated so
     *         the failure is not silently swallowed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean persistIfAbsent(OccupancySnapshot snapshot) {
        if (occupancySnapshotRepository.existsByFloorAndCapturedAt(
                snapshot.getFloor(), snapshot.getCapturedAt())) {
            return false;
        }
        try {
            occupancySnapshotRepository.saveAndFlush(snapshot);
            return true;
        } catch (DataIntegrityViolationException e) {
            if (isUniqueViolation(e)) {
                // Concurrent collector won the race for this bucket; dedupe is safe
                log.debug("Duplicate occupancy snapshot skipped for floor {} at {}",
                        snapshot.getFloor(), snapshot.getCapturedAt());
                return false;
            }
            log.error("Unexpected integrity failure persisting occupancy snapshot "
                    + "for floor {} at {}: {}", snapshot.getFloor(),
                    snapshot.getCapturedAt(), e.getMessage());
            throw e;
        }
    }

    /**
     * True only for genuine unique-key conflicts: PostgreSQL SQLState
     * 23505, or a driver message naming the bucket uniqueness constraint.
     */
    private static boolean isUniqueViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getMostSpecificCause();
        if (cause instanceof SQLException sqe && "23505".equals(sqe.getSQLState())) {
            return true;
        }
        String message = String.valueOf(cause != null ? cause.getMessage() : e.getMessage());
        return message.contains("uq_occupancy_snapshot_floor_time")
                || message.contains("duplicate key");
    }
}
