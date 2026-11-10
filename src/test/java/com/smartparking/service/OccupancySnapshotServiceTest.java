package com.smartparking.service;

import com.smartparking.dto.OccupancyHealthDTO;
import com.smartparking.entity.OccupancySnapshot;
import com.smartparking.enums.SlotStatus;
import com.smartparking.repository.OccupancySnapshotRepository;
import com.smartparking.repository.ParkingSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OccupancySnapshotServiceTest {

    @Mock
    private ParkingSlotRepository parkingSlotRepository;

    @Mock
    private OccupancySnapshotRepository occupancySnapshotRepository;

    @Mock
    private OccupancySnapshotWriter occupancySnapshotWriter;

    @InjectMocks
    private OccupancySnapshotService occupancySnapshotService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(occupancySnapshotService, "snapshotEnabled", true);
        ReflectionTestUtils.setField(occupancySnapshotService, "snapshotIntervalMs", 900000L);
        ReflectionTestUtils.setField(occupancySnapshotService, "zoneIdProperty", "Asia/Kolkata");
        lenient().when(occupancySnapshotRepository.tryAcquireSnapshotLock(anyLong())).thenReturn(true);
        lenient().when(occupancySnapshotWriter.persistIfAbsent(any(OccupancySnapshot.class))).thenReturn(true);
    }

    private Object[] row(int floor, SlotStatus status, long count) {
        return new Object[]{floor, status, count};
    }

    @Test
    void capture_writesSnapshotPerFloorWithCorrectCounts() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.OCCUPIED, 5L),
                row(1, SlotStatus.AVAILABLE, 290L),
                row(1, SlotStatus.LOCKED, 5L),
                row(2, SlotStatus.OCCUPIED, 10L),
                row(2, SlotStatus.AVAILABLE, 285L),
                row(2, SlotStatus.LOCKED, 5L)));

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(2);
        ArgumentCaptor<OccupancySnapshot> captor = ArgumentCaptor.forClass(OccupancySnapshot.class);
        verify(occupancySnapshotWriter, org.mockito.Mockito.times(2)).persistIfAbsent(captor.capture());

        OccupancySnapshot floor1 = captor.getAllValues().get(0);
        assertThat(floor1.getFloor()).isEqualTo(1);
        assertThat(floor1.getTotalCapacity()).isEqualTo(300);
        assertThat(floor1.getOccupiedSlots()).isEqualTo(5);
        assertThat(floor1.getAvailableSlots()).isEqualTo(290);
        assertThat(floor1.getLockedSlots()).isEqualTo(5);
        assertThat(floor1.getCapturedAt()).isNotNull();
        // captured_at quantized to 15-minute bucket (seconds/nanos zeroed)
        assertThat(floor1.getCapturedAt().getSecond()).isEqualTo(0);
        assertThat(floor1.getCapturedAt().getMinute() % 15).isEqualTo(0);

        OccupancySnapshot floor2 = captor.getAllValues().get(1);
        assertThat(floor2.getFloor()).isEqualTo(2);
        assertThat(floor2.getTotalCapacity()).isEqualTo(300);
        assertThat(floor2.getOccupiedSlots()).isEqualTo(10);
    }

    @Test
    void capture_emptyFacility_writesNothing() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of());

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(0);
        verify(occupancySnapshotWriter, never()).persistIfAbsent(any(OccupancySnapshot.class));
    }

    @Test
    void capture_allAvailable_slotsCountedAsAvailable() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.AVAILABLE, 300L)));

        occupancySnapshotService.captureOccupancySnapshot();

        ArgumentCaptor<OccupancySnapshot> captor = ArgumentCaptor.forClass(OccupancySnapshot.class);
        verify(occupancySnapshotWriter).persistIfAbsent(captor.capture());
        assertThat(captor.getValue().getAvailableSlots()).isEqualTo(300);
        assertThat(captor.getValue().getOccupiedSlots()).isZero();
        assertThat(captor.getValue().getLockedSlots()).isZero();
    }

    @Test
    void capture_existingBucket_skipsDuplicate() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.AVAILABLE, 300L)));
        when(occupancySnapshotWriter.persistIfAbsent(any(OccupancySnapshot.class))).thenReturn(false);

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(0);
    }

    @Test
    void capture_oneFloorDuplicate_doesNotBlockOtherFloors() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.AVAILABLE, 300L),
                row(2, SlotStatus.AVAILABLE, 300L)));
        when(occupancySnapshotWriter.persistIfAbsent(argThat(s -> s.getFloor() == 1)))
                .thenReturn(false);

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(1);
        verify(occupancySnapshotWriter).persistIfAbsent(argThat(s -> s.getFloor() == 2));
    }

    @Test
    void capture_lockNotAcquired_skipsCollection() {
        when(occupancySnapshotRepository.tryAcquireSnapshotLock(anyLong())).thenReturn(false);

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(0);
        verify(parkingSlotRepository, never()).countByFloorAndStatus();
        verify(occupancySnapshotWriter, never()).persistIfAbsent(any(OccupancySnapshot.class));
    }

    @Test
    void capture_constraintViolation_treatedAsDuplicate() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.AVAILABLE, 300L)));
        when(occupancySnapshotWriter.persistIfAbsent(any(OccupancySnapshot.class))).thenReturn(false);

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(0);
    }

    @Test
    void captureSafely_databaseFailure_loggedNotPropagated() {
        when(parkingSlotRepository.countByFloorAndStatus())
                .thenThrow(new RuntimeException("connection refused"));

        assertThatCode(() -> occupancySnapshotService.captureSafely()).doesNotThrowAnyException();
    }

    @Test
    void captureOccupancySnapshot_databaseFailure_propagates() {
        when(parkingSlotRepository.countByFloorAndStatus())
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> occupancySnapshotService.captureOccupancySnapshot())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("connection refused");
    }

    @Test
    void captureSafely_disabled_doesNothing() {
        ReflectionTestUtils.setField(occupancySnapshotService, "snapshotEnabled", false);

        occupancySnapshotService.captureSafely();

        verify(parkingSlotRepository, never()).countByFloorAndStatus();
    }

    @Test
    void currentBucket_quantizesToInterval() {
        LocalDateTime bucket = occupancySnapshotService.currentBucket(java.time.ZoneId.of("Asia/Kolkata"));

        assertThat(bucket.getSecond()).isZero();
        assertThat(bucket.getNano()).isZero();
        assertThat(bucket.getMinute() % 15).isZero();
        // Same bucket for times within the same 15-minute window
        LocalDateTime same = occupancySnapshotService.currentBucket(java.time.ZoneId.of("Asia/Kolkata"));
        assertThat(same).isEqualTo(bucket);
    }

    @Test
    void advisoryLockFailure_proceedsWithoutLock() {
        when(occupancySnapshotRepository.tryAcquireSnapshotLock(anyLong()))
                .thenThrow(new RuntimeException("function does not exist"));
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.AVAILABLE, 300L)));

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(1);
    }

    // ==================== SNAPSHOT HEALTH ====================

    private static final java.time.ZoneId ZONE = java.time.ZoneId.of("Asia/Kolkata");
    private static final long INTERVAL_SEC = 900; // 15 min, matches setUp intervalMs

    /** Bucket-aligned timestamp for an epoch second offset from now. */
    private LocalDateTime bucket(long epochSecondOffset) {
        long sec = java.time.Instant.now().getEpochSecond() + epochSecondOffset;
        long bucketed = (sec / INTERVAL_SEC) * INTERVAL_SEC;
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochSecond(bucketed), ZONE);
    }

    private Object[] pair(int floor, LocalDateTime at) {
        return new Object[]{floor, at};
    }

    private void stubHealthData(List<Object[]> pairs, long last24h) {
        when(occupancySnapshotRepository.findDistinctFloorCapturedAt()).thenReturn(pairs);
        when(occupancySnapshotRepository.countByCapturedAtGreaterThanEqual(any(LocalDateTime.class)))
                .thenReturn(last24h);
    }

    @Test
    void health_noSnapshots_reportsNoSnapshots() {
        stubHealthData(List.of(), 0L);

        OccupancyHealthDTO h = occupancySnapshotService.getSnapshotHealth();

        assertThat(h.status()).isEqualTo(OccupancyHealthDTO.STATUS_NO_SNAPSHOTS);
        assertThat(h.collectionEnabled()).isTrue();
        assertThat(h.snapshotIntervalMs()).isEqualTo(900000L);
        assertThat(h.zoneId()).isEqualTo("Asia/Kolkata");
        assertThat(h.earliestSnapshotAt()).isNull();
        assertThat(h.latestSnapshotAt()).isNull();
        assertThat(h.snapshotsLast24h()).isZero();
        assertThat(h.overdue()).isFalse();
        assertThat(h.missingIntervalCount()).isZero();
    }

    @Test
    void health_disabled_reportsDisabledEvenWithData() {
        ReflectionTestUtils.setField(occupancySnapshotService, "snapshotEnabled", false);
        LocalDateTime now = bucket(0);
        stubHealthData(List.of(pair(1, now), pair(1, bucket(-INTERVAL_SEC))), 4L);

        OccupancyHealthDTO h = occupancySnapshotService.getSnapshotHealth();

        assertThat(h.status()).isEqualTo(OccupancyHealthDTO.STATUS_DISABLED);
        assertThat(h.collectionEnabled()).isFalse();
        assertThat(h.latestSnapshotAt()).isEqualTo(now);
        assertThat(h.snapshotsLast24h()).isEqualTo(4L);
    }

    @Test
    void health_recentSnapshots_reportsActive() {
        stubHealthData(List.of(
                pair(1, bucket(-2 * INTERVAL_SEC)),
                pair(1, bucket(-INTERVAL_SEC)),
                pair(1, bucket(0)),
                pair(2, bucket(-INTERVAL_SEC)),
                pair(2, bucket(0))), 5L);

        OccupancyHealthDTO h = occupancySnapshotService.getSnapshotHealth();

        assertThat(h.status()).isEqualTo(OccupancyHealthDTO.STATUS_ACTIVE);
        assertThat(h.overdue()).isFalse();
        assertThat(h.trackedFloorCount()).isEqualTo(2);
        assertThat(h.expectedIntervals()).isEqualTo(5);
        assertThat(h.observedIntervals()).isEqualTo(5);
        assertThat(h.missingIntervalCount()).isZero();
        assertThat(h.missingIntervals()).isEmpty();
        assertThat(h.latestSnapshotAt()).isEqualTo(bucket(0));
        assertThat(h.earliestSnapshotAt()).isEqualTo(bucket(-2 * INTERVAL_SEC));
    }

    @Test
    void health_staleLatestSnapshot_reportsDelayed() {
        // Latest snapshot ~2h old: several full intervals missed
        stubHealthData(List.of(
                pair(1, bucket(-8 * INTERVAL_SEC)),
                pair(1, bucket(-7 * INTERVAL_SEC))), 0L);

        OccupancyHealthDTO h = occupancySnapshotService.getSnapshotHealth();

        assertThat(h.overdue()).isTrue();
        assertThat(h.status()).isEqualTo(OccupancyHealthDTO.STATUS_DELAYED);
    }

    @Test
    void health_missingIntervals_detectsGapsButNotPreHistory() {
        // Floor 1 has snapshots at buckets -3,-2,-1 intervals ago but
        // bucket -2 is missing (gap). Period before first snapshot
        // (-4 and earlier) must NOT be reported.
        LocalDateTime b3 = bucket(-3 * INTERVAL_SEC);
        LocalDateTime b2 = bucket(-2 * INTERVAL_SEC);
        LocalDateTime b1 = bucket(-INTERVAL_SEC);
        stubHealthData(List.of(
                pair(1, b3),
                pair(1, b1),
                pair(2, b3),
                pair(2, b2),
                pair(2, b1)), 5L);

        OccupancyHealthDTO h = occupancySnapshotService.getSnapshotHealth();

        // floor1 expected 3 (b3,b2,b1) observed 2 -> 1 missing;
        // floor2 expected 3 observed 3 -> 0 missing. Total 1.
        assertThat(h.expectedIntervals()).isEqualTo(6);
        assertThat(h.observedIntervals()).isEqualTo(5);
        assertThat(h.missingIntervalCount()).isEqualTo(1);
        assertThat(h.missingIntervals()).containsExactly(b2);
    }

    @Test
    void health_multipleFloors_countsPerFloorPairs() {
        // floor2 started tracking later — its pre-start period is not missing
        stubHealthData(List.of(
                pair(1, bucket(-3 * INTERVAL_SEC)),
                pair(1, bucket(-2 * INTERVAL_SEC)),
                pair(1, bucket(-INTERVAL_SEC)),
                pair(2, bucket(-INTERVAL_SEC))), 4L);

        OccupancyHealthDTO h = occupancySnapshotService.getSnapshotHealth();

        assertThat(h.trackedFloorCount()).isEqualTo(2);
        assertThat(h.expectedIntervals()).isEqualTo(4);
        assertThat(h.observedIntervals()).isEqualTo(4);
        assertThat(h.missingIntervalCount()).isZero();
        assertThat(h.earliestSnapshotAt()).isEqualTo(bucket(-3 * INTERVAL_SEC));
        assertThat(h.latestSnapshotAt()).isEqualTo(bucket(-INTERVAL_SEC));
    }

    @Test
    void health_databaseFailure_propagates() {
        when(occupancySnapshotRepository.findDistinctFloorCapturedAt())
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> occupancySnapshotService.getSnapshotHealth())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("connection refused");
    }
}
