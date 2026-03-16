package com.apipulse.api_pulse_app.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/scenario")
public class ScenarioController {

    // 시나리오 1 - 500 에러 폭증
    @PostMapping("/error")
    public ResponseEntity<Map<String, String>> triggerError() {
        log.error("의도적 에러 발생 - 시나리오 트리거");
        return ResponseEntity.internalServerError()
                .body(Map.of("error", "Internal Server Error"));
    }

    // 시나리오 2 - 응답 지연
    @PostMapping("/slow")
    public ResponseEntity<Map<String, String>> triggerSlow() throws InterruptedException {
        log.warn("응답 지연 시나리오 트리거 - 3초 대기");
        Thread.sleep(3000);
        return ResponseEntity.ok(Map.of("message", "지연 후 응답"));
    }

    // 시나리오 초기화 - 정상으로 복귀 확인용
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        log.info("시나리오 초기화 - 정상 상태 복귀");
        return ResponseEntity.ok(Map.of("message", "정상 상태로 복귀"));
    }
}