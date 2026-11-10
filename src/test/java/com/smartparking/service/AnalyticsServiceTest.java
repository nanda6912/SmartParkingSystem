package com.smartparking.service;

import com.smartparking.dto.AnalyticsOverviewDTO;
import com.smartparking.dto.HourlyTrafficDTO;
import com.smartparking.dto.OccupancyAnalyticsDTO;
import com.smartparking.dto.OperationalAnalyticsDTO;
import com.smartparking.dto.RevenueAnalyticsDTO;
import com.smartparking.dto.VehicleAnalyticsDTO;
import com.smartparking.entity.OccupancySnapshot;
import com.smartparking.enums.SlotStatus;
import com.smartparking.enums.VehicleType;
import com.smartparking.repository.BookingEventRepository;
import com.smartparking.repository.BookingRepository;
import com.smartparking.repository.OccupancySnapshotRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private OccupancySnapshotRepository occupancySnapshotRepository;

    @Mock
    private BookingEventRepository bookingEventRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private final LocalDateTime from = LocalDate.of(2026, 1, 1).atStartOfDay();
    private final LocalDateTime to = LocalDate.of(2026, 2, 1).atStartOfDay();

    @BeforeEach
    void setUp() {
        lenient().when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                new Object[]{1, SlotStatus.OCCUPIED, 60L},
                new Object[]{1, SlotStatus.AVAILABLE, 230L},
                new Object[]{1, SlotStatus.LOCKED, 10L},
                new Object[]{2, SlotStatus.OCCUPIED, 40L},
                new Object[]{2, SlotStatus.AVAILABLE, 250L},
                new Object[]{2, SlotStatus.LOCKED, 10L}));
        lenient().when(occupancySnapshotRepository.findTopByOrderByCapturedAtAsc()).thenReturn(null);
        lenient().when(bookingEventRepository.countByEventTimestampBetween(any(), any())).thenReturn(0L);
    }

    private Object[] statsRow(long count, double revenue, Double avgDur) {
        return new Object[]{count, revenue, avgDur};
    }

    @Test
    void overview_combinesCompletedStatsAndLiveOccupancy() {
        when(bookingRepository.completedBookingStats(from, to))
                .thenReturn(List.<Object[]>of(statsRow(10, 400.0, 95.0)));
        when(bookingRepository.completedBookingStats(any(), eq(from)))
                .thenReturn(List.<Object[]>of(statsRow(8, 300.0, 90.0)));

        AnalyticsOverviewDTO dto = analyticsService.getOverview(from, to);

        assertThat(dto.totalCompletedSessions()).isEqualTo(10);
        assertThat(dto.recordedRevenue()).isEqualTo(400.0);
        assertThat(dto.avgRevenuePerSession()).isEqualTo(40.0);
        assertThat(dto.avgDurationMinutes()).isEqualTo(95.0);
        // live occupancy: 100 occupied / 540 total = 18.51%
        assertThat(dto.currentOccupiedSlots()).isEqualTo(100);
        assertThat(dto.currentAvailableSlots()).isEqualTo(480);
        assertThat(dto.currentLockedSlots()).isEqualTo(20);
        assertThat(dto.totalCapacity()).isEqualTo(600);
        assertThat(dto.occupancyPercent()).isEqualTo(100 * 100.0 / 600);
        assertThat(dto.previousPeriodCompletedSessions()).isEqualTo(8);
        assertThat(dto.note()).contains("Recorded revenue");
    }

    @Test
    void revenue_aggregatesByDayMethodFloorAndFindsPeak() {
        LocalDate d1 = LocalDate.of(2026, 1, 10);
        LocalDate d2 = LocalDate.of(2026, 1, 11);
        when(bookingRepository.recordedRevenueByDayAndMethod(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{java.sql.Date.valueOf(d1), "UPI", 200.0, 5L},
                new Object[]{java.sql.Date.valueOf(d1), "CASH", 100.0, 3L},
                new Object[]{java.sql.Date.valueOf(d2), "UPI", 400.0, 7L}));
        when(bookingRepository.recordedRevenueByFloor(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{1, 400.0, 10L},
                new Object[]{2, 200.0, 5L}));
        when(bookingRepository.completedBookingStats(any(), eq(from)))
                .thenReturn(List.<Object[]>of(statsRow(9, 250.0, 80.0)));

        RevenueAnalyticsDTO dto = analyticsService.getRevenue(from, to);

        assertThat(dto.metricLabel()).isEqualTo("recordedRevenue");
        assertThat(dto.totalRecordedRevenue()).isEqualTo(700.0);
        assertThat(dto.avgRevenuePerSession()).isEqualTo(700.0 / 15);
        assertThat(dto.daily()).hasSize(2);
        assertThat(dto.highestRevenueDay().period()).isEqualTo(d2);
        assertThat(dto.highestRevenueDay().revenue()).isEqualTo(400.0);
        assertThat(dto.weekly()).hasSize(1);
        assertThat(dto.monthly()).hasSize(1);
        assertThat(dto.byPaymentMethod()).extracting(RevenueAnalyticsDTO.MethodRevenue::paymentMethod)
                .containsExactly("UPI", "CASH");
        assertThat(dto.byFloor()).hasSize(2);
        assertThat(dto.previousPeriodRevenue()).isEqualTo(250.0);
    }

    @Test
    void occupancy_usesSnapshotsAndReportsTrackingStart() {
        OccupancySnapshot s1 = new OccupancySnapshot(LocalDateTime.of(2026, 1, 5, 10, 0), 1, 300, 200, 90, 10);
        OccupancySnapshot earliest = new OccupancySnapshot(LocalDateTime.of(2026, 1, 1, 0, 15), 1, 300, 0, 300, 0);
        when(occupancySnapshotRepository.findTopByOrderByCapturedAtAsc()).thenReturn(earliest);
        when(occupancySnapshotRepository.findByCapturedAtBetweenOrderByCapturedAtAscFloorAsc(from, to))
                .thenReturn(List.of(s1));
        when(occupancySnapshotRepository.averageUtilizationBetween(from, to))
                .thenReturn(List.<Object[]>of(new Object[]{66.67, 4L}));
        when(occupancySnapshotRepository.utilizationByFloorBetween(from, to))
                .thenReturn(List.<Object[]>of(new Object[]{1, 66.67, 200.0, 300.0, 4L}));
        when(occupancySnapshotRepository.findTop10ByCapturedAtBetweenOrderByOccupiedSlotsDescCapturedAtAsc(from, to))
                .thenReturn(List.of(s1));

        OccupancyAnalyticsDTO dto = analyticsService.getOccupancy(from, to, null);

        assertThat(dto.trackingStart()).isEqualTo(earliest.getCapturedAt());
        assertThat(dto.snapshotsInRange()).isEqualTo(4);
        assertThat(dto.averageUtilizationPercent()).isEqualTo(66.67);
        assertThat(dto.trend()).hasSize(1);
        assertThat(dto.trend().get(0).utilizationPercent()).isEqualTo(200 * 100.0 / 300);
        assertThat(dto.byFloor()).hasSize(1);
        assertThat(dto.peakPeriods()).hasSize(1);
        assertThat(dto.current().capacity()).isEqualTo(600);
    }

    @Test
    void occupancy_beforeTrackingStart_returnsEmptyTrend() {
        when(occupancySnapshotRepository.findByFloorAndCapturedAtBetweenOrderByCapturedAtAsc(1, from, to))
                .thenReturn(List.of());
        when(occupancySnapshotRepository.averageUtilizationBetween(from, to))
                .thenReturn(List.<Object[]>of(new Object[]{null, 0L}));

        OccupancyAnalyticsDTO dto = analyticsService.getOccupancy(from, to, 1);

        assertThat(dto.trend()).isEmpty();
        assertThat(dto.averageUtilizationPercent()).isNull();
        assertThat(dto.trackingStart()).isNull();
    }

    @Test
    void vehicles_computesPercentagesAndAverages() {
        when(bookingRepository.vehicleTypeStats(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{VehicleType.CAR, 8L, 90.0, 200.0},
                new Object[]{VehicleType.BIKE, 2L, 60.0, 100.0}));
        when(bookingRepository.vehicleTypeByFloor(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{1, VehicleType.CAR, 5L},
                new Object[]{2, VehicleType.BIKE, 2L}));
        when(bookingRepository.dailyEntryTrend(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{java.sql.Date.valueOf(LocalDate.of(2026, 1, 3)), 4L}));

        VehicleAnalyticsDTO dto = analyticsService.getVehicles(from, to);

        assertThat(dto.byType()).hasSize(2);
        assertThat(dto.byType().get(0).vehicleType()).isEqualTo("CAR");
        assertThat(dto.byType().get(0).percentage()).isEqualTo(80.0);
        assertThat(dto.byType().get(0).avgDurationMinutes()).isEqualTo(90.0);
        assertThat(dto.byType().get(1).percentage()).isEqualTo(20.0);
        assertThat(dto.byFloor()).hasSize(2);
        assertThat(dto.entryTrends()).hasSize(1);
    }

    @Test
    void operations_aggregatesCountsAndPaymentMethods() {
        when(bookingRepository.bookingActivityStats(from, to))
                .thenReturn(List.<Object[]>of(new Object[]{15L, 3L}));
        when(bookingRepository.completedBookingStats(from, to))
                .thenReturn(List.<Object[]>of(statsRow(12, 480.0, 100.0)));
        when(bookingRepository.paymentMethodCounts(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{"UPI", 8L},
                new Object[]{"CASH", 4L}));
        when(bookingRepository.statusDistribution(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{com.smartparking.enums.BookingStatus.COMPLETED, 12L},
                new Object[]{com.smartparking.enums.BookingStatus.ACTIVE, 3L}));
        when(bookingRepository.dailyEntryTrend(from, to)).thenReturn(List.<Object[]>of());
        when(bookingEventRepository.countByEventTimestampBetween(from, to)).thenReturn(30L);

        OperationalAnalyticsDTO dto = analyticsService.getOperations(from, to);

        assertThat(dto.totalBookings()).isEqualTo(15);
        assertThat(dto.activeBookings()).isEqualTo(3);
        assertThat(dto.completedBookings()).isEqualTo(12);
        assertThat(dto.avgDurationMinutes()).isEqualTo(100.0);
        assertThat(dto.recordedRevenuePerCompletedSession()).isEqualTo(40.0);
        assertThat(dto.paymentMethodCounts()).containsEntry("UPI", 8L).containsEntry("CASH", 4L);
        assertThat(dto.statusDistribution()).containsEntry("COMPLETED", 12L);
        assertThat(dto.lifecycleEventsInRange()).isEqualTo(30);
    }

    @Test
    void hourlyTraffic_builds24HourProfilesAndPeaks() {
        when(bookingRepository.entriesByHour(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{9, 50L},
                new Object[]{18, 70L}));
        when(bookingRepository.exitsByHour(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{10, 40L},
                new Object[]{19, 60L}));
        when(bookingRepository.revenueByHour(from, to)).thenReturn(List.<Object[]>of(
                new Object[]{18, 800.0}));

        HourlyTrafficDTO dto = analyticsService.getHourlyTraffic(from, to);

        assertThat(dto.entriesByHour()).hasSize(24);
        assertThat(dto.entriesByHour().get(9)).isEqualTo(50);
        assertThat(dto.entriesByHour().get(18)).isEqualTo(70);
        assertThat(dto.exitsByHour().get(19)).isEqualTo(60);
        assertThat(dto.revenueByHour().get(18)).isEqualTo(800.0);
        assertThat(dto.peakHours().get(0).hour()).isEqualTo(18);
        assertThat(dto.timezone()).isNotBlank();
    }
}
