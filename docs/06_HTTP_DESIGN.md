# 06_HTTP_DESIGN.md
# Spring Controller API Runner — HTTP Design

---

# 1. 목적

이 문서는 `apirunner-http` 모듈의 설계를 정의한다.

HTTP 모듈은 다음을 책임진다:

- HTTP 요청 실행
- 세션(Cookie) 유지
- JWT 자동 주입
- JWT 자동 추출 (MVP heuristic)
- 요청/응답 시간 측정
- 향후 확장(리트라이, 타임아웃, 시나리오 지원)을 고려한 구조 제공

이 문서는 구현 세부가 아닌 **구조 고정 문서**다.

---

# 2. 설계 원칙

1. Core는 HTTP 구현을 모른다.
2. HTTP는 IntelliJ API를 모른다.
3. 인증 자동화는 HTTP 레이어 책임이다.
4. 세션 유지 및 JWT 주입은 전역 Client 단위에서 처리한다.
5. 확장(Scenario, Retry, Timeout, Logging) 대비 구조를 갖춘다.

---

# 3. 모듈 구조

```
apirunner-http/
└── src/main/kotlin/com/apirunner/http/
├── OkHttpExecutor.kt
├── OkHttpClientProvider.kt
├── SessionCookieJar.kt
├── AuthInterceptor.kt
├── JwtExtractor.kt
├── HttpConfig.kt
└── HttpError.kt
```

---

# 4. 전체 흐름
```
ExecutionEngine
↓
HttpCallStep
↓
HttpExecutor (interface)
↓
OkHttpExecutor (implementation)
↓
OkHttpClient
├── CookieJar
├── AuthInterceptor
└── Network call
↓
ApiResponse 반환
↓
ExecutionContext 업데이트

```
---
# 5. 핵심 인터페이스

Core에서 정의된 인터페이스:

```kotlin
interface HttpExecutor {
    fun execute(request: ApiRequest, context: ExecutionContext): ApiResponse
}
```
HTTP 모듈은 이를 구현한다.
## 6. HttpConfig 설계

HTTP 관련 설정을 명시적으로 관리한다.

```kotlin
data class HttpConfig(
val connectTimeoutMs: Long = 3000,
val readTimeoutMs: Long = 5000,
val writeTimeoutMs: Long = 5000,
val followRedirects: Boolean = true,
val enableLogging: Boolean = false
)
```

MVP에서는 기본값 사용.

# 7. OkHttpClientProvider

전역 Client 생성 책임.

```kotlin
object OkHttpClientProvider {funcreate(
        context:ExecutionContext,
        config:HttpConfig = HttpConfig()
    ): OkHttpClient {return OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS)
            .followRedirects(config.followRedirects)
            .cookieJar(SessionCookieJar())
            .addInterceptor(AuthInterceptor(context))
            .build()
    }
}
```

### 설계 포인트

- CookieJar는 세션 유지 책임
- AuthInterceptor는 JWT 자동 주입
- ExecutionContext는 인증 상태 공유

---

# 8. SessionCookieJar 설계

세션 유지 전담.

```kotlin
classSessionCookieJar :CookieJar {privateval cookieStore = mutableMapOf<String, List<Cookie>>()overridefunsaveFromResponse(url: HttpUrl, cookies:List<Cookie>) {
        cookieStore[url.host] = cookies
    }overridefunloadForRequest(url: HttpUrl): List<Cookie> {return cookieStore[url.host] ?: emptyList()
    }
}
```

---

## 세션 전략

- Host 기준 쿠키 저장
- 로그인 이후 세션 자동 유지
- 동일 baseUrl 사용 시 세션 지속

---

# 9. AuthInterceptor 설계

JWT 자동 주입 전담.

```kotlin
classAuthInterceptor(privateval context: ExecutionContext
) : Interceptor {overridefunintercept(chain: Interceptor.Chain): Response {val original = chain.request()val builder = original.newBuilder()

        context.variables["jwt"]?.let {
            builder.addHeader("Authorization","Bearer $it")
        }return chain.proceed(builder.build())
    }
}
```

---

## JWT 주입 전략

