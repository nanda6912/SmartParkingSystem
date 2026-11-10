package com.smartparking.dto;

public record AnalyticsOverviewDTO(
        long totalCompletedSessions,
        double recordedRevenue,
        double avgRevenuePerSession,
        Double avgDurationMinutes,
        long currentOccupiedSlots,
        long currentAvailableSlots,
        long currentLockedSlots,
        long totalCapacity,
        double occupancyPercent,
        Long previousPeriodCompletedSessions,
        Double previousPeriodRevenue,
        String note) {
}
