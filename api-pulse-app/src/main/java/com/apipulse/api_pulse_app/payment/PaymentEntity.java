package com.apipulse.api_pulse_app.payment;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardNumber;      // 카드번호 (마스킹)
    private Long amount;            // 결제 금액
    private String merchantName;    // 가맹점명

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;   // APPROVED / CANCELLED / FAILED

    private String failReason;      // 실패 사유
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        APPROVED, CANCELLED, FAILED
    }
}