1. ExecutionContext.variables["jwt"] 사용
2. 존재 시 Authorization 자동 삽입
3. 사용자 입력 헤더가 우선 (중복 방지 가능)

---

# 10. OkHttpExecutor 설계

```kotlin
classOkHttpExecutor(privateval client: OkHttpClient
) : HttpExecutor {overridefunexecute(
        request:ApiRequest,
        context:ExecutionContext
    ): ApiResponse {val fullUrl = buildUrl(context.baseUrl, request)val requestBuilder = Request.Builder()
            .url(fullUrl)

        request.headers.forEach { (k, v) ->
            requestBuilder.addHeader(k, v)
        }val okRequest =when (request.method.uppercase()) {"GET" -> requestBuilder.get().build()"POST" -> requestBuilder.post(
                request.body.orEmpty()
                    .toRequestBody("application/json".toMediaType())
            ).build()"PUT" -> requestBuilder.put(
                request.body.orEmpty()
                    .toRequestBody("application/json".toMediaType())
            ).build()"DELETE" -> requestBuilder.delete().build()else -> requestBuilder.build()
        }val start = System.currentTimeMillis()val response = client.newCall(okRequest).execute()val duration = System.currentTimeMillis() - startval bodyString = response.body?.string()// JWT 자동 추출
        JwtExtractor.extract(bodyString)?.let {
            context.variables["jwt"] = it
        }return ApiResponse(
            status = response.code,
            headers = response.headers.toMultimap()
                .mapValues { it.value.joinToString(",") },
            body = bodyString,
            durationMs = duration
        )
    }privatefunbuildUrl(baseUrl: String, request:ApiRequest): String {val queryString = request.queryParams.entries
            .joinToString("&") {"${it.key}=${it.value}" }returnif (queryString.isNotEmpty())"$baseUrl${request.pathTemplate}?$queryString"else"$baseUrl${request.pathTemplate}"
    }
}
```

---

# 11. JwtExtractor 설계 (MVP heuristic)

MVP는 간단한 방식 사용.

```kotlin
object JwtExtractor {funextract(body: String?): String? {if (body ==null)returnnullval patterns = listOf("accessToken","token","jwt")for (keyin patterns) {val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]+)\"")val match = regex.find(body)if (match !=null) {return match.groupValues[1]
            }
        }returnnull
    }
}
```

---

## 제한 사항

- JSONPath 미지원
- 복잡한 응답 구조는 탐지 실패 가능
- Scenario 확장 시 JSONPath 기반으로 교체 예정

---

# 12. 에러 처리 전략

## MVP 전략

- 네트워크 예외 → RuntimeException
- IDE에서 잡아서 ToolWindow에 표시
- HTTP 4xx/5xx는 정상 ApiResponse로 반환

---

## 확장 전략

향후 추가 가능:

- Retry 정책
- Circuit Breaker
- Timeout별 메시지 세분화

---

# 13. 확장 고려

### Scenario 지원 시

- HttpCallStep 여러 번 실행
- context.variables 기반 JWT 공유
- TemplateResolver와 연계

### Retry 전략 추가

- OkHttpInterceptor로 재시도 가능
- 또는 HttpExecutor wrapper 설계

---

# 14. 리스크

### R1. JWT heuristic 실패

→ 해결: JSONPath 기반 ExtractStep 추가

### R2. 기업망 프록시 문제

→ IDE 설정과 연동 고려 필요(후순위)

### R3. Body가 큰 경우 메모리 사용 증가

→ 스트리밍 지원 고려(확장)

---

# 15. 테스트 전략

- Cookie 저장/재사용 테스트
- Authorization 자동 주입 테스트
- JWT 추출 테스트
- GET/POST/PUT 요청 동작 테스트
- Timeout 설정 적용 테스트

---

# 16. 결론

HTTP 모듈은:

- 인증 자동화
- 세션 유지
- 요청 실행
- 응답 처리

를 전담하며,

Core와 IDE를 완전히 분리한 상태에서

확장 가능한 실행 레이어를 제공한다.

---

# 17. 다음 문서

- 07_IDE_ADAPTER_DESIGN.md
    - PSI 분석
    - ServerManager
    - Port Parsing
    - Orchestrator
    - Dialog/ToolWindow 연결 구조