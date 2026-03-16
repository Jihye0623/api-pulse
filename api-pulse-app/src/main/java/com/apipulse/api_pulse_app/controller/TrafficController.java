package com.apipulse.api_pulse_app.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
public class TrafficController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "정상 응답"
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUsers() {
        return ResponseEntity.ok(Map.of(
                "count", 42,
                "data", "정상 유저 목록"
        ));
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getOrders() {
        return ResponseEntity.ok(Map.of(
                "count", 10,
                "data", "정상 주문 목록"
        ));
    }
}