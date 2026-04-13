package com.apipulse.api_pulse_app.agent.tools;

import com.apipulse.api_pulse_app.payment.PaymentEntity;
import com.apipulse.api_pulse_app.payment.PaymentRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzePatternTool {

    private final PaymentRepository paymentRepository;

    @Tool("최근 결제 내역에서 실패 패턴을 분석합니다.")
    public String analyzeFailurePattern() {
        log.info("[Tool] AnalyzePattern 실행");

        List<PaymentEntity> allPayments =
                paymentRepository.findTop10ByOrderByCreatedAtDesc();

        // ← 이 부분이 핵심: 전체 조회 대신 위 리스트에서 필터링
        List<PaymentEntity> failed = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.FAILED)
                .collect(java.util.stream.Collectors.toList());

        long total = allPayments.size();
        long failCount = failed.size();
        double errorRate = total > 0 ? (double) failCount / total * 100 : 0;

        Map<String, Long> failReasonMap = failed.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getFailReason() != null ? p.getFailReason() : "알 수 없음",
                        Collectors.counting()
                ));

        return String.format("""
            
            === 결제 패턴 분석 결과 ===
            전체 결제: %d건
            실패 건수: %d건
            에러율: %.1f%%
            실패 사유 분포: %s
            """, total, failCount, errorRate, failReasonMap);
    }
}