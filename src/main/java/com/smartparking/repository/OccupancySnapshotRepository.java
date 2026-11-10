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

    OccupancySnapshot findTopByOrderByCapturedAtAsc();

    /** Average utilization percent and count over [from, to): [avgUtil, snapshots]. */
    @Query("""
                SELECT AVG(s.occupiedSlots * 100.0 / s.totalCapacity), COUNT(s)
                FROM OccupancySnapshot s
                WHERE s.capturedAt >= :from AND s.capturedAt < :to
            """)
    List<Object[]> averageUtilizationBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Per-floor average utilization over [from, to): [floor, avgUtil, avgOccupied, avgCapacity, snapshots]. */
    @Query("""
                SELECT s.floor,
                       AVG(s.occupiedSlots * 100.0 / s.totalCapacity),
                       AVG(s.occupiedSlots),
                       AVG(s.totalCapacity),
                       COUNT(s)
                FROM OccupancySnapshot s
                WHERE s.capturedAt >= :from AND s.capturedAt < :to
                GROUP BY s.floor
                ORDER BY s.floor
            """)
    List<Object[]> utilizationByFloorBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Highest-occupancy snapshots in range (peak periods). */
    List<OccupancySnapshot> findTop10ByCapturedAtBetweenOrderByOccupiedSlotsDescCapturedAtAsc(
            LocalDateTime from, LocalDateTime to);

    /**
     * PostgreSQL transaction-scoped advisory lock used to coordinate
     * snapshot collection across multiple application instances.
     * Returns true when the lock was acquired by this transaction.
     */
    @Query(value = "SELECT pg_try_advisory_xact_lock(:key)", nativeQuery = true)
    boolean tryAcquireSnapshotLock(@Param("key") long key);
}
