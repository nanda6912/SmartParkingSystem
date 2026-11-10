package com.smartparking.service;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

    @InjectMocks
    private OccupancySnapshotService occupancySnapshotService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(occupancySnapshotService, "snapshotEnabled", true);
        ReflectionTestUtils.setField(occupancySnapshotService, "snapshotIntervalMs", 900000L);
        ReflectionTestUtils.setField(occupancySnapshotService, "zoneIdProperty", "Asia/Kolkata");
        lenient().when(occupancySnapshotRepository.tryAcquireSnapshotLock(anyLong())).thenReturn(true);
        lenient().when(occupancySnapshotRepository.existsByFloorAndCapturedAt(anyInt(), any(LocalDateTime.class)))
                .thenReturn(false);
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
        verify(occupancySnapshotRepository, org.mockito.Mockito.times(2)).save(captor.capture());

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
        verify(occupancySnapshotRepository, never()).save(any(OccupancySnapshot.class));
    }

    @Test
    void capture_allAvailable_slotsCountedAsAvailable() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.AVAILABLE, 300L)));

        occupancySnapshotService.captureOccupancySnapshot();

        ArgumentCaptor<OccupancySnapshot> captor = ArgumentCaptor.forClass(OccupancySnapshot.class);
        verify(occupancySnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getAvailableSlots()).isEqualTo(300);
        assertThat(captor.getValue().getOccupiedSlots()).isZero();
        assertThat(captor.getValue().getLockedSlots()).isZero();
    }

    @Test
    void capture_existingBucket_skipsDuplicate() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.AVAILABLE, 300L)));
        when(occupancySnapshotRepository.existsByFloorAndCapturedAt(eq(1), any(LocalDateTime.class)))
                .thenReturn(true);

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(0);
        verify(occupancySnapshotRepository, never()).save(any(OccupancySnapshot.class));
    }

    @Test
    void capture_lockNotAcquired_skipsCollection() {
        when(occupancySnapshotRepository.tryAcquireSnapshotLock(anyLong())).thenReturn(false);

        int written = occupancySnapshotService.captureOccupancySnapshot();

        assertThat(written).isEqualTo(0);
        verify(parkingSlotRepository, never()).countByFloorAndStatus();
        verify(occupancySnapshotRepository, never()).save(any(OccupancySnapshot.class));
    }

    @Test
    void capture_constraintViolation_treatedAsDuplicate() {
        when(parkingSlotRepository.countByFloorAndStatus()).thenReturn(List.<Object[]>of(
                row(1, SlotStatus.AVAILABLE, 300L)));
        doThrow(new DataIntegrityViolationException("duplicate key"))
                .when(occupancySnapshotRepository).save(any(OccupancySnapshot.class));

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
}
