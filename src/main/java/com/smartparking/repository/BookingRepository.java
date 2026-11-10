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
}
