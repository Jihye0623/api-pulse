package com.apipulse.api_pulse_app.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AgentLogService {

    private final AgentLogRepository agentLogRepository;

    public void save(AgentExecutionResult executionResult) {
        agentLogRepository.save(
                AgentLogEntity.builder()
                        .triggerType(executionResult.getTriggerType())
                        .situation(executionResult.getSituation())
                        .agentResult(executionResult.getResult())
                        .toolsUsed(executionResult.getToolsUsed())
                        .responseTimeMs(executionResult.getResponseTimeMs())
                        .createdAt(LocalDateTime.now())
                        .build()
        );
    }
}