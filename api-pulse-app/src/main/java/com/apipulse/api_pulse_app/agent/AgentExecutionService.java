package com.apipulse.api_pulse_app.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentExecutionService {

    private final FinGuardAgent finGuardAgent;
    private final EscalationPolicyService escalationPolicyService;
    private final AgentLogService agentLogService;

    public AgentExecutionResult execute(String triggerType, String situation) {
        long startTime = System.currentTimeMillis();
        log.info("Agent 실행 시작 - type: {}", triggerType);

        escalationPolicyService.checkEscalation();

        String result = finGuardAgent.analyze(situation);
        long elapsed = System.currentTimeMillis() - startTime;

        AgentExecutionResult executionResult = AgentExecutionResult.builder()
                .triggerType(triggerType)
                .situation(situation)
                .result(result)
                .responseTimeMs(elapsed)
                .toolsUsed("GetMetrics,SearchLogs,AnalyzePattern,RestartService,SendAlert,GenerateReport")
                .build();

        agentLogService.save(executionResult);

        log.info("Agent 실행 완료 - {}ms", elapsed);
        return executionResult;
    }
}