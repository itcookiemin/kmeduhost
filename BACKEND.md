# BACKEND.md

# 제목
- claude api key를 발급받을 수 있는 프로그램

# 기술스펙
- Backend Spring Boot
- Claude Api Key를 rest api 로 호출하는 controller 하나만 생성
- jdk: 1.7
- 배포빌드 : maven

# 소스 기본 구성
- project name: StepBaking
- package groupId: io.itcookies.edu
- BootApp classname : StepBakingApp.java
- 설정파일 : application.properties
- 호출 Controller : LlmApiController.java

# 프로젝트 구조
```
backend/
├── pom.xml
├── BACKEND.md
└── src/main/
    ├── java/io/itcookies/edu/
    │   ├── StepBakingApp.java              # 메인 클래스
    │   └── controller/
    │       └── LlmApiController.java       # Claude API 컨트롤러
    └── resources/
        └── application.yml                 # 설정 파일
```

# LlmApiController 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/claude/chat` | Claude에 메시지 전송 |
| `GET` | `/api/claude/health` | API 키 유효성 확인 |

# 사용 전 필수 설정

`application.yml`에서 API 키를 실제 값으로 교체하세요:
```yaml
claude:
  api:
    key: YOUR_CLAUDE_API_KEY_HERE
```

**chat 엔드포인트 호출 예시:**
```bash
curl -X POST http://localhost:8080/api/claude/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "안녕하세요!"}'
```

# GeminiApiController 엔드포인트

| Method | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/gemini/chat` | Gemini에 메시지 전송 |
| `GET` | `/api/gemini/health` | API 키 유효성 확인 |

# Gemini API 키 설정

`application.properties`에서 API 키를 실제 값으로 교체하세요:
```properties
gemini.api.key=YOUR_GEMINI_API_KEY_HERE
gemini.api.model=gemini-2.0-flash
```

**chat 엔드포인트 호출 예시:**
```bash
curl -X POST http://localhost:8080/api/gemini/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "안녕하세요!"}'
```

---
# .env.example을 복사해서 실제 키 입력
cp .env.example .env                                                                                # .env 파일에 실제 API 키 입력
---      

생성된 파일 목록:

┌────────────────────┬──────────────────────────────────────────────────────┐
│        파일        │                         역할                         │
├────────────────────┼──────────────────────────────────────────────────────┤
│ Dockerfile         │ 멀티스테이지 빌드 (builder → runtime), non-root 실행 │
├────────────────────┼──────────────────────────────────────────────────────┤
│ docker-compose.yml │ 컨테이너 실행 설정, 환경변수 주입                    │
├────────────────────┼──────────────────────────────────────────────────────┤
│ .env.example       │ 환경변수 템플릿 (복사해서 .env로 사용)               │
├────────────────────┼──────────────────────────────────────────────────────┤
│ .dockerignore      │ 빌드 컨텍스트에서 불필요한 파일 제외                 │
└────────────────────┴──────────────────────────────────────────────────────┘
실행 방법:

# 1. .env 파일 생성 후 API 키 입력
cp .env.example .env

# 2. 빌드 & 실행
docker compose up --build

# 3. 백그라운드 실행
docker compose up -d --build

# 4. 중지
docker compose down

주의: .env 파일은 .gitignore에 추가해서 API 키가 git에 올라가지 않도록 하세요.

echo ".env" >> .gitignore
