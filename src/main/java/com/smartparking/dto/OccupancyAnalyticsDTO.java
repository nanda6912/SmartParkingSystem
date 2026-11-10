package com.smartparking.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OccupancyAnalyticsDTO(
        CurrentOccupancy current,
        LocalDateTime trackingStart,
        Long snapshotsInRange,
        Double averageUtilizationPercent,
        List<FloorUtilization> byFloor,
        List<OccupancyPoint> trend,
        List<PeakPeriod> peakPeriods) {

    public record CurrentOccupancy(long occupied, long available, long locked, long capacity,
            double utilizationPercent) {
    }

    public record FloorUtilization(Integer floor, double avgUtilizationPercent, double avgOccupiedSlots,
            double avgCapacity, long snapshots) {
    }

    public record OccupancyPoint(LocalDateTime capturedAt, Integer floor, long occupied, long available,
            long locked, long capacity, double utilizationPercent) {
    }

    public record PeakPeriod(LocalDateTime capturedAt, Integer floor, double utilizationPercent, long occupied,
            long capacity) {
    }
}
