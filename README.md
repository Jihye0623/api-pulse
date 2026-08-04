# FinGuard
> 실시간 로그 수집 · 이상 탐지 · AI 자동복구 파이프라인

**Spring Boot 애플리케이션의 로그를 ELK로 구조화하고, Z-score + 룰베이스 Hybrid 방식으로 이상을 탐지해 AI Agent가 자율 복구하는 시스템**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-007396?style=flat&logo=java&logoColor=white)](https://www.oracle.com/java/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35.0-FF6B35?style=flat)](https://docs.langchain4j.dev)
[![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.12-005571?style=flat&logo=elasticsearch&logoColor=white)](https://elastic.co)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://postgresql.org)

---

## 프로젝트 개요

금융 결제 API에서 발생하는 장애를 사람 개입 없이 자동으로 감지하고 복구하는 시스템입니다.

로그 수집부터 이상 탐지, AI Agent 자동복구, 판단 이력 저장까지 전체 파이프라인을 구현했습니다.

<img width="1486" height="1306" alt="a1" src="https://github.com/user-attachments/assets/ac3df8ef-cf75-4ff7-b232-5ce24719d90d" />
<img width="1501" height="2173" alt="a2" src="https://github.com/user-attachments/assets/69add852-7735-4850-bfb0-a693ddc41b32" />
<img width="1504" height="1658" alt="screencapture-localhost-8501-2026-04-14-09_45_25" src="https://github.com/user-attachments/assets/9ba819f4-f090-4c04-b0a2-8bd0034058c8" />
<img width="1411" height="1857" alt="screencapture-localhost-8501-Agent-2026-04-14-09_48_36" src="https://github.com/user-attachments/assets/27fc4270-ca3f-4efa-ae29-322922554245" />

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Backend | Spring Boot 3.5.11, Java 21 |
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
  ├─ MDC 기반 JSON 로그
  │       ↓
  │   Logstash (파싱·변환)
  │       ↓
  │   Elasticsearch (인덱싱)
  │       ↓
  │   AnomalyDetectionService (30초 주기 Z-score 탐지)
  │       ↓ 이상 감지 시
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
│       ├── 1_Agent_이력.py
│       ├── 2_모니터링.py
│       └── 3_챗봇.py
├── scenario/                       # 시나리오 스크립트
└── docker-compose.yml
```

---

## 핵심 구현

### 1. MDC 기반 구조화 로그

비정형 로그는 집계도, 이상 탐지도 불가능합니다. `OncePerRequestFilter`로 모든 API 요청에 자동 로깅을 적용해 개발자가 컨트롤러마다 로깅 코드를 작성하지 않아도 전 구간이 traceId 단위로 추적됩니다.

```java
MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8));
MDC.put("method", request.getMethod());
MDC.put("uri", request.getRequestURI());
MDC.put("statusCode", String.valueOf(response.getStatus()));
MDC.put("responseTime", String.valueOf(duration));
```

---

### 2. Logstash 파이프라인 — 로그 구조화 및 분류

Spring Boot가 출력하는 JSON 로그를 Logstash가 파싱·변환해 Elasticsearch에 인덱싱합니다.

HTTP 상태코드가 200이어도 애플리케이션 레벨에서 ERROR가 발생하는 경우를 처리하기 위해 `level == "ERROR"` 조건을 추가해 정확한 에러 분류를 구현했습니다.

```ruby
# level이 ERROR이거나 statusCode가 500 이상이면 is_error: true
if [level] == "ERROR" or ([statusCode] and [statusCode] >= 500) {
    mutate { add_field => { "is_error" => "true" } }
} else {
    mutate { add_field => { "is_error" => "false" } }
}
```

저장 구조:
- 인덱스: `api-pulse-YYYY.MM.dd` (일별)
- 주요 필드: `traceId`, `level`, `is_error`, `statusCode`, `responseTime`, `uri`

---

### 3. Z-score + 룰베이스 Hybrid 이상 탐지

단순 룰베이스만 사용하면 트래픽이 적은 시간대에 1건 실패만으로도 에러율 100% 오탐이 발생합니다. 데이터 분포를 고려한 Z-score를 결합해 맥락 기반 탐지를 구현했습니다.

```java
// ES에서 최근 10분 데이터를 1분 버킷으로 집계
double zScore = std < 0.001 ? 0.0 : (current - mean) / std;

boolean statisticalAnomaly = zScore > 2.0;   // 평균 대비 2 표준편차 이탈
boolean criticalByRule = current >= 80.0;     // 절대값 80% 이상 (OR 구조)

