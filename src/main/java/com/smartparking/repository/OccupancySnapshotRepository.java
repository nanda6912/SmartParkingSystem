package com.smartparking.repository;

import com.smartparking.entity.OccupancySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OccupancySnapshotRepository extends JpaRepository<OccupancySnapshot, Long> {

    boolean existsByFloorAndCapturedAt(Integer floor, LocalDateTime capturedAt);

    List<OccupancySnapshot> findByCapturedAtBetweenOrderByCapturedAtAscFloorAsc(
            LocalDateTime from, LocalDateTime to);

    List<OccupancySnapshot> findByFloorAndCapturedAtBetweenOrderByCapturedAtAsc(
            Integer floor, LocalDateTime from, LocalDateTime to);

    OccupancySnapshot findTopByOrderByCapturedAtDesc();

    /**
     * PostgreSQL transaction-scoped advisory lock used to coordinate
     * snapshot collection across multiple application instances.
     * Returns true when the lock was acquired by this transaction.
     */
    @Query(value = "SELECT pg_try_advisory_xact_lock(:key)", nativeQuery = true)
    boolean tryAcquireSnapshotLock(@Param("key") long key);
}
