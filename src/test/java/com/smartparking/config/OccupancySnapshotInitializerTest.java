package com.smartparking.config;

import com.smartparking.service.OccupancySnapshotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OccupancySnapshotInitializerTest {

    @Mock
    private OccupancySnapshotService occupancySnapshotService;

    @InjectMocks
    private OccupancySnapshotInitializer initializer;

    @Test
    void run_capturesInitialSnapshotOnStartup() {
        initializer.run(new DefaultApplicationArguments());

        verify(occupancySnapshotService).captureSafely();
    }
}
