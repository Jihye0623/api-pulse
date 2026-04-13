# FinGuard
> AI 기본법 시대의 금융 AI 운영 표준 — 설명 가능하고 통제 가능한 장애 자동 대응 Agent 시스템

**"무슨 모델을 썼나"가 아니라 "어떻게 운영했나"를 백엔드 코드로 증명하는 시스템**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-007396?style=flat&logo=java&logoColor=white)](https://www.oracle.com/java/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35.0-FF6B35?style=flat)](https://docs.langchain4j.dev)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.12-005571?style=flat&logo=elasticsearch&logoColor=white)](https://elastic.co)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://postgresql.org)

---

## 프로젝트 개요

2026년 1월 AI 기본법 시행 이후, 금융권 AI는 단순 도입을 넘어 **운영 방식 자체를 법적으로 증명**해야 하는 시대가 되었습니다.

AI 기본법 제34조는 고영향 AI 사업자에게 5가지 책무를 요구합니다. FinGuard는 이 다섯 가지를 **선언이 아닌 동작하는 코드**로 구현한 장애 자동 대응 시스템입니다.

| AI 기본법 제34조 | 의미 | FinGuard 구현 |
|---|---|---|
| 위험관리방안 수립 | 장애 사전 탐지 | ELK 실시간 모니터링 + Z-score/룰 Hybrid 탐지 |
| 설명방안 수립·시행 | 판단 근거 제시 | AgentLog — 입력값·사용 툴·판단 결과·응답시간 저장 |
| 이용자 보호 방안 | 즉각 알림·복구 | Streamlit 알림 + 자동 복구 + 실패 결제 재시도 |
| 사람의 관리·감독 | 개입 가능한 구조 | 에스컬레이션 알림 + 수동 Agent 실행 API |
| 문서 작성·보관 | 이력 저장 | PostgreSQL 영구 저장 |

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Backend | Spring Boot 3.3, Java 21 |
| AI Agent | LangChain4j 0.35.0, GPT-4o-mini (ReAct 패턴) |
| 로그 수집 | Logstash 8.12 |
| 검색·탐지 | Elasticsearch 8.12 |
| 시각화 | Kibana 8.12, Streamlit |
| DB | PostgreSQL 16 |
| 인프라 | Docker Compose |

---

## 시스템 아키텍처

```
Streamlit Dashboard
      ↕ REST API
Spring Boot (port 8080)
  │
  ├─ JSON 로그 → Logstash → Elasticsearch → Kibana
  │                              │
  │                         이상 감지 (30초 주기)
  │                              │
  └─ AI Agent (LangChain4j ReAct)
       ├─ GetMetrics      → 실시간 지표 조회
       ├─ SearchLogs      → ES 에러 로그 검색
       ├─ AnalyzePattern  → 실패 패턴 분석
       ├─ RestartService  → 오류 모드 해제 + 실패 결제 재시도
       ├─ SendAlert       → 알림 DB 저장
       └─ GenerateReport  → 장애 리포트 생성
            │
       PostgreSQL (판단 이력 영구 저장)
```

---

## 프로젝트 구조

```
finguard/
├── api-pulse-app/                  # Spring Boot 백엔드
│   └── src/main/java/com/apipulse/api_pulse_app/
│       ├── agent/                  # AI Agent
│       │   ├── FinGuardAgent.java          # GPT-4o-mini 인터페이스
│       │   ├── AgentExecutionService.java  # Agent 실행 + 로그 저장
│       │   ├── EscalationPolicyService.java# 에스컬레이션 판단
│       │   └── tools/                      # Tool 6개
│       ├── monitoring/             # 이상 탐지
│       │   ├── AnomalyDetectionService.java# Z-score Hybrid 탐지
│       │   └── MonitoringScheduler.java    # 30초 스케줄러
│       ├── alert/                  # 알림 관리
│       ├── payment/                # 결제 도메인
│       └── filter/
│           └── LoggingFilter.java  # MDC 기반 자동 로깅
├── elk/
│   └── logstash/pipeline/
│       └── api-pulse.conf          # is_error 분류 파이프라인
├── streamlit/                      # 대시보드
│   ├── app.py                      # 홈 — 알림배너 / 지표카드
│   └── pages/
│       ├── 1_모니터링.py
│       └── 2_챗봇.py
├── scenario/                       # 시나리오 스크립트
└── docker-compose.yml
```

---

## 핵심 구현

### 1. Z-score + 룰베이스 Hybrid 이상 탐지

단순 룰베이스만 사용하면 트래픽이 적은 시간대에 1건 실패만으로도 에러율 100% 오탐이 발생합니다. 데이터 분포를 고려한 Z-score를 결합해 맥락 기반 탐지를 구현했습니다.

```java
// 30초마다 ES에서 최근 10분 데이터 집계
double zScore = std < 0.001 ? 0.0 : (current - mean) / std;

boolean statisticalAnomaly = zScore > 2.0;   // 평균 대비 2 표준편차 이탈
boolean criticalByRule = current >= 80.0;     // 절대값 80% 이상

// 상태 기반 구조 — 중복 실행 방지
if (shouldTriggerAgent(currentStatus, nextStatus)) {
    agentController.runAgent("STATISTICAL_ANOMALY", situation);
}
currentStatus = nextStatus;
```

| 상태 | 조건 | 처리 |
|---|---|---|
| NORMAL | z < 2.0 | 대기 |
| WARNING | z > 2.0 | Agent 자율 처리 |
| CRITICAL | 에러율 > 80% | 에스컬레이션 |

### 2. AI Agent ReAct 패턴

SystemMessage에 순서를 고정하지 않고 판단 기준만 제시해 GPT가 상황에 따라 툴을 자율 선택하도록 설계했습니다.

```java
@SystemMessage("""
    당신은 금융 결제 시스템의 AI 장애 대응 에이전트입니다.
    사용 가능한 툴을 활용해 상황을 스스로 판단하고 최적의 순서로 대응하세요.
    툴 실행 결과를 보고 다음 행동을 결정합니다.
    복구가 불가능하다고 판단되면 즉시 에스컬레이션 알림을 발송하세요.
    """)
String analyze(@UserMessage String situation);
```

카드 오류 장애 발생 시 SearchLogs·AnalyzePattern을 생략하고 바로 RestartService를 호출한 실제 로그:
```
Agent 실행 완료 - 12084ms
사용 툴: GetMetrics, RestartService, SendAlert, GenerateReport
```

### 3. 자동복구 — 오류 모드 해제 + 실패 결제 재시도

```java
@Tool("카드 오류로 인한 결제 장애를 자동 복구합니다.")
public String recoverFromCardError() {
    // 1단계: 오류 모드 해제
    paymentService.setCardErrorMode(false);

    // 2단계: 실패 결제 최대 3건 재시도
    PaymentService.RetryResult result = paymentService.retryFailedPayments();

    return String.format("자동복구 완료 - 카드 오류 모드 해제, 재시도: %s", result.message());
}
```

### 4. MDC 기반 전 구간 요청 추적

OncePerRequestFilter로 모든 API 요청에 자동 로깅을 적용합니다. 별도 로깅 코드 없이 전체 요청이 traceId 단위로 추적됩니다.

```java
// 모든 요청에 자동 적용
MDC.put("traceId", UUID.randomUUID().toString().replace("-", "").substring(0, 8));
MDC.put("method", request.getMethod());
MDC.put("uri", request.getRequestURI());
```

---

## 시연 흐름

```
1. docker compose up -d
2. Spring Boot 실행 (port 8080)
3. streamlit run app.py (port 8501)

4. POST /scenario/card-error/on    # 카드 오류 시나리오 ON
5. 30초 대기                        # AnomalyDetectionService 자동 감지
6. Streamlit 알림 배너 확인          # WARN 알림 생성
7. GET /agent/logs                  # Agent 실행 이력 확인
8. GET /payment/history             # 실패 결제 → APPROVED 전환 확인
```

---

## 트러블슈팅

### 1. 룰베이스 단독 탐지의 오탐 문제

**문제** 새벽 시간대에 트래픽 2건 중 1건 실패 시 에러율 50%로 오탐 발생. 데이터 맥락 없이 숫자만 비교하는 방식의 한계.

**해결** Z-score 도입으로 현재 값이 과거 평균에서 얼마나 이탈했는지 기준으로 판단 변경. 절대값 80% 조건을 보완적으로 유지해 Hybrid 구조로 전환. 오탐 대폭 감소 및 트래픽 변화에 유연하게 대응.

---

### 2. Agent 동일 장애에서 반복 실행

**문제** 장애가 지속되는 동안 30초마다 Agent가 계속 실행되어 로그와 알림이 과도하게 쌓임. Agent가 이전 상태를 기억하지 못하는 stateless 구조가 원인.

**해결** NORMAL / WARNING / CRITICAL 상태를 명시적으로 관리하는 state 기반 로직 도입. 이상 진입 시 1회만 실행, 상태 유지 중 재실행 차단, 정상 복구 시 자동 초기화.

```java
private boolean shouldTriggerAgent(DetectionStatus current, DetectionStatus next) {
    return current == DetectionStatus.NORMAL && next != DetectionStatus.NORMAL;
}
```

---

### 3. 에러율 100% 초과 버그

**문제** 에러율이 120%처럼 100%를 초과하는 값 발생. 분자(ES 로그 기준)와 분모(DB 기준)가 서로 다른 데이터 소스를 참조하다 집계 시점 불일치로 발생.

**해결** 단일 데이터 소스 기준으로 통일. 방어 로직 추가로 물리적으로 100% 초과 불가능하게 처리.

```java
double safeErrorVal = Math.min(errorVal, totalVal);
double rate = Math.min(safeErrorVal / totalVal * 100.0, 100.0);
```

---

### 4. Elasticsearch 406 Not Acceptable 오류

**문제** AnomalyDetectionService에서 ES 쿼리 시 406 에러 발생. RestTemplate의 기본 Content-Type이 `text/plain`으로 전송되어 ES가 거부.

**해결** `restTemplate.exchange()`로 변경하고 `Content-Type: application/json` 헤더 명시적 추가.

```java
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
HttpEntity<String> entity = new HttpEntity<>(query, headers);
ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
```

---

### 5. is_error 필드 탐지 실패

**문제** Logstash에서 `statusCode >= 500`일 때만 `is_error: true`로 설정했는데, 결제 실패 응답이 200으로 반환되어 모든 로그가 `is_error: false`로 저장됨. AnomalyDetectionService가 에러를 감지하지 못함.

**해결** Logstash 파이프라인 조건에 `level == "ERROR"` 추가. HTTP 상태코드와 무관하게 애플리케이션 레벨 에러를 정확히 분류.

```ruby
if [level] == "ERROR" or ([statusCode] and [statusCode] >= 500) {
    mutate { add_field => { "is_error" => "true" } }
}
```

---

## 성과

- AI 기본법 제34조 5대 책무를 코드 수준에서 1대1 매핑 구현
- Z-score Hybrid 탐지로 룰베이스 단독 대비 오탐률 감소
- Agent가 상황에 따라 툴을 자율 선택 (ReAct 패턴) — 불필요한 툴 호출 제거
- 장애 감지 → 자동복구 → 실패 결제 재시도까지 전체 파이프라인 자동화
- 모든 AI 판단 이력(입력값·사용 툴·결과·응답시간) PostgreSQL 영구 저장
- MDC 기반 traceId로 전 구간 요청 추적 가능

---

## 실행 방법

### 사전 조건

- Docker Desktop 실행
- Java 21
- Python 3.9+
- OpenAI API Key

### 환경 변수 설정

```bash
# .env
OPENAI_API_KEY=your_openai_api_key
```

### 실행

```bash
# 1. 인프라 실행
docker compose up -d

# 2. Spring Boot 실행
cd api-pulse-app
./gradlew bootRun

# 3. Streamlit 실행
cd streamlit
pip install -r requirements.txt
streamlit run app.py
```

### 접속

| 서비스 | URL |
|---|---|
| Spring Boot API | http://localhost:8080 |
| Streamlit 대시보드 | http://localhost:8501 |
| Kibana | http://localhost:5601 |

---

## API 목록

```
POST /payment/approve          # 결제 승인
POST /payment/cancel/{id}      # 결제 취소
GET  /payment/history          # 결제 내역

POST /scenario/card-error/on   # 카드 오류 시나리오 ON
POST /scenario/card-error/off  # 카드 오류 시나리오 OFF
POST /scenario/payment-timeout/on
POST /scenario/payment-timeout/off
POST /scenario/payment-surge   # 결제 폭증 시나리오

POST /agent/analyze            # Agent 수동 실행
POST /agent/analyze/card-error # 카드 오류 분석
GET  /agent/logs               # Agent 실행 이력

POST /monitor/check            # 모니터링 수동 체크

GET  /alerts/unread            # 미읽 알림 조회
POST /alerts/{id}/read         # 알림 읽음 처리
POST /alerts/read-all          # 전체 읽음 처리
```

---

## 향후 개선 방향

- **아키텍처** — RabbitMQ 기반 비동기 처리 / Agent 서버 분리 (Spring → Flask)
- **AI Agent** — ReAct Thought/Action/Observation 단계별 로그 저장 / ML 기반 이상 탐지 확장
- **관측성** — Redis 캐싱으로 ES 조회 부하 감소 / 알림 정책 고도화

---

## 참고문헌

- 과학기술정보통신부. (2025). AI 기본법 제34조. https://www.law.go.kr
- 황정호. (2026.3.13). AI기본법 톺아보기⑤. 테크42
- He et al. (2017). Drain: An online log parsing approach with fixed depth tree. ICWS
- LangChain4j 0.35.0 Documentation. https://docs.langchain4j.dev
- Elasticsearch 8.12 Documentation. https://elastic.co/docs
