package com.apipulse.api_pulse_app.alert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertRepository alertRepository;

    // 읽지 않은 알림 조회 (Streamlit이 폴링)
    @GetMapping("/unread")
    public ResponseEntity<List<AlertEntity>> getUnread() {
        return ResponseEntity.ok(
                alertRepository.findByIsReadFalseOrderByCreatedAtDesc()
        );
    }

    // 전체 알림 조회
    @GetMapping
    public ResponseEntity<List<AlertEntity>> getAll() {
        return ResponseEntity.ok(
                alertRepository.findTop20ByOrderByCreatedAtDesc()
        );
    }

    // 알림 읽음 처리 (Streamlit에서 "확인" 버튼 클릭 시)
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id) {
        alertRepository.findById(id).ifPresent(alert -> {
            alert.setRead(true);
            alertRepository.save(alert);
            log.info("알림 읽음 처리 - id: {}", id);
        });
        return ResponseEntity.ok(Map.of("message", "읽음 처리 완료"));
    }

    // 전체 읽음 처리
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        List<AlertEntity> unread = alertRepository.findByIsReadFalseOrderByCreatedAtDesc();
        unread.forEach(alert -> alert.setRead(true));
        alertRepository.saveAll(unread);
        log.info("전체 알림 읽음 처리 - {}건", unread.size());
        return ResponseEntity.ok(Map.of("message", "전체 읽음 처리 완료"));
    }
}