# 04_CORE_DESIGN.md

# Spring Controller API Runner — Core Design (Execution Engine)

---

## 1. 목적

이 문서는 `apirunner-core`의 구조를 확정한다.

- 핵심 도메인 모델(ApiRequest/ApiResponse/EndpointDescriptor)
- 실행 엔진(ExecutionEngine/Plan/Step/Context)
- 템플릿 치환(TemplateResolver)
- 에러 처리 방침(최소한의 실패 모델)
- 시나리오 확장 대비를 위한 최소 장치

이 문서는 **구현 가능한 수준**의 설계를 제공하며,
IDE/HTTP/Generator 구현에 대한 의존은 포함하지 않는다.

---

## 2. 설계 원칙

1. Core는 IntelliJ API를 모른다.
2. Core는 OkHttp 같은 HTTP 구현을 모른다.
3. Core는 타입 탐색/JSON 생성 로직을 모른다.
4. 확장(Scenario)을 대비하되, MVP는 단일 실행 중심이다.
5. 인터페이스 경계를 통해 HTTP/Generator/IDE를 교체 가능하게 한다.

---

## 3. 패키지 구조(고정)

```
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
├── util/
│ └── TemplateResolver.kt
└── error/
├── CoreError.kt
└── CoreException.kt

```
---

## 4. 도메인 모델

### 4.1 EndpointDescriptor

IDE(PSI)가 추출한 “엔드포인트 정의”를 Core로 전달하기 위한 DTO.

```kotlin
data class EndpointDescriptor(
    val httpMethod: String,             // GET/POST/PUT/DELETE/PATCH
    val pathTemplate: String,            // "/users/{id}"
    val pathVariableNames: List<String>, // ["id"]
    val queryParamNames: List<String>,   // ["page","size"]
    val requestBodyType: String?         // "com.example.CreateUserRequest"
)
```
Core는 requestBodyType를 직접 해석하지 않는다.

Generator가 이를 이용해 JSON을 생성한다(IDE에서 호출).

### 4.2 ApiRequest
실제 HTTP 호출에 필요한 최소 모델.
(IDE가 생성/편집 후 엔진에 전달)

```kotlin
data class ApiRequest(
    val method: String,
    val pathTemplate: String,

    val pathVariables: MutableMap<String, String> = mutableMapOf(),
    val queryParams: MutableMap<String, String> = mutableMapOf(),
    val headers: MutableMap<String, String> = mutableMapOf(),

    var body: String? = null
)
```
설계 포인트
* pathTemplate는 템플릿 형태를 유지한다.
    * MVP에서는 {id} 기반 치환이 필요
    * 확장에서는 {{var}} 기반 치환을 사용
* headers/body는 템플릿 치환 대상이 될 수 있다.

### 4.3 ApiResponse
HTTP 호출 결과 표현.

```kotlin
data class ApiResponse(
    val status: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val durationMs: Long = 0
)
```

## 5. 실행 엔진 모델
### 5.1 ExecutionContext
실행 전/중/후 공유되는 상태 저장소.

```kotlin


class ExecutionContext {

    // Scenario 확장 대비: step 간 공유 변수
    val variables: MutableMap<String, String> = mutableMapOf()

    // 전역 헤더(예: Authorization, Trace-Id 등)
    val globalHeaders: MutableMap<String, String> = mutableMapOf()

    // 서버 baseUrl (IDE가 서버 실행/포트 확보 후 설정)
    var baseUrl: String = ""

    // 직전 응답 저장(디버깅/후속 step 활용)
    var lastResponse: ApiResponse? = null
}
```
설계 포인트
* JWT/세션 같은 “인증 상태”는
  * MVP에서는 http 모듈이 관리할 수 있으나,
  * Scenario 확장 대비 variables["jwt"] 같은 형태로 저장 가능해야 한다.
* Context는 실행 엔진 관점의 “공유 메모리”다.

### 5.2 Step
실행 단위. 실행 엔진은 Step 리스트를 순차 실행한다.

```kotlin

interface Step {
    fun execute(context: ExecutionContext)
}
```
### 5.3 ExecutionPlan
Step들의 실행 순서(Workflow).

```kotlin

data class ExecutionPlan(
    val steps: List<Step>
)
```
MVP는 2~3 step이면 충분:

* EnsureServerRunningStep (IDE에서 구현, Core에선 개념만)

* HttpCallStep (Core 구현 가능, HttpExecutor 주입)

* (선택) CaptureAuthStateStep (http 모듈 또는 추가 step)

### 5.4 ExecutionEngine
Plan 실행기.
```kotlin

class ExecutionEngine {

    fun run(plan: ExecutionPlan, context: ExecutionContext) {
        for (step in plan.steps) {
            step.execute(context)
        }
    }
}
```
설계 포인트
* MVP에서는 단순 순차 실행.

