package com.apipulse.api_pulse_app.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AgentLogRepository extends JpaRepository<AgentLogEntity, Long> {
    List<AgentLogEntity> findTop20ByOrderByCreatedAtDesc();
    List<AgentLogEntity> findByTriggerType(String triggerType);
}