package com.apipulse.api_pulse_app.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class GenerateReportTool {

    @Tool("장애 분석 리포트를 생성합니다. 원인, 조치 내역, 재발방지책을 포함합니다.")
    public String generateReport(String cause, String action, String prevention) {
        log.info("[Tool] GenerateReport 실행");

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return String.format("""
                ========================================
                📋 FinGuard 장애 분석 리포트
                ========================================
                발생 시각 : %s
                
                🔍 장애 원인
                %s
                
                🔧 조치 내역
                %s
                
                ✅ 재발 방지책
                %s
                ========================================
                """, timestamp, cause, action, prevention);
    }
}