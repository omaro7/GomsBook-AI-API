# GomsBook-AI-API

GomsBook AI-Agent를 외부 UI 및 인프라와 연결하기 위한 Spring Boot 기반
API Gateway입니다.

## 1. 프로젝트 목적

`GomsBook-AI-API`는 GomsBookEditor 및 GomsBook AI-Agent와 외부 서비스
사이의 API 경계를 담당합니다.

-   REST API 제공
-   SSE(Server-Sent Events) 기반 Agent 실행 상태 스트리밍
-   HITL(Human-in-the-Loop) 승인 API
-   EPUB 관련 API 제공
-   향후 RAG, Kafka, Qdrant, PostgreSQL, OpenTelemetry 연계
-   Nginx Web Chat UI 연계

## 2. 개발 환경

  항목          설정
  ------------- ------------------------------
  IDE           Spring Tool Suite 4
  Java          21
  Spring Boot   4.1.1
  Build         Maven
  Packaging     JAR
  개발 포트     5001
  Package       `kr.co.goms.gomsbook.ai.api`

## 3. 주요 Dependencies

현재 초기 구성:

-   Spring Web MVC
-   Spring Boot Actuator
-   Validation
-   SpringDoc OpenAPI
-   Lombok

PostgreSQL, Spring Data JPA, Kafka 등은 실제 기능 구현 단계에서
추가합니다.

## 4. 프로젝트 구조

``` text
GomsBook-AI-API
├─ pom.xml
├─ src
│  ├─ main
│  │  ├─ java
│  │  │  └─ kr.co.goms.gomsbook.ai.api
│  │  │     ├─ GomsBookAiApiApplication.java
│  │  │     ├─ agent
│  │  │     ├─ epub
│  │  │     ├─ approval
│  │  │     └─ common
│  │  └─ resources
│  │     └─ application.properties
│  └─ test
└─ README.md
```

## 5. 현재 설정

``` properties
spring.application.name=GomsBook-AI-API
server.port=5001

management.endpoints.web.exposure.include=health,info,mappings
management.endpoint.health.show-details=always
```

## 6. 현재 확인된 API

``` text
GET http://localhost:5001/actuator/health
GET http://localhost:5001/api/epub
GET http://localhost:5001/actuator/mappings
```

Health, EPUB API, MVC Mapping이 정상 동작함을 확인했습니다.

## 7. 목표 아키텍처

``` text
Web Chat / GomsBookEditor
          │
          │ REST
          ▼
      Nginx :2626
          │
          ▼
   GomsBook-AI-API
      Spring Boot
          │
          ├─ REST
          ├─ SSE
          ├─ HITL
          └─ Agent Gateway
          │
          ▼
   GomsBook-AI-Agent
          │
          ├─ Tool Calling
          ├─ RAG
          ├─ EPUB
          └─ LLM
```

향후 인프라:

``` text
GomsBook-AI-API
├─ PostgreSQL    영속 데이터 / 메타데이터
├─ Qdrant        Embedding / Vector Search
├─ Kafka         Agent Event Streaming
└─ OpenTelemetry Trace / Metrics / Logs
```

## 8. REST + SSE

사용자 명령은 REST API로 전달하고 Agent 실행 진행 상황은 SSE로
전달합니다.

``` text
POST /api/agent/run
        ↓
runId
        ↓
GET /api/agent/runs/{runId}/events
        ↓
AGENT_STARTED
RAG_CONTEXT
TOOL_STARTED
TOOL_COMPLETED
ASSISTANT_MESSAGE
APPROVAL_REQUIRED
AGENT_COMPLETED / AGENT_FAILED
```

SSE(Server-Sent Events)는 서버에서 클라이언트 방향으로 지속적으로
이벤트를 전송하는 HTTP 기반 기술입니다.

## 9. Agent API 예정 구조

``` text
kr.co.goms.gomsbook.ai.api.agent
├─ AgentRunController
├─ AgentRunService
├─ AgentRunRequest
├─ AgentRunResponse
├─ AgentEvent
└─ AgentEventType
```

예정 Endpoint:

