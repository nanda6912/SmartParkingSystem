package com.smartparking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "occupancy_snapshots")
public class OccupancySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @NotNull
    @Column(name = "floor", nullable = false)
    private Integer floor;

    @NotNull
    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity;

    @NotNull
    @Column(name = "occupied_slots", nullable = false)
    private Integer occupiedSlots;

    @NotNull
    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @NotNull
    @Column(name = "locked_slots", nullable = false)
    private Integer lockedSlots;

    public OccupancySnapshot() {
    }

    public OccupancySnapshot(LocalDateTime capturedAt, Integer floor, Integer totalCapacity,
            Integer occupiedSlots, Integer availableSlots, Integer lockedSlots) {
        this.capturedAt = capturedAt;
        this.floor = floor;
        this.totalCapacity = totalCapacity;
        this.occupiedSlots = occupiedSlots;
        this.availableSlots = availableSlots;
        this.lockedSlots = lockedSlots;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public void setCapturedAt(LocalDateTime capturedAt) {
        this.capturedAt = capturedAt;
    }

    public Integer getFloor() {
        return floor;
    }

    public void setFloor(Integer floor) {
        this.floor = floor;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Integer getOccupiedSlots() {
        return occupiedSlots;
    }

    public void setOccupiedSlots(Integer occupiedSlots) {
        this.occupiedSlots = occupiedSlots;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Integer getLockedSlots() {
        return lockedSlots;
    }

    public void setLockedSlots(Integer lockedSlots) {
        this.lockedSlots = lockedSlots;
    }
}
