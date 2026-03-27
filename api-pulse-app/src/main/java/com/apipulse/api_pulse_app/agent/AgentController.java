package com.apipulse.api_pulse_app.agent;

import com.apipulse.api_pulse_app.agent.tools.SendAlertTool;
import com.apipulse.api_pulse_app.payment.PaymentEntity;
import com.apipulse.api_pulse_app.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final FinGuardAgent finGuardAgent;
    private final AgentLogRepository agentLogRepository;
    private final PaymentRepository paymentRepository;
    private final SendAlertTool sendAlertTool;

    // Agent 수동 트리거
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyze(@RequestBody Map<String, String> req) {
        String situation = req.getOrDefault("situation", "결제 시스템 장애가 감지되었습니다. 분석해주세요.");
        log.info("Agent 분석 요청: {}", situation);

        String result = runAgent("MANUAL", situation);
        return ResponseEntity.ok(Map.of("result", result));
    }

    // 카드 오류 장애 시나리오 자동 분석
    @PostMapping("/analyze/card-error")
    public ResponseEntity<Map<String, String>> analyzeCardError() {
        String result = runAgent(
                "CARD_ERROR",
                "카드 오류가 폭증하고 있습니다. 결제 실패율이 급격히 높아졌습니다. 원인을 분석하고 조치해주세요."
        );
        return ResponseEntity.ok(Map.of("result", result));
    }

    // 타임아웃 장애 시나리오 자동 분석
    @PostMapping("/analyze/timeout")
    public ResponseEntity<Map<String, String>> analyzeTimeout() {
        String result = runAgent(
                "TIMEOUT",
                "결제 처리 시간이 비정상적으로 길어지고 있습니다. PG사 타임아웃이 의심됩니다. 분석 및 조치해주세요."
        );
        return ResponseEntity.ok(Map.of("result", result));
    }

    public String runAgent(String triggerType, String situation) {
        long startTime = System.currentTimeMillis();
        log.info("Agent 실행 시작 - type: {}", triggerType);

        // 에스컬레이션 판단 (실행 전)
        checkEscalation(triggerType);

        String result = finGuardAgent.analyze(situation);
        long elapsed = System.currentTimeMillis() - startTime;

        agentLogRepository.save(AgentLogEntity.builder()
                .triggerType(triggerType)
                .situation(situation)
                .agentResult(result)
                .toolsUsed("GetMetrics,SearchLogs,AnalyzePattern," +
                        "RestartService,SendAlert,GenerateReport")
                .responseTimeMs(elapsed)
                .createdAt(LocalDateTime.now())
                .build());

        log.info("Agent 실행 완료 - {}ms", elapsed);
        return result;
    }

    public void checkEscalation(String triggerType) {
        List<PaymentEntity> recent = paymentRepository.findTop10ByOrderByCreatedAtDesc();

        if (recent.isEmpty()) return;

        long failCount = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.FAILED)
                .count();

        double errorRate = (double) failCount / recent.size() * 100;

        if (errorRate >= 80) {
            log.error("[ESCALATION] 에러율 {}% 초과 — 사람 검토 필요", String.format("%.1f", errorRate));
            sendAlertTool.sendSlackAlert(
                    String.format("🚨 [에스컬레이션] 에러율 %.1f%% 초과 — Agent 자율 처리 범위 초과. 담당자 즉시 확인 필요.", errorRate)
            );
        } else {
            log.info("[ESCALATION] 에러율 {}% — Agent 자율 처리 범위", String.format("%.1f", errorRate));
        }
    }
}