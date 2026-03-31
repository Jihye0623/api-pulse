package com.apipulse.api_pulse_app.agent.tools;

import com.apipulse.api_pulse_app.alert.AlertEntity;
import com.apipulse.api_pulse_app.alert.AlertRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendAlertTool {

    private final AlertRepository alertRepository;

    @Tool("Streamlit 대시보드로 장애 알림을 발송합니다. 심각한 장애 발생 시 사용하세요.")
    public String sendSlackAlert(String message) {
        log.warn("[Tool] SendAlert - 알림 발송: {}", message);

        String level = message.contains("에스컬레이션") ? "ESCALATION"
                : message.contains("오류") || message.contains("실패") ? "WARN"
                : "INFO";

        alertRepository.save(AlertEntity.builder()
                .level(level)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build());

        log.warn("[Tool] 알림 DB 저장 완료 - level: {}, message: {}", level, message);
        return "알림 발송 완료 (Streamlit 대시보드): " + message;
    }
}