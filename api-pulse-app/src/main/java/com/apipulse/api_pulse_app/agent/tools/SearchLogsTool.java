package com.apipulse.api_pulse_app.agent.tools;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
public class SearchLogsTool {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String ES_URL = "http://localhost:9200";

    @Tool("Elasticsearch에서 최근 에러 로그를 검색합니다. 장애 원인 파악에 사용하세요.")
    public String searchErrorLogs(String keyword) {
        log.info("[Tool] SearchLogs 실행 - keyword: {}", keyword);
        try {
            String query = """
                {
                  "query": {
                    "bool": {
                      "must": [
                        {"term": {"is_error": "true"}},
                        {"match": {"message": "%s"}}
                      ]
                    }
                  },
                  "sort": [{"@timestamp": {"order": "desc"}}],
                  "size": 10
                }
                """.formatted(keyword);

            var response = restTemplate.postForObject(
                    ES_URL + "/finguard-*/_search",
                    Map.of("source", query),
                    Map.class
            );

            return response != null ? response.toString() : "로그 없음";
        } catch (Exception e) {
            log.error("[Tool] SearchLogs 실패: {}", e.getMessage());
            return "로그 검색 실패: " + e.getMessage();
        }
    }

    @Tool("최근 N분간의 전체 에러 로그를 가져옵니다.")
    public String getRecentErrors(int minutes) {
        log.info("[Tool] 최근 {}분 에러 로그 조회", minutes);
        try {
            String query = """
                {
                  "query": {
                    "bool": {
                      "must": [
                        {"term": {"is_error": "true"}},
                        {"range": {"@timestamp": {"gte": "now-%dm"}}}
                      ]
                    }
                  },
                  "size": 20
                }
                """.formatted(minutes);

            var response = restTemplate.postForObject(
                    ES_URL + "/finguard-*/_search",
                    Map.of("source", query),
                    Map.class
            );

            return response != null ? response.toString() : "에러 없음";
        } catch (Exception e) {
            return "조회 실패: " + e.getMessage();
        }
    }
}