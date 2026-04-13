package com.apipulse.api_pulse_app.monitoring;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonitoringSummaryDto {
    private long totalCount;
    private long approvedCount;
    private long failedCount;
    private double errorRate;
    private String topFailReason;
    private DetectionStatus status;
    private boolean detected;
    private String message;
}