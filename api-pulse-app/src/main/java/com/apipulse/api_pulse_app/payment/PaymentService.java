package com.apipulse.api_pulse_app.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final Random random = new Random();

    // 시나리오 제어 플래그
    private volatile boolean timeoutMode = false;
    private volatile boolean cardErrorMode = false;

    public PaymentEntity approve(String cardNumber, Long amount, String merchantName)
            throws InterruptedException {

        log.info("결제 승인 요청 - 카드: {}, 금액: {}, 가맹점: {}",
                maskCard(cardNumber), amount, merchantName);

        // 타임아웃 시나리오
        if (timeoutMode) {
            log.warn("결제 처리 지연 발생 - PG사 응답 없음");
            Thread.sleep(5000);
        }

        // 카드 오류 시나리오 or 랜덤 실패 (5%)
        boolean failed = cardErrorMode || random.nextInt(100) < 5;

        PaymentEntity payment = PaymentEntity.builder()
                .cardNumber(maskCard(cardNumber))
                .amount(amount)
                .merchantName(merchantName)
                .status(failed
                        ? PaymentEntity.PaymentStatus.FAILED
                        : PaymentEntity.PaymentStatus.APPROVED)
                .failReason(failed ? (cardErrorMode ? "카드 오류" : "일시적 오류") : null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        if (failed) {
            log.error("결제 승인 실패 - 사유: {}", payment.getFailReason());
        } else {
            log.info("결제 승인 완료 - ID: {}", payment.getId());
        }

        return payment;
    }

    public PaymentEntity cancel(Long paymentId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역 없음: " + paymentId));

        payment.setStatus(PaymentEntity.PaymentStatus.CANCELLED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("결제 취소 완료 - ID: {}", paymentId);
        return payment;
    }

    public PaymentEntity getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 내역 없음: " + paymentId));
    }

    public List<PaymentEntity> getHistory() {
        return paymentRepository.findTop10ByOrderByCreatedAtDesc();
    }

    // 시나리오 제어
    public void setTimeoutMode(boolean enabled) {
        this.timeoutMode = enabled;
        log.warn("타임아웃 모드: {}", enabled ? "ON" : "OFF");
    }

    public void setCardErrorMode(boolean enabled) {
        this.cardErrorMode = enabled;
        log.warn("카드 오류 모드: {}", enabled ? "ON" : "OFF");
    }

    private String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}