package com.apipulse.api_pulse_app.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class SendAlertTool {

    @Value("${slack.webhook-url:}")
    private String slackWebhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Tool("Slack으로 장애 알림을 발송합니다. 심각한 장애 발생 시 사용하세요.")
    public String sendSlackAlert(String message) {
        log.warn("[Tool] SendAlert - Slack 알림 발송: {}", message);

        if (slackWebhookUrl == null || slackWebhookUrl.isEmpty()) {
            log.warn("[Tool] Slack Webhook URL 미설정 - 로그로 대체");
            return "⚠️ [장애 알림] " + message + " (Slack 미설정으로 로그 출력)";
        }

        try {
            restTemplate.postForObject(
                    slackWebhookUrl,
                    Map.of("text", "🚨 [FinGuard 장애 알림]\n" + message),
                    String.class
            );
            return "Slack 알림 발송 완료: " + message;
        } catch (Exception e) {
            log.error("[Tool] Slack 발송 실패: {}", e.getMessage());
            return "Slack 발송 실패: " + e.getMessage();
        }
    }
}