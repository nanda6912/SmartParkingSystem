package com.smartparking.dto;

import java.time.LocalDate;
import java.util.List;

public record RevenueAnalyticsDTO(
        String metricLabel,
        double totalRecordedRevenue,
        double avgRevenuePerSession,
        RevenuePoint highestRevenueDay,
        double previousPeriodRevenue,
        List<RevenuePoint> daily,
        List<RevenuePoint> weekly,
        List<RevenuePoint> monthly,
        List<MethodRevenue> byPaymentMethod,
        List<FloorRevenue> byFloor) {

    public record RevenuePoint(LocalDate period, double revenue, long sessions) {
    }

    public record MethodRevenue(String paymentMethod, double revenue, long sessions) {
    }

    public record FloorRevenue(Integer floor, double revenue, long sessions) {
    }
}
