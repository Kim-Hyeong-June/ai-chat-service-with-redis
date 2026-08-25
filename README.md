# Enterprise Chatbot

Spring Boot WebFlux 기반의 **기업용 RAG(Retrieval-Augmented Generation) 챗봇** 프로젝트입니다.

사용자가 업로드한 문서를 파싱하고 임베딩하여 PostgreSQL `pgvector`에 저장한 뒤, 질문과 관련된 문서를 검색하여 OpenAI 모델에 Context로 전달합니다.

또한 Redis를 이용하여 대화 기록을 관리하고, SSE(Server-Sent Events)를 통해 AI 응답을 실시간으로 스트리밍합니다.

---

## 1. 주요 기능

### 사용자 인증

* 회원가입 / 로그인
* JWT 기반 인증
* Access Token / Refresh Token 발급
* 사용자별 문서 및 대화 데이터 분리

### AI Chat

* OpenAI API 연동
* Spring WebFlux 기반 비동기 처리
* SSE 기반 실시간 응답 스트리밍
* 멀티턴 대화 지원
* Redis 기반 대화 History 관리

### RAG

```text
문서 업로드
    ↓
Apache Tika 문서 파싱
    ↓
Chunking
    ↓
Embedding 생성
    ↓
PostgreSQL + pgvector 저장
    ↓
사용자 질문
    ↓
질문 Embedding 생성
    ↓
Vector Similarity Search
    ↓
관련 Chunk Top 3 검색
    ↓
Context 생성
    ↓
OpenAI
    ↓
최종 답변
```

### 문서 처리

* 파일 업로드
* Apache Tika 기반 텍스트 추출
* 문서 Chunking
* Chunk별 Embedding 생성
* 사용자별 문서 관리

### Vector Search

* OpenAI Embedding API 사용
* PostgreSQL pgvector 사용
* Cosine Distance 기반 유사도 검색
* Top-K 검색
* Distance Threshold를 이용한 관련성 필터링

예:

```sql
SELECT
    dc.content,
    dc.embedding <=> CAST(:embedding AS vector) AS distance
FROM document_chunks dc
JOIN documents d
    ON dc.document_id = d.id
WHERE d.user_id = :userId
ORDER BY distance
LIMIT 3;
```

가장 유사한 문서의 거리가 임계값보다 큰 경우 관련 문서가 없는 것으로 처리합니다.

---

## 2. Tech Stack

### Backend

* Java 17
* Spring Boot 3
* Spring WebFlux
* Project Reactor
* Spring Security
* R2DBC

### Database

* PostgreSQL
* pgvector
* Flyway

### Cache / Conversation

* Redis
* ReactiveRedisTemplate

### AI

* OpenAI Chat API
* OpenAI Embedding API
* RAG

### Document Processing

* Apache Tika

### Infrastructure

* Docker
* Docker Compose

---

## 3. Architecture

```text
                   Client
                     │
                     │ HTTP / SSE
                     ↓
              Spring WebFlux
                     │
          ┌──────────┼──────────┐
          ↓          ↓          ↓
        Auth        Chat      Document
          │          │          │
          ↓          ↓          ↓
        JWT       OpenAI      Tika
                     │          │
                     │       Chunking
                     │          ↓
                     │      Embedding
                     │          │
          ┌──────────┴──────────┘
          ↓
     PostgreSQL
      + pgvector

          ↑
     Vector Search

                     │
                     ↓
                   Redis
              Conversation
                 History
```

---

## 4. RAG 동작 과정

### 문서 등록

사용자가 문서를 업로드하면 Apache Tika를 이용하여 텍스트를 추출합니다.

```text
File
 ↓
Tika
 ↓
Text
 ↓
Chunking
 ↓
Embedding
 ↓
pgvector
```

파일 I/O와 Tika 파싱은 Blocking 작업이기 때문에 WebFlux의 Netty Event Loop를 차단하지 않도록 `boundedElastic` Scheduler에서 처리합니다.

```java
Mono.fromCallable(() -> {
    // File I/O
    // Tika parsing
    return chunks;
})
.subscribeOn(Schedulers.boundedElastic());
```

### 질문 처리

사용자 질문을 Embedding Vector로 변환합니다.

```text
"회사 연차 규정이 뭐야?"

        ↓

Embedding API

        ↓

[0.12, -0.35, 0.78, ...]
```

질문의 Embedding과 저장된 Chunk Embedding의 Cosine Distance를 비교합니다.

```text
Question Vector
       ↓
pgvector
       ↓
Chunk A → 0.12
Chunk B → 0.25
Chunk C → 0.41
       ↓
Top 3
```

Distance가 작을수록 질문과 의미적으로 유사한 문서입니다.

검색된 Chunk들은 하나의 Context로 결합되어 LLM에 전달됩니다.

---

## 5. Multi-turn Conversation

이전 대화 기록을 저장하여 문맥이 이어지는 대화를 지원합니다.

