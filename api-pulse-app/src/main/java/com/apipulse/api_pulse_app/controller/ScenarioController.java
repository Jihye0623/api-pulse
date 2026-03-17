package com.apipulse.api_pulse_app.controller;

import com.apipulse.api_pulse_app.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/scenario")
@RequiredArgsConstructor
public class ScenarioController {

    private final PaymentService paymentService;

    // 시나리오 1 - 500 에러 폭증
    @PostMapping("/error")
    public ResponseEntity<Map<String, String>> triggerError() {
        log.error("의도적 에러 발생 - 시나리오 트리거");
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal Server Error"));
    }

    // 시나리오 2 - 응답 지연
    @PostMapping("/slow")
    public ResponseEntity<Map<String, String>> triggerSlow() throws InterruptedException {
        log.warn("응답 지연 시나리오 트리거 - 3초 대기");
        Thread.sleep(3000);
        return ResponseEntity.ok(Map.of("message", "지연 후 응답"));
    }

    // 시나리오 초기화 - 정상으로 복귀 확인용
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        log.info("시나리오 초기화 - 정상 상태 복귀");
        return ResponseEntity.ok(Map.of("message", "정상 상태로 복귀"));
    }

    // 결제 폭증 시나리오
    @PostMapping("/payment-surge")
    public ResponseEntity<Map<String, String>> paymentSurge() throws InterruptedException {
        log.warn("결제 폭증 시나리오 시작");
        for (int i = 0; i < 50; i++) {
            paymentService.approve("1234567890123456",
                    (long)(Math.random() * 500000), "테스트가맹점");
        }
        return ResponseEntity.ok(Map.of("message", "결제 폭증 시나리오 완료"));
    }

    // 타임아웃 시나리오 ON
    @PostMapping("/payment-timeout/on")
    public ResponseEntity<Map<String, String>> timeoutOn() {
        paymentService.setTimeoutMode(true);
        return ResponseEntity.ok(Map.of("message", "타임아웃 모드 ON"));
    }

    // 타임아웃 시나리오 OFF
    @PostMapping("/payment-timeout/off")
    public ResponseEntity<Map<String, String>> timeoutOff() {
        paymentService.setTimeoutMode(false);
        return ResponseEntity.ok(Map.of("message", "타임아웃 모드 OFF"));
    }

    // 카드 오류 시나리오 ON
    @PostMapping("/card-error/on")
    public ResponseEntity<Map<String, String>> cardErrorOn() {
        paymentService.setCardErrorMode(true);
        return ResponseEntity.ok(Map.of("message", "카드 오류 모드 ON"));
    }

    // 카드 오류 시나리오 OFF
    @PostMapping("/card-error/off")
    public ResponseEntity<Map<String, String>> cardErrorOff() {
        paymentService.setCardErrorMode(false);
        return ResponseEntity.ok(Map.of("message", "카드 오류 모드 OFF"));
    }
}