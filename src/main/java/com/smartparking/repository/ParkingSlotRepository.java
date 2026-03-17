package com.smartparking.repository;

import com.smartparking.entity.ParkingSlot;
import com.smartparking.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {
    
    List<ParkingSlot> findByFloor(Integer floor);
    
    List<ParkingSlot> findByFloorAndStatus(Integer floor, SlotStatus status);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ParkingSlot ps WHERE ps.id = :slotId")
    Optional<ParkingSlot> findByIdWithLock(@Param("slotId") Long slotId);
    
    @Query("SELECT ps FROM ParkingSlot ps WHERE ps.status = :status AND (ps.lockUntil IS NULL OR ps.lockUntil < :now)")
    List<ParkingSlot> findAvailableSlots(@Param("status") SlotStatus status, @Param("now") LocalDateTime now);
    
    @Query("SELECT ps FROM ParkingSlot ps WHERE ps.lockUntil IS NOT NULL AND ps.lockUntil < :now")
    List<ParkingSlot> findExpiredLockedSlots(@Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(ps) FROM ParkingSlot ps WHERE ps.floor = :floor AND ps.status = :status")
    Long countByFloorAndStatus(@Param("floor") Integer floor, @Param("status") SlotStatus status);
    
    @Query("SELECT ps FROM ParkingSlot ps WHERE ps.slotNumber = :slotNumber AND ps.floor = :floor")
    Optional<ParkingSlot> findBySlotNumberAndFloor(@Param("slotNumber") Integer slotNumber, @Param("floor") Integer floor);
}
