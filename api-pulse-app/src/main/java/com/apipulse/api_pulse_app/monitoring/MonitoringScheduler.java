package com.apipulse.api_pulse_app.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonitoringScheduler {

    private final MonitoringController monitoringController;

    @Scheduled(fixedRate = 30000) // 30초마다
    public void monitor() {
        monitoringController.check();
    }
}