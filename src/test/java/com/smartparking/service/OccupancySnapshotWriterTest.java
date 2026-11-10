package com.smartparking.service;

import com.smartparking.entity.OccupancySnapshot;
import com.smartparking.repository.OccupancySnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OccupancySnapshotWriterTest {

    @Mock
    private OccupancySnapshotRepository occupancySnapshotRepository;

    @InjectMocks
    private OccupancySnapshotWriter occupancySnapshotWriter;

    private OccupancySnapshot snapshot() {
        return new OccupancySnapshot(LocalDateTime.of(2026, 1, 1, 10, 0), 1, 300, 5, 290, 5);
    }

    @Test
    void persistIfAbsent_writesWhenBucketMissing() {
        when(occupancySnapshotRepository.existsByFloorAndCapturedAt(1, snapshot().getCapturedAt()))
                .thenReturn(false);

        assertThat(occupancySnapshotWriter.persistIfAbsent(snapshot())).isTrue();
        verify(occupancySnapshotRepository).saveAndFlush(any(OccupancySnapshot.class));
    }

    @Test
    void persistIfAbsent_skipsExistingBucket() {
        OccupancySnapshot s = snapshot();
        when(occupancySnapshotRepository.existsByFloorAndCapturedAt(1, s.getCapturedAt()))
                .thenReturn(true);

        assertThat(occupancySnapshotWriter.persistIfAbsent(s)).isFalse();
        verify(occupancySnapshotRepository, never()).saveAndFlush(any(OccupancySnapshot.class));
    }

    @Test
    void persistIfAbsent_constraintViolation_treatedAsDuplicate() {
        OccupancySnapshot s = snapshot();
        when(occupancySnapshotRepository.existsByFloorAndCapturedAt(1, s.getCapturedAt()))
                .thenReturn(false);
        when(occupancySnapshotRepository.saveAndFlush(any(OccupancySnapshot.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThat(occupancySnapshotWriter.persistIfAbsent(s)).isFalse();
    }
}
