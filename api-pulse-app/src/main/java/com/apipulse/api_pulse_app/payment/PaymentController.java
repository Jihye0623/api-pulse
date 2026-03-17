package com.apipulse.api_pulse_app.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 승인
    @PostMapping("/approve")
    public ResponseEntity<PaymentEntity> approve(@RequestBody Map<String, Object> req)
            throws InterruptedException {
        String cardNumber = (String) req.get("cardNumber");
        Long amount = Long.valueOf(req.get("amount").toString());
        String merchantName = (String) req.get("merchantName");

        return ResponseEntity.ok(paymentService.approve(cardNumber, amount, merchantName));
    }

    // 결제 취소
    @PostMapping("/cancel/{id}")
    public ResponseEntity<PaymentEntity> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.cancel(id));
    }

    // 결제 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<PaymentEntity> getPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPayment(id));
    }

    // 최근 결제 내역
    @GetMapping("/history")
    public ResponseEntity<List<PaymentEntity>> getHistory() {
        return ResponseEntity.ok(paymentService.getHistory());
    }
}