// 상태 기반 구조 — 동일 장애에서 Agent 중복 실행 방지
if (shouldTriggerAgent(currentStatus, nextStatus)) {
    agentController.runAgent("STATISTICAL_ANOMALY", situation);
}
currentStatus = nextStatus;
```

| 상태 | 조건 | 처리 |
|---|---|---|
| NORMAL | z < 2.0, 에러율 정상 | 대기 |
| WARNING | z > 2.0 | Agent 자율 처리 |
| CRITICAL | 에러율 >= 80% | 에스컬레이션 알림 |

---

### 4. AI Agent ReAct 패턴

SystemMessage에 순서를 고정하지 않고 판단 기준만 제시해 GPT가 상황에 따라 툴을 자율 선택하도록 설계했습니다.

```java
@SystemMessage("""
    당신은 금융 결제 시스템의 AI 장애 대응 에이전트입니다.
    
    사용 가능한 툴을 활용해 상황을 스스로 판단하고 최적의 순서로 대응하세요.
    
    판단 기준:
    - 먼저 시스템 상태와 지표를 확인해 장애 원인을 파악하세요.
    - 원인이 파악되면 적절한 복구 툴을 실행하세요.
    - 복구 후 지표를 다시 확인해 정상화 여부를 검증하세요.
    - 자동복구가 불가능하다고 판단되면 즉시 에스컬레이션 알림을 발송하세요.
    - 모든 처리가 완료되면 장애 리포트를 생성하세요.
    
    항상 한국어로 응답하세요.
    각 툴 실행 결과를 바탕으로 다음 행동을 결정하세요.
    """)
String analyze(@UserMessage String situation);
```

카드 오류 장애 발생 시 `SearchLogs`·`AnalyzePattern`을 생략하고 바로 `RestartService`를 호출한 실제 로그 — 순서 고정 없이 GPT가 스스로 불필요한 툴을 건너뛴 ReAct 동작의 증거입니다.

```
Agent 실행 완료 - 12084ms
사용 툴: GetMetrics, RestartService, SendAlert, GenerateReport
```

---

### 5. 자동복구 — 오류 모드 해제 + 실패 결제 재시도

단순히 오류 모드를 끄는 것에서 나아가, 장애 중 발생한 실패 결제를 최대 3건까지 자동으로 재시도합니다.

```java
@Tool("카드 오류로 인한 결제 장애를 자동 복구합니다. 카드 오류 모드를 해제하고 최근 실패한 결제를 재시도합니다.")
public String recoverFromCardError() {
    try {
        paymentService.setCardErrorMode(false);
        PaymentService.RetryResult retryResult = paymentService.retryFailedPayments();
        return String.format("""
            === 카드 오류 자동복구 완료 ===
            1. 카드 오류 모드 해제 완료
            2. 실패 결제 재시도: %s
            상태: 정상화됨
            """, retryResult.message());
    } catch (Exception e) {
        return "자동복구 실패 - 수동 개입 필요: " + e.getMessage();
    }
}
```

---

## 전체 파이프라인 흐름

```
① 카드 오류 ON → 결제 요청 실패
② MDC 필터가 ERROR 레벨 로그 자동 생성
③ Logstash가 is_error: true로 분류 → ES 저장
④ AnomalyDetectionService (30초 주기)
   → ES에서 최근 10분 집계
   → Z-score 계산: 3.16 > 2.0 → CRITICAL 판정
   → 상태 전환 감지 → Agent 트리거
⑤ FinGuardAgent.analyze() 호출
   → GPT가 상황 판단 → 툴 자율 선택
   → RestartService: 오류 모드 해제 + 실패 결제 재시도
   → SendAlert: 에스컬레이션 알림 생성
   → GenerateReport: 장애 리포트 저장
