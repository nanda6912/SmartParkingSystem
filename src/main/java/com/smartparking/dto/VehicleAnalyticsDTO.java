package com.smartparking.dto;

import java.time.LocalDate;
import java.util.List;

public record VehicleAnalyticsDTO(
        List<VehicleTypeStats> byType,
        List<VehicleFloorStats> byFloor,
        List<DailyCount> entryTrends) {

    public record VehicleTypeStats(String vehicleType, long sessions, double percentage,
            Double avgDurationMinutes, Double avgRevenue) {
    }

    public record VehicleFloorStats(Integer floor, String vehicleType, long sessions) {
    }

    public record DailyCount(LocalDate date, long count) {
    }
}
