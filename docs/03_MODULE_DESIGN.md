# 03_MODULE_DESIGN.md

# Spring Controller API Runner — Module Design (Multi-module Gradle)

---

## 1. 목적

이 문서는 다음을 고정한다:

- 멀티 모듈 분리 기준(왜/어떻게 나눴는지)
- 각 모듈의 책임(Responsibility)과 금지 사항(Non-responsibility)
- 의존 관계(Dependency graph) 및 경계 인터페이스
- 패키지/디렉터리 구조
- 테스트 전략(어디서 무엇을 테스트할지)
- 향후 리팩토링(분리/이동) 기준

---

## 2. 모듈 구성

### 최종 모듈(초기부터 분리)

- `apirunner-core`
- `apirunner-http`
- `apirunner-generator`
- `apirunner-ide`

---

## 3. 의존 관계(고정)

의존 방향은 단방향이다.

```
apirunner-ide
├── depends on: apirunner-core
├── depends on: apirunner-http
└── depends on: apirunner-generator

apirunner-http
└── depends on: apirunner-core

apirunner-generator
└── depends on: apirunner-core

apirunner-core
└── no dependency (pure)

```


### 금지

- `core` → `ide/http/generator` 의존 금지
- `http` → `ide` 의존 금지
- `generator` → `ide` 의존 금지

---

## 4. 모듈 별 책임/금지 사항

---

# 4.1 apirunner-core

## 역할

“실행 엔진”과 “공통 도메인 모델”을 제공한다.

IDE/HTTP/Generator를 전혀 몰라야 한다.

## 포함 대상

- ExecutionEngine / ExecutionPlan / Step / ExecutionContext
- ApiRequest / ApiResponse / EndpointDescriptor
- TemplateResolver(변수 치환)
- 공통 오류 모델(CoreError 등) (선택)

## 금지

- IntelliJ Platform API
- OkHttp/HTTP 라이브러리 직접 의존
- Reflection 기반 타입 탐색(Generator 책임)
- UI 구성

## 패키지 구조

apirunner-core/src/main/kotlin/com/apirunner/core/
├── model/
│ ├── ApiRequest.kt
│ ├── ApiResponse.kt
│ └── EndpointDescriptor.kt
├── engine/
│ ├── ExecutionContext.kt
│ ├── ExecutionPlan.kt
│ ├── ExecutionEngine.kt
│ └── Step.kt
├── http/
│ └── HttpExecutor.kt
└── util/
└── TemplateResolver.kt

## 테스트 전략

- Step 실행 순서 보장
- Template 치환 동작
- ExecutionContext 상태 업데이트 규칙

---

# 4.2 apirunner-http

## 역할

HTTP 실행 구현체를 제공한다.

## 포함 대상

- OkHttpClientProvider
- CookieJar(Session 유지)
- Interceptor(Auth 헤더 자동 주입)
- OkHttpExecutor(HttpExecutor 구현)
- JWT 추출(heuristic) 로직(MVP)

## 금지

- IntelliJ API
- PSI 분석
- UI
- 서버 실행/포트 파싱(IDE 책임)

## 패키지 구조

apirunner-http/src/main/kotlin/com/apirunner/http/
├── OkHttpExecutor.kt
├── OkHttpClientProvider.kt
├── SessionCookieJar.kt
├── AuthInterceptor.kt
└── JwtExtractor.kt

## 핵심 경계

- `OkHttpExecutor`는 `apirunner-core`의 `HttpExecutor`를 구현한다.

## 테스트 전략

- CookieJar가 저장/재사용 되는지
- Interceptor가 context.jwt를 헤더로 주입하는지
- JWT heuristic 추출 성공/실패 케이스

---

# 4.3 apirunner-generator

## 역할

타입 정보(typeName)를 기반으로 입력(Body/Param) 기본값을 생성한다.

## 포함 대상

- BodyGenerator 인터페이스/구현
- GeneratorConfig(maxDepth, collectionSize 등)
- TypeInspector(Reflection 기반 MVP)
- DefaultValueStrategy(primitive 기본값)
- (확장 대비) Enum/Optional/Generic 처리

## 금지

- IntelliJ API
- PSI 분석
- 서버 실행/HTTP 호출
- 결과 출력

## 패키지 구조

apirunner-generator/src/main/kotlin/com/apirunner/generator/
├── BodyGenerator.kt
├── DefaultBodyGenerator.kt
├── GeneratorConfig.kt
├── TypeInspector.kt
├── ReflectionTypeInspector.kt
├── DefaultValueStrategy.kt
└── SimpleDefaultValueStrategy.kt

