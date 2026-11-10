package com.smartparking.service;

import com.smartparking.dto.AnalyticsOverviewDTO;
import com.smartparking.dto.HourlyTrafficDTO;
import com.smartparking.dto.OccupancyAnalyticsDTO;
import com.smartparking.dto.OperationalAnalyticsDTO;
import com.smartparking.dto.RevenueAnalyticsDTO;
import com.smartparking.dto.VehicleAnalyticsDTO;
import com.smartparking.entity.OccupancySnapshot;
import com.smartparking.enums.SlotStatus;
import com.smartparking.repository.BookingEventRepository;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.OccupancySnapshotRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only analytics aggregation over permanent booking records,
 * booking lifecycle events and occupancy snapshots. All monetary
 * figures are "recorded revenue": fees stored on bookings that have a
 * recorded payment method and payment timestamp. They are not
 * independently verified settlements.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);
    private static final String RECORDED_REVENUE_NOTE =
            "Recorded revenue: fees on bookings with a recorded payment method and payment time. "
                    + "Not independently verified settlements.";

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ParkingSlotRepository parkingSlotRepository;

    @Autowired
    private OccupancySnapshotRepository occupancySnapshotRepository;

    @Autowired
    private BookingEventRepository bookingEventRepository;

    // ============================ OVERVIEW ============================

    public AnalyticsOverviewDTO getOverview(LocalDateTime from, LocalDateTime to) {
        Object[] stats = firstRow(bookingRepository.completedBookingStats(from, to));
        long sessions = num(stats, 0).longValue();
        double revenue = num(stats, 1).doubleValue();
        Double avgDuration = stats != null && stats[2] != null ? num(stats, 2).doubleValue() : null;

        // Previous period of equal length immediately before `from`
        Duration period = Duration.between(from, to);
        LocalDateTime prevFrom = from.minus(period);
        LocalDateTime prevTo = from;
        Object[] prevStats = firstRow(bookingRepository.completedBookingStats(prevFrom, prevTo));
        long prevSessions = prevStats != null ? num(prevStats, 0).longValue() : 0;
        double prevRevenue = prevStats != null ? num(prevStats, 1).doubleValue() : 0;

        OccupancyAnalyticsDTO.CurrentOccupancy current = currentOccupancy();

        return new AnalyticsOverviewDTO(
                sessions,
                revenue,
                sessions > 0 ? revenue / sessions : 0,
                avgDuration,
                current.occupied(),
                current.available(),
                current.locked(),
                current.capacity(),
                current.utilizationPercent(),
                prevSessions,
                prevRevenue,
                RECORDED_REVENUE_NOTE);
    }

    // ============================ REVENUE ============================

    public RevenueAnalyticsDTO getRevenue(LocalDateTime from, LocalDateTime to) {
        List<Object[]> rows = bookingRepository.recordedRevenueByDayAndMethod(from, to);

        // Aggregate per day across methods
        Map<LocalDate, double[]> dayAgg = new TreeMap<>(); // date -> [revenue, sessions]
        Map<String, double[]> methodAgg = new HashMap<>();
        double total = 0;
        long sessions = 0;

        for (Object[] r : rows) {
            LocalDate day = toLocalDate(r[0]);
            String method = str(r[1]);
            double rev = num(r[2]).doubleValue();
            long cnt = num(r[3]).longValue();
            dayAgg.computeIfAbsent(day, d -> new double[2]);
            dayAgg.get(day)[0] += rev;
            dayAgg.get(day)[1] += cnt;
            methodAgg.computeIfAbsent(method, m -> new double[2]);
            methodAgg.get(method)[0] += rev;
            methodAgg.get(method)[1] += cnt;
            total += rev;
            sessions += cnt;
        }

        List<RevenueAnalyticsDTO.RevenuePoint> daily = new ArrayList<>();
        RevenueAnalyticsDTO.RevenuePoint highest = null;
        for (Map.Entry<LocalDate, double[]> e : dayAgg.entrySet()) {
            RevenueAnalyticsDTO.RevenuePoint p = new RevenueAnalyticsDTO.RevenuePoint(
                    e.getKey(), e.getValue()[0], (long) e.getValue()[1]);
            daily.add(p);
            if (highest == null || p.revenue() > highest.revenue()) {
                highest = p;
            }
        }

        // Weekly (ISO week starting Monday) and monthly rollups from daily points
        Map<LocalDate, double[]> weekAgg = new TreeMap<>();
        Map<LocalDate, double[]> monthAgg = new TreeMap<>();
        for (RevenueAnalyticsDTO.RevenuePoint p : daily) {
            LocalDate weekStart = p.period().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate monthStart = p.period().withDayOfMonth(1);
            weekAgg.computeIfAbsent(weekStart, d -> new double[2]);
            weekAgg.get(weekStart)[0] += p.revenue();
            weekAgg.get(weekStart)[1] += p.sessions();
            monthAgg.computeIfAbsent(monthStart, d -> new double[2]);
            monthAgg.get(monthStart)[0] += p.revenue();
            monthAgg.get(monthStart)[1] += p.sessions();
        }

        List<RevenueAnalyticsDTO.RevenuePoint> weekly = toPoints(weekAgg);
        List<RevenueAnalyticsDTO.RevenuePoint> monthly = toPoints(monthAgg);

        List<RevenueAnalyticsDTO.MethodRevenue> byMethod = methodAgg.entrySet().stream()
                .map(e -> new RevenueAnalyticsDTO.MethodRevenue(
                        e.getKey(), e.getValue()[0], (long) e.getValue()[1]))
                .sorted((a, b) -> Double.compare(b.revenue(), a.revenue()))
                .toList();

        List<RevenueAnalyticsDTO.FloorRevenue> byFloor = bookingRepository
                .recordedRevenueByFloor(from, to).stream()
                .map(r -> new RevenueAnalyticsDTO.FloorRevenue(
                        (Integer) r[0], num(r[1]).doubleValue(), num(r[2]).longValue()))
                .toList();

        // Previous period comparison (equal length)
        Duration period = Duration.between(from, to);
        Object[] prevRevenue = firstRow(bookingRepository.completedBookingStats(from.minus(period), from));
        double prevRevenueTotal = prevRevenue != null ? num(prevRevenue, 1).doubleValue() : 0;

        return new RevenueAnalyticsDTO(
                "recordedRevenue",
                total,
                sessions > 0 ? total / sessions : 0,
                highest,
                prevRevenueTotal,
                daily,
                weekly,
                monthly,
                byMethod,
                byFloor);
    }

    // ============================ OCCUPANCY ============================

    public OccupancyAnalyticsDTO getOccupancy(LocalDateTime from, LocalDateTime to, Integer floor) {
        OccupancyAnalyticsDTO.CurrentOccupancy current = currentOccupancy();

        OccupancySnapshot trackingStart = occupancySnapshotRepository.findTopByOrderByCapturedAtAsc();
        LocalDateTime trackingStartAt = trackingStart != null ? trackingStart.getCapturedAt() : null;

        // Snapshots are only available from tracking start onward; queries
        // before that simply return no rows (reported via trackingStart).
        List<OccupancySnapshot> trendSnapshots = floor != null
                ? occupancySnapshotRepository.findByFloorAndCapturedAtBetweenOrderByCapturedAtAsc(
                        floor, from, to)
                : occupancySnapshotRepository.findByCapturedAtBetweenOrderByCapturedAtAscFloorAsc(from, to);

        List<OccupancyAnalyticsDTO.OccupancyPoint> trend = trendSnapshots.stream()
                .map(s -> new OccupancyAnalyticsDTO.OccupancyPoint(
                        s.getCapturedAt(), s.getFloor(),
                        s.getOccupiedSlots(), s.getAvailableSlots(), s.getLockedSlots(),
                        s.getTotalCapacity(), utilization(s)))
                .toList();

        Object[] avgRow = firstRow(occupancySnapshotRepository.averageUtilizationBetween(from, to));
        Double avgUtil = avgRow != null && avgRow[0] != null ? num(avgRow, 0).doubleValue() : null;
        Long snapCount = avgRow != null && avgRow[1] != null ? num(avgRow, 1).longValue() : 0L;

        List<OccupancyAnalyticsDTO.FloorUtilization> byFloor = occupancySnapshotRepository
                .utilizationByFloorBetween(from, to).stream()
                .map(r -> new OccupancyAnalyticsDTO.FloorUtilization(
                        (Integer) r[0],
                        r[1] != null ? num(r, 1).doubleValue() : 0,
                        r[2] != null ? num(r, 2).doubleValue() : 0,
                        r[3] != null ? num(r, 3).doubleValue() : 0,
                        num(r, 4).longValue()))
                .toList();

        List<OccupancyAnalyticsDTO.PeakPeriod> peaks = occupancySnapshotRepository
                .findTop10ByCapturedAtBetweenOrderByOccupiedSlotsDescCapturedAtAsc(from, to)
                .stream()
                .map(s -> new OccupancyAnalyticsDTO.PeakPeriod(
                        s.getCapturedAt(), s.getFloor(), utilization(s),
                        s.getOccupiedSlots(), s.getTotalCapacity()))
                .toList();

        return new OccupancyAnalyticsDTO(
                current, trackingStartAt, snapCount, avgUtil, byFloor, trend, peaks);
    }

    // ============================ VEHICLES ============================

    public VehicleAnalyticsDTO getVehicles(LocalDateTime from, LocalDateTime to) {
        List<Object[]> typeRows = bookingRepository.vehicleTypeStats(from, to);
        long totalSessions = typeRows.stream().mapToLong(r -> num(r, 1).longValue()).sum();

        List<VehicleAnalyticsDTO.VehicleTypeStats> byType = typeRows.stream()
                .map(r -> new VehicleAnalyticsDTO.VehicleTypeStats(
                        str(r[0]),
                        num(r, 1).longValue(),
                        totalSessions > 0 ? num(r, 1).longValue() * 100.0 / totalSessions : 0,
                        r[2] != null ? num(r, 2).doubleValue() : null,
                        r[3] != null ? num(r, 3).doubleValue() : null))
                .sorted((a, b) -> Long.compare(b.sessions(), a.sessions()))
                .toList();

        List<VehicleAnalyticsDTO.VehicleFloorStats> byFloor = bookingRepository
                .vehicleTypeByFloor(from, to).stream()
                .map(r -> new VehicleAnalyticsDTO.VehicleFloorStats(
                        (Integer) r[0], str(r[1]), num(r, 2).longValue()))
                .toList();

        List<VehicleAnalyticsDTO.DailyCount> trends = bookingRepository
                .dailyEntryTrend(from, to).stream()
                .map(r -> new VehicleAnalyticsDTO.DailyCount(toLocalDate(r[0]), num(r, 1).longValue()))
                .toList();

        return new VehicleAnalyticsDTO(byType, byFloor, trends);
    }

    // ============================ OPERATIONS ============================

    public OperationalAnalyticsDTO getOperations(LocalDateTime from, LocalDateTime to) {
        Object[] act = firstRow(bookingRepository.bookingActivityStats(from, to));
        long total = act != null ? num(act, 0).longValue() : 0;
        long active = act != null && act[1] != null ? num(act, 1).longValue() : 0;

        Object[] comp = firstRow(bookingRepository.completedBookingStats(from, to));
        long completed = comp != null ? num(comp, 0).longValue() : 0;
        double revenue = comp != null ? num(comp, 1).doubleValue() : 0;
        Double avgDur = comp != null && comp[2] != null ? num(comp, 2).doubleValue() : null;

        Map<String, Long> payments = new HashMap<>();
        for (Object[] r : bookingRepository.paymentMethodCounts(from, to)) {
            payments.put(str(r[0]), num(r, 1).longValue());
        }

        List<OperationalAnalyticsDTO.DailyCount> trends = bookingRepository
                .dailyEntryTrend(from, to).stream()
                .map(r -> new OperationalAnalyticsDTO.DailyCount(toLocalDate(r[0]), num(r, 1).longValue()))
                .toList();

        Map<String, Long> statusDist = new HashMap<>();
        for (Object[] r : bookingRepository.statusDistribution(from, to)) {
            statusDist.put(r[0] != null ? r[0].toString() : "UNKNOWN", num(r, 1).longValue());
        }

        long events = bookingEventRepository.countByEventTimestampBetween(from, to);

        return new OperationalAnalyticsDTO(
                total, active, completed, avgDur,
                completed > 0 ? revenue / completed : 0,
                payments, trends, statusDist, events);
    }

    // ============================ HOURLY TRAFFIC ============================

    public HourlyTrafficDTO getHourlyTraffic(LocalDateTime from, LocalDateTime to) {
        int[] entries = new int[24];
        int[] exits = new int[24];
        double[] revenue = new double[24];

        for (Object[] r : bookingRepository.entriesByHour(from, to)) {
            entries[((Number) r[0]).intValue()] = num(r, 1).intValue();
        }
        for (Object[] r : bookingRepository.exitsByHour(from, to)) {
            exits[((Number) r[0]).intValue()] = num(r, 1).intValue();
        }
        for (Object[] r : bookingRepository.revenueByHour(from, to)) {
            revenue[((Number) r[0]).intValue()] = num(r, 1).doubleValue();
        }

        List<HourlyTrafficDTO.PeakHour> peaks = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            if (entries[h] > 0 || exits[h] > 0 || revenue[h] > 0) {
                peaks.add(new HourlyTrafficDTO.PeakHour(h, entries[h], exits[h], revenue[h]));
            }
        }
        peaks.sort((a, b) -> Long.compare(b.entries() + b.exits(), a.entries() + a.exits()));

        return new HourlyTrafficDTO(
                toIntList(entries), toIntList(exits), toDoubleList(revenue),
                peaks.stream().limit(10).toList(),
                java.time.ZoneId.systemDefault().getId());
    }

    // ============================ HELPERS ============================

    private OccupancyAnalyticsDTO.CurrentOccupancy currentOccupancy() {
        long occupied = 0, available = 0, locked = 0;
        for (Object[] row : parkingSlotRepository.countByFloorAndStatus()) {
            SlotStatus status = (SlotStatus) row[1];
            long count = num(row, 2).longValue();
            switch (status) {
                case OCCUPIED -> occupied += count;
                case AVAILABLE -> available += count;
                case LOCKED -> locked += count;
            }
        }
        long capacity = occupied + available + locked;
        double pct = capacity > 0 ? occupied * 100.0 / capacity : 0;
        return new OccupancyAnalyticsDTO.CurrentOccupancy(occupied, available, locked, capacity, pct);
    }

    private static double utilization(OccupancySnapshot s) {
        return s.getTotalCapacity() != null && s.getTotalCapacity() > 0
                ? s.getOccupiedSlots() * 100.0 / s.getTotalCapacity()
                : 0;
    }

    private static List<RevenueAnalyticsDTO.RevenuePoint> toPoints(Map<LocalDate, double[]> agg) {
        return agg.entrySet().stream()
                .map(e -> new RevenueAnalyticsDTO.RevenuePoint(
                        e.getKey(), e.getValue()[0], (long) e.getValue()[1]))
                .toList();
    }

    private static Object[] firstRow(List<Object[]> rows) {
        return rows != null && !rows.isEmpty() ? rows.get(0) : null;
    }

    private static Number num(Object[] row, int idx) {
        if (row == null || idx >= row.length || row[idx] == null) {
            return 0;
        }
        return row[idx] instanceof Number n ? n : 0;
    }

    private static Number num(Object value) {
        return value instanceof Number n ? n : 0;
    }

    private static String str(Object o) {
        return o != null ? o.toString() : "UNKNOWN";
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (o instanceof LocalDate d) {
            return d;
        }
        return LocalDate.parse(o.toString());
    }

    private static List<Integer> toIntList(int[] arr) {
        List<Integer> list = new ArrayList<>(24);
        for (int v : arr) {
            list.add(v);
        }
        return list;
    }

    private static List<Double> toDoubleList(double[] arr) {
        List<Double> list = new ArrayList<>(24);
        for (double v : arr) {
            list.add(v);
        }
        return list;
    }
}
