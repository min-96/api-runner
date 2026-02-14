# 01_PRODUCT_OVERVIEW.md
# Spring Controller API Runner (IntelliJ Plugin)

## 1. 목적
Spring 개발자가 API를 빠르게 확인하기 위해 Postman/Swagger/별도 테스트 코드 작성에 의존하는 흐름을,
**IDE 내부(Controller 코드 기준)에서 즉시 실행 가능한 경험**으로 대체한다.

- Controller 메서드 옆 ▶ 버튼으로 실행
- Path/Query/Body 입력을 자동으로 채워주고, 사용자는 값만 수정
- 서버 자동 실행 및 포트 자동 감지
- 쿠키/세션/JWT를 자동 유지하여 반복 호출을 최소화
- (확장) 여러 API를 시나리오로 묶어 순차 실행

---

## 2. 배경/문제 정의
### 현재 개발 흐름의 문제
- IDE ↔ Postman 전환 비용(컨텍스트 스위칭)
- 반복적인 입력(파라미터/바디) 작성과 유지관리
- 인증(JWT/세션) 재설정/주입 번거로움
- “빠른 확인”을 위해 테스트 코드까지 작성해야 하는 부담

### 왜 Controller 기준인가
- 개발자가 가장 많이 머무는 지점이 Controller 코드
- Endpoint 정의(어노테이션, 파라미터, 바디 타입)가 코드에 있으므로 자동화에 최적
- 테스트처럼 “메서드 단위 실행 UX”를 API에도 제공 가능

---

## 3. 타겟 사용자/사용 시나리오
### 타겟 사용자
- Spring MVC 기반 백엔드 개발자
- 로컬 개발 중 API를 자주 수동 호출하는 팀
- Postman을 쓰지만 “입력/인증/반복 호출”이 귀찮은 사용자

### 대표 사용 시나리오
1) 로그인 API 한번 실행 → JWT 자동 저장
2) 이후 모든 API 호출 시 Authorization 자동 주입
3) PathVariable/QueryParam/RequestBody는 자동 생성 → 값만 수정
4) 서버가 꺼져 있으면 자동 기동 → 호출 완료 후 결과를 ToolWindow에서 확인

---

## 4. 제품 컨셉
### 핵심 컨셉
> “Postman을 IDE에 넣는다. 단, Spring 프로젝트에 최적화된 방식으로.”

- IDE에서 실행
- Controller 기반
- 입력 자동 생성
- 인증 자동 유지
- 결과는 간결하게(검증/Assertion은 MVP에서 제외)

---

## 5. MVP 범위(Scope)
### MVP에 포함(필수)
1) Controller 메서드 옆 ▶ 실행 버튼 제공
2) Endpoint 추출(HTTP method, pathTemplate, params, requestBodyType)
3) 입력 자동 생성
    - PathVariable 기본값 자동 채움
    - RequestParam 기본값 자동 채움
    - RequestBody JSON 자동 생성(타입 기반, 깊이 제한)
4) 입력 UI
    - Dialog에서 Path/Query/Headers/Body 편집
5) 서버 자동 실행/재사용
    - 실행 중이면 재사용(기본)
    - 실행 안 되어 있으면 RunConfiguration으로 실행
6) 포트 자동 감지
    - 서버 로그에서 포트 추출
    - 실패 시 사용자가 직접 입력하는 fallback
7) HTTP 호출
    - OkHttp 기반 호출
    - CookieJar로 세션 유지
    - Interceptor로 JWT 자동 주입
8) 결과 출력
    - ToolWindow에 status/latency/headers/body 표시

### MVP에서 제외(Non-Goals)
- 시나리오 UI 편집기(드래그앤드롭)
- 조건 분기/반복/병렬 실행/리트라이 정책
- WebFlux 완전 대응(우선 WebMVC 중심)
- 테스트 검증(assertion), CI 연동
- OpenAPI/Swagger 연동 기반 자동 시나리오 생성(후순위)

---

## 6. 성공 기준(Success Metrics)
MVP 완료 후 아래가 “실제로 편하다” 수준이면 성공:
- (S1) Controller에서 ▶ 클릭 → 10초 이내에 첫 호출까지 도달(서버 미기동 포함)
- (S2) 동일 엔드포인트 재호출 시 입력 자동 채움으로 3~5초 내 반복 실행 가능
- (S3) 로그인 1회 후 JWT/세션 유지로 인증 API 호출이 사실상 “사라짐”
- (S4) 최소 1개의 실제 프로젝트에서 팀원이 자발적으로 사용(주 3회 이상)

---

## 7. 제품 원칙(Design Principles)
1) **자동화 우선, 강제 최소**
    - 키는 자동 생성, 값은 사용자가 쉽게 수정
2) **실행 엔진은 UI/IDE와 분리**
    - 나중에 시나리오/파일 기반 정의/CLI 확장도 가능하도록
3) **안전한 실패(Fail-safe)**
    - 포트 파싱 실패/서버 실행 실패 시 사용자 입력 fallback 제공
4) **MVP는 “빠른 확인”에만 집중**
    - 검증/테스트 프레임워크로 확장하는 건 후순위

---

## 8. 주요 의사결정(Decision Summary)
- Input UI: **Dialog**
- Result UI: **ToolWindow**
- HTTP: **OkHttp**
    - CookieJar로 세션 유지
    - Interceptor로 JWT 자동 주입
- Port Detection: **Server log parsing**
- Architecture: **Multi-module Gradle**
    - apirunner-core / apirunner-generator / apirunner-http / apirunner-ide

---

## 9. 오픈 이슈(Open Questions)
- JWT 자동 추출 방식:
    - MVP: token/accessToken/jwt key heuristic
    - 확장: JSONPath 규칙 저장
- 서버 종료 정책:
    - 기본: 재사용(종료하지 않음)
    - 옵션: 실행 후 종료(추가 설정)
- WebFlux 지원 범위:
    - MVP: 로그 패턴 일부 대응 + 제한 사항 명시

---

## 10. 다음 문서(다음 단계)
- 02_SYSTEM_ARCHITECTURE.md
    - 전체 모듈 다이어그램
    - 데이터/실행 흐름 시퀀스
    - 책임과 의존 방향 고정
