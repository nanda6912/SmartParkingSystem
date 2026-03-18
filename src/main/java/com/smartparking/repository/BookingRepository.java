package com.smartparking.repository;

import com.smartparking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
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
    
    @Query("SELECT b FROM Booking b WHERE b.isActive = true AND b.exitTime IS NULL")
    List<Booking> findActiveBookingsWithoutExit();
    
    @Query("SELECT b FROM Booking b WHERE b.isActive = true AND b.parkingSlot.floor = :floor")
    List<Booking> findActiveBookingsByFloor(@Param("floor") Integer floor);
    
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingTime BETWEEN :startTime AND :endTime AND b.isActive = false")
    Long countCompletedBookingsInPeriod(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT SUM(b.parkingFee) FROM Booking b WHERE b.bookingTime BETWEEN :startTime AND :endTime AND b.parkingFee IS NOT NULL")
    Long calculateRevenueInPeriod(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT b FROM Booking b WHERE b.bookingTime >= :date ORDER BY b.bookingTime DESC")
    List<Booking> findBookingsFromDate(@Param("date") LocalDateTime date);
    
    @Query("SELECT b FROM Booking b WHERE b.bookingTime >= :date AND b.exitTime IS NOT NULL")
    List<Booking> findExitedBookingsFromDate(@Param("date") LocalDateTime date);
    
    /**
     * Find active booking by vehicle number
     */
    Optional<Booking> findByVehicleNumberAndIsActiveTrue(String vehicleNumber);
}