* 확장 시:

  * 실패 정책(중단/continue)
  * step 결과 리포트
  * 타임아웃/취소 토큰
  * 병렬 실행 등을 추가할 수 있다.
  

## 6. HTTP 경계 인터페이스
### 6.1 HttpExecutor
HTTP 호출 구현을 Core에서 추상화한다.

```kotlin

interface HttpExecutor {
    fun execute(request: ApiRequest, context: ExecutionContext): ApiResponse
}
```
구현체는 apirunner-http가 제공(OkHttpExecutor 등)

## 7. Core 기본 Step 구현
### 7.1 HttpCallStep
HttpExecutor를 통해 HTTP 호출을 수행한다.

```kotlin


class HttpCallStep(
    private val request: ApiRequest,
    private val httpExecutor: HttpExecutor
) : Step {

    override fun execute(context: ExecutionContext) {
        val resolved = TemplateResolver.resolve(request, context)
        val response = httpExecutor.execute(resolved, context)
        context.lastResponse = response
    }
}
``` 
설계 포인트
* “치환된 request”를 만들고 실행한다.
* Step 실행 결과는 context에 저장한다.

## 8. TemplateResolver 설계(확장 대비 핵심)
### 8.1 지원 범위(MVP)
* {pathVariable} 치환
* {{variable}} 치환(Scenario 대비 — MVP에서는 사용 빈도 낮음)

### 8.2 동작 규칙
* PathTemplate에서 {id}는 request.pathVariables["id"]로 치환
* Body/Headers/Query에서 {{jwt}} 같은 형태는 context.variables["jwt"]로 치환

### 8.3 구현 예시
```kotlin


object TemplateResolver {

    fun resolve(request: ApiRequest, context: ExecutionContext): ApiRequest {

        val resolvedPath = resolvePathVariables(request.pathTemplate, request.pathVariables)
        val resolvedQuery = request.queryParams.mapValues { (_, v) -> resolveVars(v, context) }.toMutableMap()
        val resolvedHeaders = request.headers.mapValues { (_, v) -> resolveVars(v, context) }.toMutableMap()
        val resolvedBody = request.body?.let { resolveVars(it, context) }

        // context.globalHeaders를 요청 headers에 병합 (요청이 우선)
        val mergedHeaders = context.globalHeaders.toMutableMap().apply {
            putAll(resolvedHeaders)
        }

        return request.copy(
            pathTemplate = resolvedPath,
            queryParams = resolvedQuery,
            headers = mergedHeaders,
            body = resolvedBody
        )
    }

    private fun resolvePathVariables(template: String, vars: Map<String, String>): String {
        var out = template
        vars.forEach { (k, v) ->
            out = out.replace("{$k}", v)
        }
        return out
    }

    private fun resolveVars(text: String, context: ExecutionContext): String {
        var out = text
        context.variables.forEach { (k, v) ->
            out = out.replace("{{${k}}}", v)
        }
        return out
    }
}
```
## 9. 에러 처리(최소 방침)
### 9.1 CoreError / CoreException
Core는 최소한의 오류 모델만 제공한다.
```kotlin


sealed class CoreError {
    data class TemplateResolutionFailed(val reason: String) : CoreError()
    data class StepFailed(val stepName: String, val reason: String) : CoreError()
}

class CoreException(val error: CoreError) : RuntimeException(error.toString())
```
MVP에서는
* 엔진은 기본적으로 “예외 발생 시 중단” 전략을 사용한다.
* IDE는 예외를 잡아 ToolWindow에 표시한다.

## 10. 테스트 전략(core)
### 10.1 ExecutionEngine 순차 실행 테스트
* step 2개가 순차로 실행되는지
* context 변수가 step 간 전달되는지

### 10.2 TemplateResolver 테스트
* {id} 치환 동작

* {{jwt}} 치환 동작

* globalHeaders 병합 동작

### 10.3 HttpCallStep 테스트
* Fake HttpExecutor로 응답 주입
* context.lastResponse 업데이트 확인

## 11. 향후 확장(Scenario) 시 core 변경 최소화
Scenario를 추가해도 core를 크게 바꾸지 않기 위해:

* Plan/Step 구조 유지

* Context.variables 적극 활용

* TemplateResolver를 공용으로 유지

추가될 Step 예시:

* ExtractVariableStep (response에서 값 추출)

* DelayStep

* ConditionalStep

* RepeatStep

## 12. 다음 문서
* 05_GENERATOR_DESIGN.md
  * 자동 Body/Param 생성 규칙(타입별 기본값, depth 제한, 확장 전략)

* 06_HTTP_DESIGN.md

  * OkHttpExecutor, CookieJar, AuthInterceptor, JWT 저장 전략
