package com.apipulse.api_pulse_app.monitoring;

import com.apipulse.api_pulse_app.payment.PaymentEntity;
import com.apipulse.api_pulse_app.payment.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final PaymentRepository paymentRepository;

    public MonitoringSummaryDto getCurrentSummary() {
        List<PaymentEntity> recent = paymentRepository.findTop20ByOrderByCreatedAtDesc();

        if (recent.isEmpty()) {
            return MonitoringSummaryDto.builder()
                    .totalCount(0)
                    .approvedCount(0)
                    .failedCount(0)
                    .errorRate(0.0)
                    .topFailReason("-")
                    .status(DetectionStatus.NORMAL)
                    .detected(false)
                    .message("데이터 없음")
                    .build();
        }

        long totalCount = recent.size();

        long approvedCount = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.APPROVED)
                .count();

        long failedCount = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.FAILED)
                .count();

        double errorRate = totalCount == 0 ? 0.0 : (double) failedCount / totalCount * 100.0;
        errorRate = Math.min(errorRate, 100.0);

        String topFailReason = extractTopFailReason(recent);
        DetectionStatus status = resolveStatus(errorRate);
        boolean detected = status != DetectionStatus.NORMAL;
        String message = buildMessage(errorRate, status);

        return MonitoringSummaryDto.builder()
                .totalCount(totalCount)
                .approvedCount(approvedCount)
                .failedCount(failedCount)
                .errorRate(errorRate)
                .topFailReason(topFailReason)
                .status(status)
                .detected(detected)
                .message(message)
                .build();
    }

    private DetectionStatus resolveStatus(double errorRate) {
        if (errorRate >= 80.0) {
            return DetectionStatus.CRITICAL;
        }
        if (errorRate >= 50.0) {
            return DetectionStatus.WARNING;
        }
        return DetectionStatus.NORMAL;
    }

    private String buildMessage(double errorRate, DetectionStatus status) {
        return switch (status) {
            case CRITICAL -> String.format("실패율 %.1f%%로 치명적 장애 상태입니다.", errorRate);
            case WARNING -> String.format("실패율 %.1f%%로 장애 의심 상태입니다.", errorRate);
            case NORMAL -> String.format("정상 범위입니다. 현재 실패율 %.1f%%입니다.", errorRate);
        };
    }

    private String extractTopFailReason(List<PaymentEntity> recent) {
        Map<String, Long> failReasonCount = recent.stream()
                .filter(p -> p.getStatus() == PaymentEntity.PaymentStatus.FAILED)
                .map(p -> {
                    String reason = p.getFailReason();
                    return (reason == null || reason.isBlank()) ? "UNKNOWN" : reason;
                })
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return failReasonCount.entrySet().stream()
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("-");
    }
}