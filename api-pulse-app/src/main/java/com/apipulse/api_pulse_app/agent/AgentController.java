package com.apipulse.api_pulse_app.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentExecutionService agentExecutionService;
    private final AgentLogRepository agentLogRepository;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyze(@RequestBody Map<String, String> req) {
        String situation = req.getOrDefault("situation", "결제 시스템 장애가 감지되었습니다. 분석해주세요.");
        log.info("Agent 분석 요청: {}", situation);

        AgentExecutionResult executionResult = agentExecutionService.execute("MANUAL", situation);
        return ResponseEntity.ok(Map.of("result", executionResult.getResult()));
    }

    @PostMapping("/analyze/card-error")
    public ResponseEntity<Map<String, String>> analyzeCardError() {
        AgentExecutionResult executionResult = agentExecutionService.execute(
                "CARD_ERROR",
                "카드 오류가 폭증하고 있습니다. 결제 실패율이 급격히 높아졌습니다. 원인을 분석하고 조치해주세요."
        );
        return ResponseEntity.ok(Map.of("result", executionResult.getResult()));
    }

    @PostMapping("/analyze/timeout")
    public ResponseEntity<Map<String, String>> analyzeTimeout() {
        AgentExecutionResult executionResult = agentExecutionService.execute(
                "TIMEOUT",
                "결제 처리 시간이 비정상적으로 길어지고 있습니다. PG사 타임아웃이 의심됩니다. 분석 및 조치해주세요."
        );
        return ResponseEntity.ok(Map.of("result", executionResult.getResult()));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AgentLogEntity>> getLogs() {
        return ResponseEntity.ok(
                agentLogRepository.findTop20ByOrderByCreatedAtDesc()
        );
    }

    /**
     * 기존 다른 클래스(예: AnomalyDetectionService)에서 호출하던 runAgent 유지용
     */
    public String runAgent(String triggerType, String situation) {
        return agentExecutionService.execute(triggerType, situation).getResult();
    }
}