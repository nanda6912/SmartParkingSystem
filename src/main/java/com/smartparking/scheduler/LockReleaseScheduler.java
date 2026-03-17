package com.smartparking.scheduler;

import com.smartparking.service.ParkingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LockReleaseScheduler {
    
    @Autowired
    private ParkingService parkingService;
    
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void releaseExpiredLocks() {
        parkingService.releaseExpiredLocks();
    }
}
