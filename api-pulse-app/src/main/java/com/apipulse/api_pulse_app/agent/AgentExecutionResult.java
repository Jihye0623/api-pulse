package com.apipulse.api_pulse_app.agent;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AgentExecutionResult {
    private String triggerType;
    private String situation;
    private String result;
    private long responseTimeMs;
    private String toolsUsed;
}