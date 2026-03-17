package com.apipulse.api_pulse_app.agent.tools;

import com.apipulse.api_pulse_app.payment.PaymentEntity;
import com.apipulse.api_pulse_app.payment.PaymentRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetMetricsTool {

    private final PaymentRepository paymentRepository;

    @Tool("현재 결제 시스템의 실시간 지표를 조회합니다. 총 건수, 성공/실패율을 반환합니다.")
    public String getCurrentMetrics() {
        log.info("[Tool] GetMetrics 실행");

        List<PaymentEntity> recent = paymentRepository.findTop10ByOrderByCreatedAtDesc();
        long approved = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.APPROVED)
                .count();
        long failed = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.FAILED)
                .count();
        long cancelled = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.CANCELLED)
                .count();

        return String.format("""
                === 실시간 결제 지표 (최근 10건) ===
                승인: %d건
                실패: %d건
                취소: %d건
                실패율: %.1f%%
                """, approved, failed, cancelled,
                recent.size() > 0 ? (double) failed / recent.size() * 100 : 0);
    }
}