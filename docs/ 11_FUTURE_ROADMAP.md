# 11_FUTURE_ROADMAP.md
# Spring Controller API Runner — Future Roadmap

---

# 1. 목적

이 문서는 MVP 이후의 확장 방향을 정의한다.

포함 내용:

- Phase 2 ~ Phase 5 기능 확장
- Scenario 지원 설계 방향
- 아키텍처 확장 전략
- 장기 제품 비전
- 기술 부채 관리 계획

---

# 2. 현재 MVP 상태 요약

MVP는 다음을 지원한다:

- Controller ▶ 실행
- 자동 Body 생성
- Path/Query 자동 채움
- 서버 자동 실행
- 포트 자동 감지
- JWT 자동 유지
- 세션 자동 유지
- 결과 ToolWindow 표시

MVP는 “단일 요청 자동 실행 도구”다.

---

# 3. Phase 2 — Scenario 지원

---

## 3.1 목표

여러 API를 순차 실행할 수 있는 기능 제공.

예:

1. 로그인
2. 토큰 추출
3. 인증 API 호출
4. 응답 값 추출
5. 다음 API에 주입

---

## 3.2 설계 전략

### Core 확장

현재 Core는 이미 Step 기반 구조를 가지고 있다.

확장 Step 예시:

- ExtractVariableStep
- DelayStep
- ConditionalStep
- RepeatStep

---

### ExecutionPlan 확장

현재:

```kotlin
ExecutionPlan(
    steps = listOf(HttpCallStep)
)
```

Scenario 지원 시:

```kotlin
ExecutionPlan(
    steps = listOf(
        HttpCallStep(loginRequest),
        ExtractVariableStep("$.accessToken", "jwt"),
        HttpCallStep(authRequest)
    )
)
```

---

## 3.3 TemplateResolver 확장

현재:

- {pathVar}
- {{variable}}

확장:

- JSONPath 기반 추출 지원
- 변수 scope 구분 (global/local)

---

## 3.4 UI 전략

Dialog → ToolWindow 기반 Scenario Editor로 전환

ToolWindow에서:

- Step 리스트
- Step 추가/삭제
- Extract 설정
- 실행 버튼

---

# 4. Phase 3 — JSONPath 기반 응답 추출

---

## 4.1 목표

JwtExtractor heuristic 제거

대신:

```kotlin
ExtractVariableStep(
    jsonPath = "$.data.accessToken",
    variableName = "jwt"
)
```

## 4.2 구조

- JSONPath 라이브러리 도입
- 응답 body 파싱 후 변수 저장
- context.variables에 저장

---

## 4.3 장점

- 인증 외에도 값 공유 가능
- Scenario 강력해짐
- Postman 수준 기능 확보

---

# 5. Phase 4 — Persistent Scenario 파일 지원

---

## 5.1 목표

Scenario를 파일로 저장하고 재사용 가능하게 한다.

---

## 5.2 파일 포맷 예시

YAML:

```yaml
steps:-request:method:POSTpath:/loginbody:username:adminpassword:1234extract:$.accessToken:jwt-request:method:GETpath:/users/meheaders:Authorization:"Bearer {{jwt}}"
```

---

## 5.3 장점

- 팀 공유 가능
- Git 버전 관리 가능
- CI와 연계 가능

---

# 6. Phase 5 — Advanced HTTP Features

---

## 6.1 Retry 정책

- 재시도 횟수 설정
- 특정 status code만 재시도

---

## 6.2 Timeout 커스터마이징

- 요청 단위 timeout 설정

---

## 6.3 Parallel Execution

- Step 병렬 실행 지원

---

# 7. Phase 6 — Test Export 기능

---

## 7.1 목표

Controller 실행 기록 → 테스트 코드 자동 생성

---

## 7.2 가능 기능

- ApiRequest → JUnit 테스트 코드 변환
- ApiRequest → RestAssured 코드 변환
- ApiRequest → .http 파일 변환

---

# 8. Phase 7 — Production Grade Feature

---

## 8.1 Mock Server 모드

- 서버 없이 Request/Response 시뮬레이션

---

## 8.2 Performance Mode

- 반복 호출
- Latency 통계
- 평균 응답 시간 측정

---

## 8.3 GraphQL 지원

- Controller 외 GraphQL endpoint 지원

---

# 9. 장기 제품 방향

---

## 9.1 Positioning

이 플러그인은:

- Postman 대체
- 테스트 코드 작성 전 검증 도구
- Backend 개발 생산성 도구

---

## 9.2 차별화 포인트

| Postman | API Runner |
| --- | --- |
| 수동 작성 | Controller 기반 자동 생성 |
| 서버 수동 실행 | 서버 자동 실행 |
| JWT 수동 관리 | 자동 관리 |
| IDE 외부 | IDE 내부 |

---

# 10. 아키텍처 유지 전략

---

## 원칙 1

Core는 IntelliJ를 몰라야 한다.

---

## 원칙 2

HTTP는 교체 가능해야 한다.

---

## 원칙 3

Generator는 PSI를 몰라야 한다.

---

## 원칙 4

Step 기반 구조는 절대 깨지면 안 된다.

---

# 11. 기술 부채 관리 전략

---

## 부채 1: Reflection 기반 Generator

→ Kotlin metadata 기반으로 교체

---

## 부채 2: JWT heuristic

→ JSONPath 기반 ExtractStep으로 교체

---

## 부채 3: Dialog UX 한계

→ ToolWindow 중심 UI로 전환

---

# 12. 로드맵 요약

| Phase | 목표 |
| --- | --- |
| MVP | 단일 API 자동 실행 |
| Phase 2 | Scenario 지원 |
| Phase 3 | JSONPath 추출 |
| Phase 4 | Scenario 파일 저장 |
| Phase 5 | Advanced HTTP 기능 |
| Phase 6 | 테스트 코드 Export |
| Phase 7 | Production Grade 확장 |

---

# 13. 최종 결론

이 프로젝트는 단순 플러그인이 아니다.

- 아키텍처적으로 확장 가능
- Core 중심 구조
- Step 기반 실행 엔진
- Scenario로 확장 가능
- IDE 내부 Postman 대체 도구

MVP는 작게 시작하지만,

구조는 충분히 확장 가능한 상태다.
