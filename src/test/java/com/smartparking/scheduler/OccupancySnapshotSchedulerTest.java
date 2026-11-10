package com.smartparking.scheduler;

import com.smartparking.service.OccupancySnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OccupancySnapshotSchedulerTest {

    @Mock
    private OccupancySnapshotService occupancySnapshotService;

    @InjectMocks
    private OccupancySnapshotScheduler scheduler;

    @Test
    void scheduledRun_delegatesToService() {
        scheduler.captureScheduledSnapshot();

        verify(occupancySnapshotService).captureSafely();
    }
}
