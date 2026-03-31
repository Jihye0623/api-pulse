package com.apipulse.api_pulse_app.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    List<AlertEntity> findByIsReadFalseOrderByCreatedAtDesc();
    List<AlertEntity> findTop20ByOrderByCreatedAtDesc();
}