```text
User
"연차가 며칠이야?"

Assistant
"연차는 15일입니다."

User
"신청은 어떻게 해?"
```

두 번째 질문에서도 이전 대화를 함께 전달하여 모델이 `"연차 신청 방법"`에 대한 질문임을 이해할 수 있도록 합니다.

Redis를 이용하여 사용자별 Conversation History를 관리합니다.

---

## 6. System Prompt & RAG Context

검색된 문서가 존재하면 System Prompt에 RAG Context를 포함합니다.

```text
너는 친절한 AI 상담원이다.
아래 문서를 참고해서 답변해라.

[검색된 문서 Context]
```

관련 문서가 없는 경우 일반 상담원 System Prompt를 사용합니다.

```text
너는 친절한 AI 상담원이다.
```

---

## 7. Reactive Architecture

애플리케이션은 Spring WebFlux와 Reactor의 `Mono`, `Flux`를 사용합니다.

```text
HTTP Request
     ↓
Netty Event Loop
     ↓
Spring WebFlux
     ↓
Service
     ↓
R2DBC / Redis / OpenAI
     ↓
Mono / Flux
     ↓
HTTP / SSE Response
```

R2DBC를 사용하여 PostgreSQL I/O를 논블로킹 방식으로 처리합니다.

파일 처리와 Tika 파싱처럼 Blocking이 필요한 작업은 `boundedElastic` Scheduler로 분리합니다.

---

## 8. Docker Architecture

Docker Compose를 이용하여 애플리케이션 실행 환경을 구성합니다.

```text
Docker Compose
│
├── chatbot-app
│      └── Spring Boot
│
├── chatbot-postgres
│      └── PostgreSQL + pgvector
│
└── chatbot-redis
       └── Redis
```

Docker Compose 내부에서는 서비스 이름을 Hostname으로 사용합니다.

```text
app
 ├── postgres:5432
 └── redis:6379
```

---

## 9. Environment Variables

OpenAI API Key는 소스코드에 직접 저장하지 않고 환경변수로 관리합니다.

`application.yml`

```yaml
openai:
  api-key: ${OPENAI_API_KEY}
```

Docker Compose:

```yaml
environment:
  SPRING_PROFILES_ACTIVE: docker
  OPENAI_API_KEY: ${OPENAI_API_KEY}
```

실행 전 환경변수를 설정합니다.

```bash
export OPENAI_API_KEY='YOUR_OPENAI_API_KEY'
```

> API Key, JWT Secret 등의 민감정보는 Git 저장소에 커밋하지 않습니다.

---

## 10. 실행 방법

### 환경변수 설정

```bash
export OPENAI_API_KEY='YOUR_OPENAI_API_KEY'
```

### Docker Compose 실행

```bash
docker compose up -d --build
```

### 컨테이너 확인

```bash
docker ps
```

### Spring Boot 로그 확인

```bash
docker logs --tail 100 -f chatbot-app
```

### 종료

```bash
docker compose down
```

---

## 11. Database Migration

Flyway를 사용하여 데이터베이스 스키마를 관리합니다.

```text
PostgreSQL
    ↓
chatdb
    ↓
Flyway + JDBC
    ↓
DB Migration
    ↓
Table / Column / Index 생성 및 변경
```

실제 애플리케이션의 데이터 조회 및 저장은 R2DBC를 이용합니다.

```text
Flyway + JDBC
→ DB Schema 관리

R2DBC
→ 애플리케이션 데이터 조회/저장
```

---

## 12. Project Structure

```text

src/main/java/com/example/chatbot
│
├── domain
│   ├── chat
│   ├── document
│   └── user
│
├── global
│   ├── config
│   ├── exception
│   └── jwt
│
└── infrastructure
    ├── openai
    └── redis

src/main/resources
│
├── application.yml
└── db
    └── migration

Dockerfile
docker-compose.yml
```

---

## 13. 핵심 구현 포인트

이 프로젝트에서 중점적으로 구현한 부분은 다음과 같습니다.

* Spring WebFlux 기반 Reactive Backend
* SSE 기반 LLM 응답 스트리밍
* PostgreSQL + pgvector 기반 Vector Search
* 문서 Parsing → Chunking → Embedding 파이프라인
* Cosine Distance 기반 관련 문서 검색
* Distance Threshold 기반 검색 결과 필터링
* RAG Context를 활용한 LLM 응답 생성
* Redis 기반 Multi-turn Conversation
* Blocking 작업과 Non-blocking 작업 분리
* JWT 기반 사용자 인증
* Docker Compose 기반 실행 환경 구성
* Flyway 기반 DB Migration
* 환경변수를 이용한 API Key 관리

---

## 14. 향후 개선 사항

* Chunk Size / Overlap 검색 품질 비교
* Metadata Filtering
* Hybrid Search
* Reranking 적용
* RAG Evaluation 구축
* 검색 정확도 및 응답 품질 측정
* Agent Tool Calling
* Multi-step Agent Workflow
* Observability 및 Monitoring 강화
