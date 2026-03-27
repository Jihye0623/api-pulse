package com.apipulse.api_pulse_app.agent;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agent_logs")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String triggerType;

    @Column(columnDefinition = "TEXT")
    private String situation;

    @Column(columnDefinition = "TEXT")
    private String agentResult;

    private String toolsUsed;
    private Long responseTimeMs;
    private LocalDateTime createdAt;
}