## 테스트 전략

- 단순 DTO JSON 생성
- 깊이 제한 동작
- 순환 참조 방어(가능하면)
- primitive/enum/list/object 생성 규칙

---

# 4.4 apirunner-ide

## 역할

IntelliJ 환경에서 Core/HTTP/Generator를 조합해 사용자 경험을 만든다.

## 포함 대상

- RunLineMarkerContributor(▶ 표시)
- PSI EndpointExtractor
- Dialog(Input UI)
- ToolWindow(Result UI)
- ServerManager(RunConfig 실행, 로그 감시, 포트 파싱)
- Orchestrator(전체 흐름 조합)

## 금지

- Core/Generator/HTTP에 들어갈 로직(중복 구현 금지)
- JSON 생성 알고리즘 직접 구현 금지(Generator 호출)
- OkHttp 직접 호출 금지(HttpExecutor 사용)

## 패키지 구조(권장)
```
apirunner-ide/src/main/kotlin/com/apirunner/ide/
├── marker/
│ └── ControllerRunLineMarker.kt
├── action/
│ └── RunControllerAction.kt
├── psi/
│ ├── EndpointExtractor.kt
│ └── PsiTypeResolver.kt
├── ui/
│ ├── RequestDialog.kt
│ ├── ResultToolWindowFactory.kt
│ └── ResultPresenter.kt
├── server/
│ ├── ServerManager.kt
│ └── PortParser.kt
└── orchestration/
└── ApiExecutionOrchestrator.kt

```


## 테스트 전략(현실적)

- IDE 관련은 통합 테스트가 어렵기 때문에:
    - 핵심 로직을 core/generator/http로 최대 이동
    - ide는 얇게 유지
- PortParser는 순수 함수로 만들어 단위 테스트 가능

---

## 5. Gradle 멀티 모듈 구조

### settings.gradle.kts

- root 이름
- include 모듈

### 루트 build.gradle.kts

- 공통 Kotlin 버전
- 공통 repositories/test deps

### 각 모듈 build.gradle.kts

- `core`: kotlin stdlib만
- `http`: okhttp + core
- `generator`: jackson + core
- `ide`: intellij plugin + (core/http/generator)

---

## 6. 경계 모델(도메인 모델의 “소유권”)

### core가 소유하는 모델

- ApiRequest, ApiResponse, EndpointDescriptor
- ExecutionPlan/Engine/Context/Step

### ide가 소유하는 모델

- PSI 관련 타입들(PsiMethod 등)
- UI state(사용자 입력 임시 상태)

### generator가 소유하는 모델

- GeneratorConfig
- TypeField(필드 정보)

### http가 소유하는 모델

- OkHttpClient 구성 요소
- JWT 추출 규칙(MVP heuristic)

---

## 7. 향후 리팩토링 계획(로드맵 관점)

### 7.1 Scenario 지원 추가 시(Phase 2)

- core:
    - Step 추가: ExtractVariableStep, ConditionalStep(선택)
    - TemplateResolver를 적극 사용
    - ExecutionContext.variables 활용 확대
- ide:
    - Scenario 실행 UI/편집기(ToolWindow) 추가
- generator/http:
    - 변경 최소화

### 7.2 “Export to .http” 추가 시(Phase 3)

- ide:
    - ApiRequest → .http 변환기 추가
- core:
    - 변경 없음

### 7.3 Generator 고도화(Phase 2~3)

- Kotlin metadata 기반 필드 추출(Reflection 개선)
- Generic/List 처리 강화
- Enum/Optional/Nullable 처리 강화
- 사용자 정의 샘플 규칙(프로젝트별 preset)

---

## 8. 리스크(모듈 설계 관점)

### R1. IDE 모듈이 비대해지는 위험

- 해결:
    - 가능한 로직은 core/http/generator로 이동
    - ide는 “조합/호출”만 담당

### R2. Generator Reflection 한계

- 해결:
    - MVP는 Reflection으로 시작
    - Phase 2에 개선(타입 인식, Kotlin metadata)

### R3. HTTP/인증 자동화 복잡도 증가

- 해결:
    - http 모듈 내부에서만 해결하도록 책임 고정
    - core는 단순 인터페이스 유지

---

## 9. 다음 문서

- 04_CORE_DESIGN.md
    - core의 세부 모델/Step/TemplateResolver 확정