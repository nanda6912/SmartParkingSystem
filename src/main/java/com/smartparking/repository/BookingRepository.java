package com.smartparking.repository;

import com.smartparking.entity.Booking;
import com.smartparking.enums.BookingStatus;
import com.smartparking.enums.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByVehicleNumber(String vehicleNumber);

    Optional<Booking> findByBookingCode(String bookingCode);

    List<Booking> findByIsActiveTrue();

    List<Booking> findByIsActiveFalse();

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.parkingSlot WHERE b.isActive = true AND b.exitTime IS NULL")
    List<Booking> findActiveBookingsWithoutExit();

    @Query("SELECT b FROM Booking b WHERE b.isActive = true AND b.parkingSlot.floor = :floor")
    List<Booking> findActiveBookingsByFloor(@Param("floor") Integer floor);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.parkingSlot WHERE b.vehicleNumber = :vehicleNumber AND b.isActive = true AND b.exitTime IS NULL")
    Optional<Booking> findActiveBookingByVehicleNumber(@Param("vehicleNumber") String vehicleNumber);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingTime BETWEEN :startTime AND :endTime AND b.isActive = false")
    Long countCompletedBookingsInPeriod(@Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT SUM(b.parkingFee) FROM Booking b WHERE b.bookingTime BETWEEN :startTime AND :endTime AND b.parkingFee IS NOT NULL")
    Long calculateRevenueInPeriod(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.bookingTime >= :date ORDER BY b.bookingTime DESC")
    List<Booking> findBookingsFromDate(@Param("date") LocalDateTime date);

    @Query("SELECT b FROM Booking b LEFT JOIN FETCH b.parkingSlot WHERE b.exitTime >= :date")
    List<Booking> findExitedBookingsFromDate(@Param("date") LocalDateTime date);

    /**
     * Find active booking by vehicle number
     */
    @EntityGraph(attributePaths = {"parkingSlot"})
    Optional<Booking> findByVehicleNumberAndIsActiveTrue(String vehicleNumber);

    /**
     * Count active bookings directly in the database
     */
    long countByIsActiveTrue();

    /**
     * Find booking by ID with parking slot eagerly loaded
     */
    @Query("""
                SELECT b
                FROM Booking b
                LEFT JOIN FETCH b.parkingSlot
                WHERE b.id = :id
            """)
    Optional<Booking> findByIdWithParkingSlot(@Param("id") Long id);

    /**
     * Find booking by code with parking slot eagerly loaded
     */
    @Query("""
                SELECT b
                FROM Booking b
                LEFT JOIN FETCH b.parkingSlot
                WHERE b.bookingCode = :bookingCode
            """)
    Optional<Booking> findByBookingCodeWithParkingSlot(@Param("bookingCode") String bookingCode);

    /**
     * Paged booking history search with optional filters.
     * {@code to} is exclusive (pass start of the day after the range end).
     */
    @Query("""
                SELECT b
                FROM Booking b
                LEFT JOIN FETCH b.parkingSlot
                WHERE (:from IS NULL OR b.bookingTime >= :from)
                  AND (:to IS NULL OR b.bookingTime < :to)
                  AND (:status IS NULL OR b.status = :status)
                  AND (:vehicleType IS NULL OR b.vehicleType = :vehicleType)
                  AND (:paymentMethod IS NULL OR b.paymentMethod = :paymentMethod)
                  AND (:isActive IS NULL OR b.isActive = :isActive)
                ORDER BY b.bookingTime DESC, b.id DESC
            """)
    Page<Booking> searchBookingHistory(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") BookingStatus status,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("paymentMethod") String paymentMethod,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // ==================== ANALYTICS QUERIES ====================

    /**
     * Completed-session aggregates for bookings whose exit_time falls in
     * [from, to): count, recorded fee sum, average duration (minutes).
     */
    @Query("""
                SELECT COUNT(b), COALESCE(SUM(b.parkingFee), 0), COALESCE(AVG(b.durationMinutes), 0)
                FROM Booking b
                WHERE b.exitTime IS NOT NULL AND b.exitTime >= :from AND b.exitTime < :to
            """)
    List<Object[]> completedBookingStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Recorded-revenue aggregates keyed on payment_time (only bookings with
     * a recorded payment method and fee). Returns
     * [day, paymentMethod, revenue, sessions].
     */
    @Query(value = """
                SELECT DATE(payment_time) AS d, payment_method,
                       COALESCE(SUM(parking_fee), 0) AS revenue, COUNT(*) AS sessions
                FROM bookings
                WHERE payment_time IS NOT NULL
                  AND payment_time >= :from AND payment_time < :to
                  AND payment_method IS NOT NULL
                  AND parking_fee IS NOT NULL
                GROUP BY DATE(payment_time), payment_method
                ORDER BY d
            """, nativeQuery = true)
    List<Object[]> recordedRevenueByDayAndMethod(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Recorded revenue by floor: [floor, revenue, sessions]. */
    @Query(value = """
                SELECT ps.floor, COALESCE(SUM(b.parking_fee), 0), COUNT(*)
                FROM bookings b
                JOIN parking_slots ps ON b.parking_slot_id = ps.id
                WHERE b.payment_time IS NOT NULL
                  AND b.payment_time >= :from AND b.payment_time < :to
                  AND b.payment_method IS NOT NULL
                  AND b.parking_fee IS NOT NULL
                GROUP BY ps.floor
                ORDER BY ps.floor
            """, nativeQuery = true)
    List<Object[]> recordedRevenueByFloor(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Hourly entry counts: [hourOfDay, count] by booking_time. */
    @Query(value = """
                SELECT EXTRACT(HOUR FROM booking_time)::int AS h, COUNT(*)
                FROM bookings
                WHERE booking_time >= :from AND booking_time < :to
                GROUP BY h ORDER BY h
            """, nativeQuery = true)
    List<Object[]> entriesByHour(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Hourly exit counts: [hourOfDay, count] by exit_time. */
    @Query(value = """
                SELECT EXTRACT(HOUR FROM exit_time)::int AS h, COUNT(*)
                FROM bookings
                WHERE exit_time IS NOT NULL
                  AND exit_time >= :from AND exit_time < :to
                GROUP BY h ORDER BY h
            """, nativeQuery = true)
    List<Object[]> exitsByHour(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Hourly recorded revenue: [hourOfDay, revenue] by payment_time. */
    @Query(value = """
                SELECT EXTRACT(HOUR FROM payment_time)::int AS h, COALESCE(SUM(parking_fee), 0)
                FROM bookings
                WHERE payment_time IS NOT NULL
                  AND payment_time >= :from AND payment_time < :to
                  AND payment_method IS NOT NULL
                  AND parking_fee IS NOT NULL
                GROUP BY h ORDER BY h
            """, nativeQuery = true)
    List<Object[]> revenueByHour(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Vehicle-type stats for completed sessions: [type, sessions, avgDurationMin, avgFee]. */
    @Query("""
                SELECT b.vehicleType, COUNT(b), AVG(b.durationMinutes), AVG(b.parkingFee)
                FROM Booking b
                WHERE b.exitTime IS NOT NULL AND b.exitTime >= :from AND b.exitTime < :to
                GROUP BY b.vehicleType
            """)
    List<Object[]> vehicleTypeStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Vehicle-type x floor sessions: [floor, type, sessions]. */
    @Query("""
                SELECT ps.floor, b.vehicleType, COUNT(b)
                FROM Booking b
                JOIN b.parkingSlot ps
                WHERE b.exitTime IS NOT NULL AND b.exitTime >= :from AND b.exitTime < :to
                GROUP BY ps.floor, b.vehicleType
            """)
    List<Object[]> vehicleTypeByFloor(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Daily entry trend: [date, entries]. */
    @Query(value = """
                SELECT DATE(booking_time) AS d, COUNT(*)
                FROM bookings
                WHERE booking_time >= :from AND booking_time < :to
                GROUP BY d ORDER BY d
            """, nativeQuery = true)
    List<Object[]> dailyEntryTrend(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Status distribution for bookings created in range: [status, count]. */
    @Query("""
                SELECT b.status, COUNT(b)
                FROM Booking b
                WHERE b.bookingTime >= :from AND b.bookingTime < :to
                GROUP BY b.status
            """)
    List<Object[]> statusDistribution(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Recorded payment counts by method: [method, count]. */
    @Query("""
                SELECT b.paymentMethod, COUNT(b)
                FROM Booking b
                WHERE b.paymentTime IS NOT NULL AND b.paymentTime >= :from AND b.paymentTime < :to
                  AND b.paymentMethod IS NOT NULL
                GROUP BY b.paymentMethod
            """)
    List<Object[]> paymentMethodCounts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Total bookings created in [from, to): count, active count. */
    @Query("""
                SELECT COUNT(b),
                       SUM(CASE WHEN b.isActive = true AND b.exitTime IS NULL THEN 1 ELSE 0 END)
                FROM Booking b
                WHERE b.bookingTime >= :from AND b.bookingTime < :to
            """)
    List<Object[]> bookingActivityStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
