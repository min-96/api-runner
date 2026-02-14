# 02_SYSTEM_ARCHITECTURE.md
# Spring Controller API Runner — System Architecture

---

## 1. 목적

이 문서는 다음을 명확히 정의한다:

- 전체 시스템 구조
- 모듈 간 의존 방향
- 실행 흐름(Controller ▶ 클릭부터 결과 출력까지)
- 핵심 인터페이스 경계
- 확장 가능성을 고려한 구조적 원칙

이 문서는 “구현 세부”가 아니라 “구조 고정 문서”다.

---

# 2. 전체 아키텍처 개요

## 2.1 모듈 구조

                ┌────────────────────┐
                │   apirunner-ide    │
                │ (IntelliJ Adapter) │
                └─────────┬──────────┘
                          ↓
            ┌──────────────────────────┐
            │      apirunner-core      │
            │ (Execution Engine Model) │
            └───────────┬──────────────┘
                        ↓
    ┌────────────────────────────┐
    │       apirunner-http       │
    │ (OkHttp Executor Layer)    │
    └────────────────────────────┘

    ┌────────────────────────────┐
    │     apirunner-generator    │
    │ (Auto Input Generator)     │
    └────────────────────────────┘

---

## 2.2 의존 방향 원칙

IDE → Core → (HTTP / Generator)

절대 반대로 의존하지 않는다.

### 금지 규칙

- Core는 IntelliJ API를 모른다.
- HTTP는 IntelliJ API를 모른다.
- Generator는 IntelliJ API를 모른다.
- IDE는 Core 내부 로직을 직접 실행하지 않는다 (Engine만 호출).

---

# 3. 실행 흐름 (MVP 기준)

## 3.1 전체 시퀀스

```yaml

[1] User clicks ▶ on Controller

↓

[2] apirunner-ide
- PSI 분석
- EndpointDescriptor 생성

↓

[3] apirunner-generator
- RequestBody 자동 생성
- Path/Query 기본값 생성

↓

[4] Dialog UI 표시 (사용자 수정)
  
↓
  
[5] ExecutionPlan 생성
  
↓

[6] apirunner-core ExecutionEngine 실행
  
↓
  
[7] EnsureServerRunningStep
- RunConfiguration 실행
- 로그에서 포트 파싱
  
↓

[8] HttpCallStep
- HttpExecutor 호출
  
↓
  
[9] apirunner-http
- OkHttp 실행
- Cookie 저장
- JWT 자동 추출
  
↓
  
[10] ApiResponse 반환
  
↓

[11] ToolWindow에 결과 표시


```



---

# 4. 책임 분리 구조

## 4.1 IDE Adapter (apirunner-ide)

### 책임

- Controller PSI 분석
- EndpointDescriptor 생성
- Request 생성 및 Dialog UI
- RunConfiguration 제어
- 서버 로그 감시
- ToolWindow 렌더링

### 절대 하지 말아야 할 것

- JSON 자동 생성 로직
- HTTP 실행 로직
- ExecutionEngine 로직 직접 구현

---

## 4.2 Core (apirunner-core)

### 책임

- ApiRequest / ApiResponse 모델
- ExecutionPlan
- ExecutionEngine
- Step 인터페이스
- ExecutionContext
- TemplateResolver

### Core의 특징

- IntelliJ 의존성 없음
- HTTP 라이브러리 의존성 없음
- 순수 JVM 라이브러리처럼 동작

---

## 4.3 HTTP Layer (apirunner-http)

### 책임

- OkHttpClient 구성
- CookieJar
- AuthInterceptor
- HttpExecutor 구현

### 구조

HttpExecutor (Core 인터페이스)

↑

OkHttpExecutor (HTTP 모듈 구현)


Core는 HttpExecutor 인터페이스만 안다.

---

## 4.4 Generator (apirunner-generator)

### 책임

- 타입 기반 JSON 자동 생성
- 기본값 전략
- Depth 제한
- 컬렉션 생성 전략

### 입력

- typeName (String)

### 출력

- JSON String

Generator는 PSI를 모른다.

---

# 5. 핵심 인터페이스 경계

## 5.1 HttpExecutor

```kotlin
interface HttpExecutor {
    fun execute(request: ApiRequest, context: ExecutionContext): ApiResponse
}
```

Core는 구현체를 모른다.

## 5.2 BodyGenerator
```kotlin
interface BodyGenerator {
fun generate(typeName: String, config: GeneratorConfig): String
}

```

IDE는 Generator를 호출한다

## 5.3 Step
```kotlin

interface Step {
fun execute(context: ExecutionContext)
}


```


MVP Step:
* EnsureServerRunningStep
* HttpCallStep

# 6. 데이터 흐름 구조

## 6.1 Request 생성 흐름

```
PSI
  ↓EndpointDescriptor
  ↓Generator
  ↓ApiRequest
  ↓Dialog 수정
  ↓ExecutionPlan
```

---

## 6.2 HTTP 호출 흐름

```
ExecutionEngine
  ↓
HttpCallStep
  ↓
HttpExecutor
  ↓
ApiResponse
  ↓
ExecutionContext 업데이트
```

---

# 7. 확장성 고려 (시나리오 대비)

현재 구조는 단일 실행을 기준으로 하지만,

이미 다음 확장을 고려한 형태다.

## 7.1 Scenario 추가 시 변경점

- ExecutionPlan에 Step 여러 개 추가
- ExtractStep 추가
- TemplateResolver 적극 활용
- ExecutionContext.variables 활용

Core 구조 변경 없음.

---

# 8. 서버 실행 전략

## 기본 전략

- 서버가 이미 실행 중이면 재사용
- 실행 중이 아니면 RunConfiguration으로 실행
- 로그에서 포트 파싱
- 실패 시 fallback UI

## 포트 파싱 방식

정규식 기반:

```
Tomcat started on port(s):8080
Netty started on port(s):8080
```

---

# 9. UI 아키텍처

## 9.1 Input UI

- Dialog 기반
- 탭 구조
    - Path / Query
    - Headers
    - Body(JSON Editor)

## 9.2 Result UI

- ToolWindow 기반
- Status / Latency / Headers / Pretty JSON

---

# 10. 아키텍처 원칙 요약

1. Core는 IDE를 모른다.
2. HTTP는 Core 인터페이스만 구현한다.
3. Generator는 타입 이름만 입력받는다.
4. ExecutionEngine은 Step 기반으로 확장 가능하다.
5. MVP는 단순하지만 구조는 시나리오 대비형이다.

---

# 11. 다음 단계

- 03_MODULE_DESIGN.md
    - 각 모듈의 세부 패키지 구조
    - Gradle 의존 구조 고정
    - 테스트 전략 정의