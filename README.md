# 🧠 AI Chatbot (Spring + OpenAI + Redis)

## 📌 Overview

OpenAI API와 Redis를 활용하여 **멀티턴 대화를 지원하는 AI 챗봇 서비스**를 구현했습니다.

단순한 단발성 질문/응답이 아닌,
👉 **사용자별 대화 히스토리를 유지하여 맥락(Context)을 기반으로 응답하는 구조**를 설계했습니다.

---

## 🧠 Key Features

* 🔹 OpenAI Chat API 연동 (WebClient 기반)
* 🔹 Redis 기반 사용자별 대화 히스토리 관리
* 🔹 멀티턴(Multi-turn) 대화 구현
* 🔹 system / user / assistant 역할 기반 메시지 구조 설계
* 🔹 JSON 직렬화/역직렬화를 통한 데이터 구조 유지
* 🔹 대화 길이 제한 (최근 메시지 유지)으로 성능 최적화
* 🔹 환경변수 기반 API Key 관리 (보안 고려)

---

## 🏗 Architecture

```
Controller
   ↓
ChatService (대화 흐름 관리)
   ↓
Redis (대화 저장/조회)
   ↓
OpenAiClient (API 호출)
```

---

## 🔄 Flow

1. Redis에서 사용자 대화 히스토리 조회
2. system 메시지 (초기 1회) 설정
3. 사용자 메시지 추가
4. 대화 길이 제한 (최근 메시지 유지)
5. OpenAI API 호출 (전체 대화 전달)
6. assistant 응답 생성
7. Redis에 대화 업데이트 저장
8. 클라이언트에 응답 반환

---

## 🧱 Message Structure

```json
[
  {"role":"system","content":"너는 친절한 AI 상담원이다"},
  {"role":"user","content":"안녕"},
  {"role":"assistant","content":"안녕하세요"},
  {"role":"user","content":"뭐해?"}
]
```

👉 OpenAI의 `messages` 구조를 그대로 사용하여
👉 대화 맥락(Context)을 유지

---

## 🛠 Tech Stack

* Java 17
* Spring Boot
* Spring WebFlux (WebClient)
* Redis
* Jackson (ObjectMapper)
* OpenAI API

---

## 🔐 Security

API Key는 코드에 직접 작성하지 않고
👉 **환경변수(`OPENAI_API_KEY`)로 관리**

```yaml
openai:
  api:
    key: ${OPENAI_API_KEY}
```

---

## 🚀 How to Run

### 1. 환경변수 설정

```bash
export OPENAI_API_KEY=your_api_key
```

---

### 2. Redis 실행

```bash
redis-server
```

---

### 3. 서버 실행

```bash
./gradlew bootRun
```

---

### 4. API 테스트 (Postman)

```
POST /api/chat
```

```json
{
  "userId": "user1",
  "message": "안녕"
}
```

---

## 📈 Improvement (Next Step)

* 🔥 Streaming 응답 (실시간 출력)
* 🔥 토큰 기반 메시지 관리
* 🔥 Redis TTL 적용 (대화 자동 만료)
* 🔥 채팅 UI (React) 연동

---

## 💬 What I Learned

* OpenAI API의 `messages` 구조 기반 멀티턴 대화 설계
* Redis를 활용한 상태 관리(Stateful 서비스 구현)
* JSON 직렬화/역직렬화의 필요성과 활용
* API Key 보안 관리 (환경변수 활용)
* Service 계층에서 대화 흐름을 관리하는 아키텍처 설계

---

## 🧩 Conclusion

이 프로젝트는 단순 API 호출을 넘어
👉 **대화 상태를 유지하는 AI 서비스 구조를 설계하고 구현한 프로젝트입니다.**
