package com.apipulse.api_pulse_app.agent;

import com.apipulse.api_pulse_app.agent.tools.SendAlertTool;
import com.apipulse.api_pulse_app.payment.PaymentEntity;
import com.apipulse.api_pulse_app.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EscalationPolicyService {

    private final PaymentRepository paymentRepository;
    private final SendAlertTool sendAlertTool;

    private boolean escalationOpen = false;

    public void checkEscalation() {
        List<PaymentEntity> recent = paymentRepository.findTop10ByOrderByCreatedAtDesc();

        if (recent.isEmpty()) {
            return;
        }

        long failCount = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.FAILED)
                .count();

        double errorRate = (double) failCount / recent.size() * 100.0;
        errorRate = Math.min(errorRate, 100.0);

        if (errorRate >= 80.0 && !escalationOpen) {
            escalationOpen = true;

            log.error("[ESCALATION] 에러율 {}% 초과 — 사람 검토 필요",
                    String.format("%.1f", errorRate));

            sendAlertTool.sendSlackAlert(
                    String.format(
                            "🚨 [에스컬레이션] 에러율 %.1f%% 초과 — Agent 자율 처리 범위 초과. 담당자 즉시 확인 필요.",
                            errorRate
                    )
            );
        }

        if (errorRate < 80.0 && escalationOpen) {
            escalationOpen = false;

            log.info("[ESCALATION] 정상 복귀 - 에러율 {}%",
                    String.format("%.1f", errorRate));
        }
    }
}