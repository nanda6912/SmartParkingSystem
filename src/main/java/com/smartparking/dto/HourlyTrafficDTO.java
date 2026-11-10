package com.smartparking.dto;

import java.util.List;

public record HourlyTrafficDTO(
        List<Integer> entriesByHour,
        List<Integer> exitsByHour,
        List<Double> revenueByHour,
        List<PeakHour> peakHours,
        String timezone) {

    public record PeakHour(int hour, long entries, long exits, double revenue) {
    }
}
