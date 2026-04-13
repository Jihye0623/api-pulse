package com.apipulse.api_pulse_app.monitoring;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DetectionResult {
    private double currentErrorRate;
    private double meanErrorRate;
    private double stdDev;
    private double zScore;
    private boolean statisticalAnomaly;
    private boolean criticalByRule;
    private DetectionStatus status;
}