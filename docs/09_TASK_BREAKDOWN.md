# 09_TASK_BREAKDOWN.md
# Spring Controller API Runner — Task Breakdown & Development Plan

---

# 1. 목적

이 문서는 실제 개발을 진행하기 위한 실행 계획이다.

포함 내용:

- Phase 기반 개발 단계
- 각 Phase의 목표
- 상세 체크리스트
- 위험 구간
- 리팩토링 시점
- MVP 완료 기준

---

# 2. 전체 개발 전략

개발은 다음 순서로 진행한다:

Phase 0 → Core 안정화  
Phase 1 → Generator 완성  
Phase 2 → HTTP 실행 완성  
Phase 3 → IDE 연결  
Phase 4 → 서버 실행 자동화  
Phase 5 → UX 안정화

---

# 3. Phase 0 — Core 안정화

## 목표
Core가 IntelliJ 없이 완전 독립적으로 동작하도록 완성한다.

---

## 작업 목록

### 0.1 모델 확정
- [ ] ApiRequest 구현
- [ ] ApiResponse 구현
- [ ] EndpointDescriptor 구현
- [ ] ExecutionContext 구현

### 0.2 엔진 구현
- [ ] Step 인터페이스
- [ ] ExecutionPlan
- [ ] ExecutionEngine

### 0.3 TemplateResolver 구현
- [ ] PathVariable 치환
- [ ] {{variable}} 치환
- [ ] globalHeaders 병합

---

## 테스트 항목
- [ ] Step 순차 실행 확인
- [ ] Context 변수 공유 확인
- [ ] Template 치환 테스트

---

## 위험 구간
❗ Template 치환 로직이 복잡해지지 않도록 단순 유지

---

## 완료 기준
Core 단위 테스트 100% 통과

---

# 4. Phase 1 — Generator 완성

## 목표
RequestBody 자동 생성이 실사용 가능한 수준으로 동작

---

## 작업 목록

### 1.1 ReflectionTypeInspector
- [ ] Primitive 타입 인식
- [ ] Enum 인식
- [ ] Collection 인식
- [ ] 필드 추출

### 1.2 DefaultValueStrategy
- [ ] String
- [ ] Number
- [ ] Boolean
- [ ] Enum

### 1.3 DefaultBodyGenerator
- [ ] Depth 제한 적용
- [ ] Nested 객체 생성
- [ ] JSON Pretty 출력

### 1.4 ParamGenerator
- [ ] Path 기본값 생성
- [ ] Query 기본값 생성

---

## 테스트 항목
- [ ] 단순 DTO JSON 생성
- [ ] Nested 객체 생성
- [ ] Enum 처리
- [ ] Depth 제한 동작

---

## 위험 구간
❗ 순환 참조 → maxDepth로 방어  
❗ Generic 타입 미지원 → MVP에서는 단순화

---

## 완료 기준
실제 프로젝트 DTO로 JSON 생성 가능

---

# 5. Phase 2 — HTTP 실행 완성

## 목표
OkHttp 기반 실행 + 세션 + JWT 자동화

---

## 작업 목록

### 2.1 OkHttpClientProvider
- [ ] Timeout 설정
- [ ] CookieJar 등록
- [ ] Interceptor 등록

### 2.2 SessionCookieJar
- [ ] Host 기반 쿠키 저장
- [ ] 재요청 시 자동 적용

### 2.3 AuthInterceptor
- [ ] context.variables["jwt"] 자동 주입

### 2.4 JwtExtractor
- [ ] token/accessToken 탐색
- [ ] context에 저장

### 2.5 OkHttpExecutor
- [ ] GET
- [ ] POST
- [ ] PUT
- [ ] DELETE
- [ ] 응답 시간 측정

---

## 테스트 항목
- [ ] JWT 자동 주입 확인
- [ ] 로그인 후 JWT 저장 확인
- [ ] 쿠키 재사용 확인
- [ ] 4xx/5xx 처리

---

## 위험 구간
❗ JWT heuristic 실패  
→ Phase 2에서는 단순 구현 유지

---

## 완료 기준
로그인 → JWT 저장 → 인증 API 호출 성공

---

# 6. Phase 3 — IDE 연결

## 목표
Controller ▶ 버튼 → Dialog → HTTP 호출 → ToolWindow 출력

---

## 작업 목록

### 3.1 RunLineMarker 구현
- [ ] @Mapping 감지
- [ ] ▶ 아이콘 표시

### 3.2 EndpointExtractor
- [ ] HTTP Method 추출
- [ ] PathTemplate 추출
- [ ] PathVariable 추출
- [ ] QueryParam 추출
- [ ] RequestBodyType 추출

### 3.3 기본 ApiRequest 생성
- [ ] Generator 호출
- [ ] Param 기본값 적용

### 3.4 Dialog 구현
- [ ] Path/Query 탭
- [ ] Headers 탭
- [ ] Body 탭(JSON Editor)
- [ ] Run 버튼 동작

### 3.5 Orchestrator 연결
- [ ] ExecutionPlan 생성
- [ ] Engine 호출
- [ ] Result 전달

---

## 위험 구간
❗ Kotlin PSI 처리 난이도  
→ MVP는 Java + 기본 Kotlin 우선

---

## 완료 기준
Controller ▶ 클릭 시 실제 API 호출 성공

---

# 7. Phase 4 — 서버 자동 실행

## 목표
서버 미기동 시 자동 실행 + 포트 자동 감지

---

## 작업 목록

### 4.1 ModuleResolver
- [ ] 현재 파일 → Module 탐색

### 4.2 SpringBootApplication 탐색
- [ ] Annotation 기반 탐색

### 4.3 RunConfiguration 생성
- [ ] 기존 실행 중 재사용
- [ ] 없으면 생성

### 4.4 로그 감시
- [ ] ProcessListener 등록
- [ ] PortParser 적용
- [ ] baseUrl 설정

---

## 위험 구간
❗ 로그 패턴 불일치  
❗ 서버 기동 지연

---

## 완료 기준
서버 꺼진 상태에서 ▶ 클릭 시 자동 실행 + 호출 성공

---

# 8. Phase 5 — UX 안정화

## 목표
사용자 경험 개선 및 반복 실행 최적화

---

## 작업 목록

- [ ] 입력값 유지 전략
- [ ] ToolWindow Status 색상
- [ ] JSON Pretty 출력 개선
- [ ] 오류 메시지 개선
- [ ] 포트 파싱 실패 fallback UI

---

# 9. 리팩토링 시점

## Refactor 1 (Phase 2 완료 후)
- HTTP/Generator 경계 점검
- 중복 로직 제거

## Refactor 2 (Phase 3 완료 후)
- IDE 모듈 비대 여부 점검
- Orchestrator 분리 여부 판단

## Refactor 3 (MVP 완료 후)
- Scenario 대비 Step 구조 재검토
- TemplateResolver 확장성 점검

---

# 10. MVP 완료 기준

다음 조건을 모두 만족하면 MVP 완료:

- [ ] Controller ▶ 버튼 동작
- [ ] 자동 Body 생성
- [ ] Path/Query 자동 채움
- [ ] 서버 자동 실행
- [ ] 포트 자동 감지
- [ ] JWT 자동 유지
- [ ] 세션 자동 유지
- [ ] ToolWindow 결과 표시
- [ ] 반복 호출이 빠름

---

# 11. 다음 단계 문서

- 10_RISK_AND_LIMITATIONS.md
- 11_FUTURE_ROADMAP.md
