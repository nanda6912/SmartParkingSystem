package com.smartparking.service;

import com.smartparking.entity.OccupancySnapshot;
import com.smartparking.repository.OccupancySnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists individual occupancy snapshot rows in their own transaction.
 * A duplicate-key or failed insert for one floor can then be skipped
 * without rolling back sibling floors or poisoning the collector's
 * transaction (on PostgreSQL a constraint violation aborts the whole
 * transaction, so catching it inside a shared transaction cannot work).
 */
@Component
public class OccupancySnapshotWriter {

    @Autowired
    private OccupancySnapshotRepository occupancySnapshotRepository;

    /**
     * Inserts the snapshot unless a row already exists for the same
     * (floor, captured_at) bucket.
     *
     * @return true when a new row was written
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
            // Concurrent collector won the race for this bucket; dedupe is safe
            return false;
        }
    }
}
