package com.apipulse.api_pulse_app.alert;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String level;        // INFO / WARN / ESCALATION

    @Column(columnDefinition = "TEXT")
    private String message;

    private boolean isRead;
    private LocalDateTime createdAt;
}