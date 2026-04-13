package com.apipulse.api_pulse_app.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    @PostMapping("/check")
    public ResponseEntity<MonitoringSummaryDto> check() {
        return ResponseEntity.ok(monitoringService.getCurrentSummary());
    }

    @GetMapping("/summary")
    public ResponseEntity<MonitoringSummaryDto> summary() {
        return ResponseEntity.ok(monitoringService.getCurrentSummary());
    }
}