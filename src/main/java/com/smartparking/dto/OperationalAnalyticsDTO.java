package com.smartparking.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record OperationalAnalyticsDTO(
        long totalBookings,
        long activeBookings,
        long completedBookings,
        Double avgDurationMinutes,
        double recordedRevenuePerCompletedSession,
        Map<String, Long> paymentMethodCounts,
        List<DailyCount> bookingTrends,
        Map<String, Long> statusDistribution,
        long lifecycleEventsInRange) {

    public record DailyCount(LocalDate date, long count) {
    }
}
