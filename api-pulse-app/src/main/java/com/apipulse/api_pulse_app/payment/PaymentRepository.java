package com.apipulse.api_pulse_app.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findTop10ByOrderByCreatedAtDesc();
    List<PaymentEntity> findByStatus(PaymentEntity.PaymentStatus status);
}