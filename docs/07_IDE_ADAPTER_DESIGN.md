# 07_IDE_ADAPTER_DESIGN.md

# Spring Controller API Runner — IDE Adapter Design

---

# 1. 목적

이 문서는 `apirunner-ide` 모듈의 설계를 정의한다.

IDE Adapter의 역할은 다음과 같다:

- Controller PSI 분석
- EndpointDescriptor 생성
- 자동 입력 생성(Generator 호출)
- Input Dialog UI 표시
- 서버 자동 실행 및 포트 감지
- ExecutionPlan 생성 및 실행
- 결과 ToolWindow 표시

IDE 모듈은 “조합/오케스트레이션 계층”이다.

실행 로직(Core)이나 HTTP 로직을 직접 구현하지 않는다.

---

# 2. 설계 원칙

1. IDE는 Core의 인터페이스만 사용한다.
2. IDE는 HTTP를 직접 호출하지 않는다 (HttpExecutor 사용).
3. JSON 자동 생성 로직은 Generator 호출만 한다.
4. Server 실행/포트 파싱은 IDE 책임이다.
5. IDE 코드는 얇게 유지한다(로직은 최대한 Core/Generator/HTTP로 이동).

---

# 3. 패키지 구조
```
apirunner-ide/
└── src/main/kotlin/com/apirunner/ide/
├── marker/
│ └── ControllerRunLineMarker.kt
├── action/
│ └── RunControllerAction.kt
├── psi/
│ ├── EndpointExtractor.kt
│ └── PsiTypeResolver.kt
├── orchestration/
│ └── ApiExecutionOrchestrator.kt
├── server/
│ ├── ServerManager.kt
│ └── PortParser.kt
├── ui/
│ ├── RequestDialog.kt
│ ├── ResultToolWindowFactory.kt
│ └── ResultPresenter.kt
└── util/
└── ModuleResolver.kt

```
---

## 4. IDE 실행 흐름(Controller ▶ 클릭부터 결과 출력까지)

### 4.1 High-level sequence

```
[1] User clicks ▶ on Controller method
↓
[2] ControllerRunLineMarker detects endpoint & triggers action
↓
[3] RunControllerAction
↓
[4] EndpointExtractor extracts EndpointDescriptor from PSI
↓
[5] Build default ApiRequest
- DefaultParamGenerator: path/query defaults
- BodyGenerator: request body JSON defaults
  ↓
  [6] RequestDialog shows (Path/Query/Headers/Body)
  ↓
  [7] User edits values → ApiRequest confirmed
  ↓
  [8] ApiExecutionOrchestrator executes:
- ServerManager.ensureServerRunning() → baseUrl
- Build ExecutionPlan (HttpCallStep)
- ExecutionEngine.run(plan, context)
  ↓
  [9] ResultPresenter updates ToolWindow


```
---
## 5. PSI 분석(EndpointExtractor) 설계

### 5.1 역할 범위

EndpointExtractor는 PSI를 해석하여 “엔드포인트 정의”를 `EndpointDescriptor`로 뽑는다.

- HTTP Method 추출
- PathTemplate 추출
- PathVariable 이름 추출
- QueryParam 이름 추출
- RequestBody 타입 이름 추출(fully-qualified name이 이상적)

**중요:** EndpointExtractor는 “입력값 생성”을 하지 않는다.  
입력값 생성은 Generator(Body/Param)에서 수행한다.

---

### 5.2 입력/출력

#### 입력
- `PsiMethod` (Controller 메서드 PSI)

#### 출력
- `com.apirunner.core.model.EndpointDescriptor`

Core에 정의된 모델을 그대로 사용한다.

```kotlin
// apirunner-core:model/EndpointDescriptor.kt
data class EndpointDescriptor(
    val httpMethod: String,
    val pathTemplate: String,
    val pathVariableNames: List<String>,
    val queryParamNames: List<String>,
    val requestBodyType: String?
)
```

### 5.3 추출 규칙(우선순위)

### 5.3.1 HTTP Method / PathTemplate

지원 어노테이션(우선 MVP):

- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`
- `@RequestMapping(method=..., value=...)`

**HTTP Method 규칙**

1. `@GetMapping` → GET
2. `@PostMapping` → POST
3. `@RequestMapping(method=...)` → 해당 값
4. 다중 method인 경우:
    - MVP: 첫 번째만 사용
    - 확장: 사용자 선택 UI

**PathTemplate 규칙**

- class-level mapping + method-level mapping을 결합해야 한다.

예)

```java
@RequestMapping("/api")classUserController {@GetMapping("/users/{id}")
  ...
}
```

→ pathTemplate = `/api/users/{id}`

MVP 구현에서는:

- class-level mapping 존재하면 prefix로 결합
- 없으면 method-level만 사용

---

### 5.3.2 PathVariable 이름 추출

대상:

- `@PathVariable`
- 이름이 없으면 파라미터명 사용

예)

```java
@GetMapping("/users/{id}")
Userget(@PathVariable Long id)
```

→ pathVariableNames = `["id"]`

---

### 5.3.3 QueryParam 이름 추출

대상:

- `@RequestParam`
- 이름이 없으면 파라미터명 사용

예)

```java
@GetMapping("/users")
List<User>list(@RequestParamint page,@RequestParamint size)
```

→ queryParamNames = `["page","size"]`

---

### 5.3.4 RequestBody 타입 추출

대상:

- `@RequestBody`

예)

```kotlin
@PostMapping("/users")funcreate(@RequestBody req:CreateUserRequest): ...
```

→ requestBodyType = `"com.example.CreateUserRequest"` (가능하면 FQN)

**추출 방식(권장 흐름)**

- `PsiParameter.type`에서 `PsiClassType.resolve()` → `PsiClass.qualifiedName` 획득
- Kotlin의 경우 UAST(`UMethod`) 활용 고려(추후)

**MVP 제한**

- Generic type은 우선 raw type으로
- Kotlin data class는 FQN 추출이 우선 목표

---

### 5.4 EndpointExtractor 결과 예시

입력 코드:

```java
@RequestMapping("/api")classUserController {@PostMapping("/users/{id}")
  Userupdate(@PathVariable Long id,@RequestParamint version,@RequestBody UpdateUserRequest body
  ) { ... }
}
```

출력:

```kotlin
EndpointDescriptor(
  httpMethod ="POST",
  pathTemplate ="/api/users/{id}",
  pathVariableNames = listOf("id"),
  queryParamNames = listOf("version"),
  requestBodyType ="com.example.UpdateUserRequest"
)
```

---

## 6. 기본 ApiRequest 생성(Generator 연결) 설계

IDE는 `EndpointDescriptor`를 기반으로 “기본값이 채워진 ApiRequest”를 만든다.

### 6.1 생성 규칙

- pathVariables: 이름 목록 → 기본값 생성 (`"1"` 등)
- queryParams: 이름 목록 → 기본값 생성 (`"1"` 등)
- headers: 기본 헤더 자동 세팅
    - `Content-Type: application/json` (body가 있을 때)
- body: requestBodyType이 있으면 generator로 JSON 생성
    - 없으면 null

### 6.2 구성 흐름

```
EndpointDescriptor
  ↓
DefaultParamGenerator.generatePathVars(names)
DefaultParamGenerator.generateQueryParams(names)
BodyGenerator.generate(typeName)
  ↓ApiRequest(method, pathTemplate, vars, query, headers, body)
```

---

## 7. Input UI(RequestDialog) 설계

### 7.1 목적

- “키는 자동 생성”
- “값은 사용자가 빠르게 수정”
- Run을 누르면 ApiRequest 확정

### 7.2 탭 구조

- Tab 1: Path / Query
- Tab 2: Headers
- Tab 3: Body (JSON Editor)

### 7.3 결과

Dialog는 최종 `ApiRequest`를 반환한다.

---

## 8. Orchestration 설계(ApiExecutionOrchestrator)

IDE 계층에서 전체 실행을 조합한다.

### 8.1 책임

- ExecutionContext 생성
- ServerManager 통해 baseUrl 확보
- HttpExecutor/ExecutionEngine 조합
- ExecutionPlan 생성 및 실행
- 결과를 Presenter로 전달

### 8.2 MVP Plan 구성

- `HttpCallStep(apiRequest, httpExecutor)` 1개로 시작
- (확장) EnsureServerRunningStep을 Core Step으로 옮기지 않고 IDE에서 선처리(단순화)

---

## 9. 서버 실행(ServerManager) 설계

### 9.1 책임

- Controller가 속한 모듈 탐색
- SpringBootApplication 클래스 탐색
- RunConfiguration 생성/재사용
- 서버 실행
- ProcessHandler 로그 감시
- PortParser로 포트 추출
- baseUrl 반환: `http://localhost:{port}`

### 9.2 재사용 정책(MVP 기본)

- 이미 실행 중이면 재사용
- 꺼져 있으면 실행

### 9.3 포트 파싱 실패 시

- fallback: 포트를 직접 입력 받는 작은 Dialog
- 성공한 포트는 프로젝트 단위로 저장(추후)

---

## 10. 결과 출력(Result ToolWindow) 설계

### 10.1 표시 항목(MVP)

- Status
- Duration(ms)
- Headers
- Body(Pretty JSON)

### 10.2 ResultPresenter 역할

- ToolWindow UI 컴포넌트를 찾아 업데이트
- 예외 발생 시 에러 메시지도 출력

---

## 11. 리스크 및 대응(IDE 관점)

### R1. PSI 파싱(특히 Kotlin) 난이도

- MVP: Java + 기본 Kotlin 케이스 우선
- 확장: UAST 기반 파싱 도입

### R2. 멀티 모듈에서 실행 대상 앱 모호

- 최초 실행 시 앱 선택 UI + 저장(Phase 2)

### R3. 포트 파싱 실패

- 로그 패턴 다중 지원 + fallback 포트 입력

### R4. IDE 모듈 비대화

- Orchestrator/PSI/Server/UI로 패키지 분리
- 순수 로직은 core/http/generator로 이동

---

## 12. 확장(Scenario) 연결 지점

Scenario가 추가되면 IDE는 다음을 확장한다:

- ToolWindow에 Scenario 실행 UI 추가
- Scenario 정의를 ExecutionPlan(Step 리스트)로 변환
- ExtractVariableStep 등 Core Step 추가(Phase 2)

IDE는 여전히 “Plan을 만들어 Engine에 넘기는 역할”만 한다.

---

## 13. 다음 문서

- 08_UI_UX_DESIGN.md
    - Dialog 상세 UI 구성(테이블/JSON editor)
    - ToolWindow 정보 구조
    - 반복 실행 UX(히스토리/재실행)