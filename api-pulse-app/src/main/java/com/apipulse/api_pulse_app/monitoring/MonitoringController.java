package com.apipulse.api_pulse_app.monitoring;

import com.apipulse.api_pulse_app.agent.AgentController;
import com.apipulse.api_pulse_app.payment.PaymentEntity;
import com.apipulse.api_pulse_app.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor")
@RequiredArgsConstructor
public class MonitoringController {

    private final PaymentRepository paymentRepository;
    private final AgentController agentController;

    @PostMapping("/check")
    public ResponseEntity<Map<String, Object>> check() {

        List<PaymentEntity> recent = paymentRepository.findTop20ByOrderByCreatedAtDesc();

        if (recent.isEmpty()) {
            return ResponseEntity.ok(Map.of("message", "데이터 없음"));
        }

        long failCount = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.FAILED)
                .count();

        double errorRate = (double) failCount / recent.size() * 100;

        if (errorRate >= 50) {
            String result = agentController.runAgent(
                    "LIVE_MONITOR",
                    String.format("실시간 모니터링 결과 최근 %d건 중 실패율 %.1f%% 입니다. 분석 및 조치해주세요.",
                            recent.size(), errorRate)
            );

            return ResponseEntity.ok(Map.of(
                    "detected", true,
                    "errorRate", errorRate,
                    "result", result
            ));
        }

        return ResponseEntity.ok(Map.of(
                "detected", false,
                "errorRate", errorRate
        ));
    }
}