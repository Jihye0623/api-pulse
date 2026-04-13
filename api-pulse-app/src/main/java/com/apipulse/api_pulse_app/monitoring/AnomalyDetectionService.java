package com.apipulse.api_pulse_app.monitoring;

import com.apipulse.api_pulse_app.agent.AgentController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final AgentController agentController;
    private final RestTemplate restTemplate;

    @Value("${anomaly.es-url:http://localhost:9200}")
    private String esUrl;

    @Value("${anomaly.detection.z-score-threshold:2.0}")
    private double zScoreThreshold;

    // 운영 상태
    private DetectionStatus currentStatus = DetectionStatus.NORMAL;

    @Scheduled(fixedRateString = "${anomaly.detection.interval-ms:30000}")
    public void detect() {
        try {
            List<Double> errorRates = queryErrorRateSeries();

            if (errorRates.size() < 3) {
                log.debug("[AnomalyDetection] 데이터 부족 - 건너뜀 ({}건)", errorRates.size());
                return;
            }

            double mean = calculateMean(errorRates);
            double std = calculateStd(errorRates, mean);
            double current = errorRates.get(errorRates.size() - 1);
            double zScore = std < 0.001 ? 0.0 : (current - mean) / std;

            // 혼합 판정
            boolean statisticalAnomaly = zScore > zScoreThreshold;
            boolean criticalByRule = current >= 80.0;

            DetectionStatus nextStatus;
            if (criticalByRule) {
                nextStatus = DetectionStatus.CRITICAL;
            } else if (statisticalAnomaly) {
                nextStatus = DetectionStatus.WARNING;
            } else {
                nextStatus = DetectionStatus.NORMAL;
            }

            DetectionResult result = DetectionResult.builder()
                    .currentErrorRate(current)
                    .meanErrorRate(mean)
                    .stdDev(std)
                    .zScore(zScore)
                    .statisticalAnomaly(statisticalAnomaly)
                    .criticalByRule(criticalByRule)
                    .status(nextStatus)
                    .build();

            log.info(
                    "[AnomalyDetection] errorRate={}%, mean={}%, std={}%, zScore={}, statisticalAnomaly={}, criticalByRule={}, status={}",
                    String.format("%.1f", result.getCurrentErrorRate()),
                    String.format("%.1f", result.getMeanErrorRate()),
                    String.format("%.1f", result.getStdDev()),
                    String.format("%.2f", result.getZScore()),
                    result.isStatisticalAnomaly(),
                    result.isCriticalByRule(),
                    result.getStatus()
            );

            if (currentStatus != nextStatus) {
                log.info(
                        "[AnomalyDetection] 상태 전이 발생: {} -> {} (errorRate={}%, zScore={})",
                        currentStatus,
                        nextStatus,
                        String.format("%.1f", current),
                        String.format("%.2f", zScore)
                );
            }

            // 정상 -> 이상 상태로 바뀌는 순간에만 Agent 실행
            if (shouldTriggerAgent(currentStatus, nextStatus)) {
                log.warn(
                        "[AnomalyDetection] 이상 감지! status={}, errorRate={}%, zScore={}",
                        nextStatus,
                        String.format("%.1f", current),
                        String.format("%.2f", zScore)
                );

                agentController.runAgent(
                        "STATISTICAL_ANOMALY",
                        String.format(
                                "통계적 이상 감지. 현재 에러율 %.1f%%, 최근 평균 %.1f%%, z-score %.2f. " +
                                        "상태는 %s이며, 정상 패턴에서 이탈했습니다. 원인을 분석하고 조치해주세요.",
                                current, mean, zScore, nextStatus.name()
                        )
                );
            }

            currentStatus = nextStatus;

        } catch (Exception e) {
            log.debug("[AnomalyDetection] ES 연결 없음 - 건너뜀: {}", e.getMessage());
        }
    }

    private boolean shouldTriggerAgent(DetectionStatus currentStatus, DetectionStatus nextStatus) {
        return currentStatus == DetectionStatus.NORMAL && nextStatus != DetectionStatus.NORMAL;
    }

    @SuppressWarnings("unchecked")
    private List<Double> queryErrorRateSeries() {
        String query = """
            {
              "size": 0,
              "query": {
                "range": {
                  "@timestamp": { "gte": "now-10m" }
                }
              },
              "aggs": {
                "by_minute": {
                  "date_histogram": {
                    "field": "@timestamp",
                    "fixed_interval": "1m"
                  },
                  "aggs": {
                    "total": { "value_count": { "field": "statusCode" } },
                    "errors": {
                      "filter": { "term": { "is_error": "true" } }
                    }
                  }
                }
              }
            }
            """;

        Map<String, Object> response = restTemplate.postForObject(
                esUrl + "/finguard-*/_search",
                query,
                Map.class
        );

        List<Double> errorRates = new ArrayList<>();

        if (response == null) return errorRates;

        Map<String, Object> aggs = (Map<String, Object>) response.get("aggregations");
        if (aggs == null) return errorRates;

        Map<String, Object> byMinute = (Map<String, Object>) aggs.get("by_minute");
        if (byMinute == null) return errorRates;

        List<Map<String, Object>> buckets = (List<Map<String, Object>>) byMinute.get("buckets");
        if (buckets == null) return errorRates;

        for (Map<String, Object> bucket : buckets) {
            Map<String, Object> total = (Map<String, Object>) bucket.get("total");
            Map<String, Object> errors = (Map<String, Object>) bucket.get("errors");

            if (total == null || errors == null) {
                continue;
            }

            Number totalCount = (Number) total.get("value");
            Number errorCount = (Number) errors.get("doc_count");

            if (totalCount == null || totalCount.doubleValue() <= 0) {
                errorRates.add(0.0);
            } else {
                double totalVal = totalCount.doubleValue();
                double errorVal = errorCount == null ? 0.0 : errorCount.doubleValue();

                // 방어 로직: errorCount가 totalCount보다 큰 비정상 상황 차단
                double safeErrorVal = Math.min(errorVal, totalVal);

                double rate = safeErrorVal / totalVal * 100.0;
                rate = Math.min(rate, 100.0);

                errorRates.add(rate);
            }
        }

        return errorRates;
    }

    private double calculateMean(List<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double calculateStd(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}