``` text
POST /api/agent/run
GET  /api/agent/runs/{runId}
GET  /api/agent/runs/{runId}/events
```

초기에는 Mock Agent로 REST/SSE를 검증한 뒤 실제 `GomsBook-AI-Agent`와
연결합니다.

## 10. HITL 확장

``` text
Agent
  ↓
APPROVAL_REQUIRED
  ↓ SSE
Web Chat
  ↓
사용자 승인/거절
  ↓ REST
GomsBook-AI-API
  ↓
Agent 작업 계속
```

## 11. 데이터 저장 전략

``` text
PostgreSQL
├─ Project
├─ Document Metadata
├─ Chunk Metadata
├─ Agent Run
├─ Approval
└─ RAG Evaluation

Qdrant
├─ Embedding Vector
├─ Vector Search
├─ topK Retrieval
└─ Vector Payload
```

Vector 검색과 관계형 영속 데이터의 역할을 분리합니다.

## 12. Docker 연계

``` text
D:\04.GomsBook-AI\
├─ GomsBookEditor
├─ GomsBook-AI-Agent
├─ GomsBook-AI-MCP
├─ GomsBook-AI-API
└─ GomsBook-AI-Docker
```

    Port 용도
  ------ -----------------------------
    5001 STS4 개발용 GomsBook-AI-API
    2626 Nginx 외부 진입점
    8080 Docker 내부 GomsBook-AI-API
    6333 Qdrant
    9092 Kafka
    4317 OpenTelemetry gRPC
    4318 OpenTelemetry HTTP

## 13. 개발 원칙

-   API 계층과 Agent Core를 분리한다.
-   Controller는 HTTP 입출력 책임에 집중한다.
-   Service/Application 계층에 유스케이스를 둔다.
-   Agent Core 구현 세부사항은 Adapter 뒤에 숨긴다.
-   장시간 Agent 작업은 이벤트 기반으로 처리한다.
-   사용자 승인이 필요한 변경은 HITL을 거친다.
-   Vector 검색과 관계형 영속 데이터의 역할을 분리한다.
-   Trace/Event 식별자를 고려해 관측 가능성을 확보한다.

## 14. 개발 Roadmap

### Phase 1 --- API Foundation

-   [x] Spring Boot 프로젝트 생성
-   [x] Java 21 설정
-   [x] 개발 포트 5001 설정
-   [x] Actuator Health 확인
-   [x] `/api/epub` 확인
-   [x] Actuator Mappings 확인

### Phase 2 --- Agent REST/SSE

-   [ ] AgentRunRequest
-   [ ] AgentRunResponse
-   [ ] AgentEventType
-   [ ] AgentEvent
-   [ ] AgentRunService
-   [ ] AgentRunController
-   [ ] Mock Agent SSE 테스트
-   [ ] Web Chat 연동

### Phase 3 --- Agent Integration

-   [ ] GomsBook-AI-Agent 연결 경계 설계
-   [ ] AgentExecutor Adapter
-   [ ] Tool 실행 이벤트 연계
-   [ ] RAG 이벤트 연계
-   [ ] HITL 승인 연계

### Phase 4 --- Infrastructure

-   [ ] Docker API Build
-   [ ] Nginx 연계
-   [ ] Qdrant
-   [ ] PostgreSQL
-   [ ] Kafka
-   [ ] OpenTelemetry
-   [ ] Jaeger

### Phase 5 --- Operations

-   [ ] Spring Security
-   [ ] API 인증/인가
-   [ ] Prometheus/Grafana 검토
-   [ ] 모바일 알림 연계
-   [ ] 장애/재시도 정책
-   [ ] 운영 배포 전략

## 15. 다음 작업

Mock Agent 기반 REST + SSE 흐름을 구현합니다.

``` text
POST /api/agent/run
        ↓
runId 반환
        ↓
GET /api/agent/runs/{runId}/events
        ↓
Agent Event Stream
```

이 흐름을 독립적으로 검증한 뒤 실제 GomsBook-AI-Agent의
`AgentExecutor`와 연결합니다.