⑥ Streamlit 대시보드: NORMAL 복구 확인
⑦ 사람이 에스컬레이션 알림 읽음 처리
```

---

## 트러블슈팅

### 1. 룰베이스 단독 탐지의 오탐 문제

**문제** 새벽 시간대에 트래픽 2건 중 1건 실패 시 에러율 50%로 오탐 발생. 데이터 맥락 없이 숫자만 비교하는 방식의 한계.

**해결** Z-score 도입으로 현재 값이 과거 평균에서 얼마나 이탈했는지 기준으로 판단 변경. 절대값 80% 조건을 보완적으로 유지해 Hybrid 구조로 전환.

---

### 2. Agent 동일 장애에서 반복 실행

**문제** 장애가 지속되는 동안 30초마다 Agent가 계속 실행되어 로그와 알림이 과도하게 쌓임. Agent가 이전 상태를 기억하지 못하는 stateless 구조가 원인.

**해결** NORMAL / WARNING / CRITICAL 상태를 명시적으로 관리하는 state 기반 로직 도입. 이상 진입 시 1회만 실행, 상태 유지 중 재실행 차단, 정상 복구 시 자동 초기화.

```java
private boolean shouldTriggerAgent(DetectionStatus currentStatus, DetectionStatus nextStatus) {
    return currentStatus == DetectionStatus.NORMAL && nextStatus != DetectionStatus.NORMAL;
}
```

---

### 3. 에러율 100% 초과 버그

**문제** 에러율이 120%처럼 100%를 초과하는 값 발생. 분자(ES 로그 기준)와 분모(DB 기준)가 서로 다른 데이터 소스를 참조하다 집계 시점 불일치로 발생.

**해결** 단일 데이터 소스 기준으로 통일. 방어 로직 추가.

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

**문제** Logstash에서 `statusCode >= 500`일 때만 `is_error: true`로 설정했는데, 결제 실패 응답이 200으로 반환되어 모든 로그가 `is_error: false`로 저장됨.

**해결** Logstash 파이프라인 조건에 `level == "ERROR"` 추가.

```ruby
if [level] == "ERROR" or ([statusCode] and [statusCode] >= 500) {
    mutate { add_field => { "is_error" => "true" } }
}
```

---

## 성과

- MDC 기반 자동 로깅으로 개발자 로깅 코드 없이 전 구간 traceId 추적
- Z-score Hybrid 탐지로 룰베이스 단독 대비 오탐률 감소
- 상태 기반 구조로 동일 장애에서 Agent 중복 실행 방지
- Agent가 상황에 따라 툴 자율 선택 (ReAct 패턴) — 불필요한 툴 호출 제거
- 장애 감지 → 자동복구 → 실패 결제 재시도까지 전체 파이프라인 자동화
- 모든 Agent 판단 이력 (입력값·사용 툴·결과·응답시간) PostgreSQL 영구 저장

---

## 실행 방법

### 사전 조건

- Docker Desktop 실행
- Java 21
- Python 3.9+
- OpenAI API Key

### 환경 변수 설정

```yaml
# api-pulse-app/src/main/resources/application.yml
langchain4j:
  open-ai:
    chat-model:
      api-key: your_openai_api_key  # 또는 환경 변수(OPENAI_API_KEY) 설정
```

### 실행

```bash
# 1. 인프라 실행 (Spring Boot를 로컬에서 직접 띄울 경우)
docker compose up -d postgres elasticsearch logstash kibana

# 또는 전체를 컨테이너로 실행할 경우 (컨테이너 실행 전에 ./gradlew bootJar 빌드 필요)
# docker compose up -d

# 2. Spring Boot 로컬 실행
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
POST /payment/approve              # 결제 승인
GET  /payment/history              # 결제 내역

POST /scenario/card-error/on       # 카드 오류 시나리오 ON
POST /scenario/card-error/off      # 카드 오류 시나리오 OFF
POST /scenario/payment-timeout/on
POST /scenario/payment-timeout/off

POST /agent/analyze/card-error     # Agent 수동 실행
GET  /agent/logs                   # Agent 실행 이력

POST /monitor/check                # 모니터링 수동 체크

GET  /alerts/unread                # 미읽 알림 조회
POST /alerts/read-all              # 전체 읽음 처리
```

---

## 향후 개선 방향

- **아키텍처** — RabbitMQ 기반 비동기 처리 / Agent 서버 분리 (Spring → Flask)
- **AI Agent** — ReAct Thought/Action/Observation 단계별 로그 저장 / ML 기반 이상 탐지 확장
- **관측성** — Redis 캐싱으로 ES 조회 부하 감소 / 알림 정책 고도화

---

## 참고문헌

- He et al. (2017). Drain: An online log parsing approach with fixed depth tree. ICWS. https://doi.org/10.1109/ICWS.2017.13
- Goldstein & Uchida. (2020). Anomaly Detection: A Survey. arXiv. https://arxiv.org/abs/2004.00433
- LangChain4j 0.35.0 Documentation. https://docs.langchain4j.dev
- Elasticsearch 8.12 Documentation. https://elastic.co/docs
- 과학기술정보통신부. (2025). AI 기본법 제34조. https://www.law.go.kr
