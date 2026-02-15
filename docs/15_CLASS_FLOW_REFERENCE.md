# 15_CLASS_FLOW_REFERENCE.md

이 문서는 **현재 코드 기준**으로 `apirunner-core`, `apirunner-generator`, `apirunner-http`, `apirunner-ide`의 모든 main 클래스/오브젝트를 문서화한다.

목표는 두 가지다.
1. 클래스별 책임과 연결 지점을 한 번에 파악한다.
2. "왜 이렇게 설계했는지"(설계 의도)를 유지보수 기준으로 남긴다.

---

## 1) 전체 실행 흐름 (현재 실제 런타임 경로)

1. `ApiRunnerRunLineMarkerContributor`가 Spring mapping 메서드에 실행 아이콘을 노출한다.
2. 사용자가 거터 아이콘을 누르면 `ApiRunnerExecutor.run(PsiMethod)`가 시작된다.
3. `EndpointExtractor`가 endpoint 메타(`EndpointDescriptor`)를 만든다.
4. `DefaultBodyGenerator`/`DefaultParamGenerator`로 초기 요청을 만든다.
5. `ApiRunnerProjectService`에서 endpoint별 draft를 병합하고 `ApiRequestDialogWrapper`로 사용자 수정값을 받는다.
6. `ApiExecutionOrchestrator`가 `ExecutionPlan(HttpCallStep)`을 만들어 `ExecutionEngine`로 실행한다.
7. `HttpCallStep`이 `TemplateResolver`로 요청을 최종 치환하고 `HttpExecutor`(실제 구현: `OkHttpExecutor`)를 호출한다.
8. `OkHttpExecutor`가 HTTP 호출 후 `ApiResponse`를 만들고 JWT를 재추출해 `ExecutionContext.variables`에 저장한다.
9. `ResultPresenter`가 요청/응답을 `ResultViewModel`로 변환하고 `ApiRunnerResultService`에 전달한다.
10. ToolWindow가 status/request/response를 렌더링한다.

---

## 2) 모듈 의존 구조

- `apirunner-core`: 순수 실행 모델/엔진, 다른 모듈 의존 없음
- `apirunner-generator` -> `apirunner-core`
- `apirunner-http` -> `apirunner-core`
- `apirunner-ide` -> `apirunner-core`, `apirunner-generator`, `apirunner-http`

설계 의도:
- Core를 순수하게 유지해서 IDE/HTTP 교체 비용을 낮추기 위함.
- IDE는 조립자(오케스트레이션) 역할만 하고 도메인 로직은 하위 모듈로 위임.

---

## 3) apirunner-core 클래스 문서

### `ExecutionContext`
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/engine/ExecutionContext.kt`
- 역할: 실행 상태 저장소.
- 주요 상태:
  - `variables`: 템플릿 치환/토큰 공유(jwt 등)
  - `globalHeaders`: 공통 헤더
  - `baseUrl`: 서버 베이스 URL
  - `lastRequest`: 마지막으로 실제 전송된 요청
  - `lastResponse`: 마지막 응답
- 의도: Step 간 데이터 전달을 위한 단일 컨텍스트.

### `ExecutionEngine`
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/engine/ExecutionEngine.kt`
- 역할: `ExecutionPlan.steps`를 순차 실행.
- 의도: 복잡한 정책 없이 deterministic 실행을 제공하는 최소 엔진.

### `ExecutionPlan`
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/engine/ExecutionPlan.kt`
- 역할: 실행할 `Step` 목록 모델.
- 의도: 실행 단위를 조립 가능한 구조로 만들기 위함.

### `Step` (interface)
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/engine/Step.kt`
- 역할: 실행 단계의 공통 계약.
- 의도: 서버 확보, HTTP 호출, 사전/사후 처리 등 단계 확장용 경계.

### `HttpCallStep`
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/engine/HttpCallStep.kt`
- 역할:
  - `TemplateResolver.resolve()`로 요청 최종화
  - `context.lastRequest` 저장
  - `HttpExecutor.execute()` 실행
  - `context.lastResponse` 저장
- 의도: 템플릿 치환과 transport 호출을 분리하면서도 동일 Step에서 원자적으로 처리.

### `HttpExecutor` (interface)
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/http/HttpExecutor.kt`
- 역할: HTTP 실행 추상화.
- 구현체: `apirunner-http`의 `OkHttpExecutor`.
- 의도: Core가 HTTP 라이브러리를 몰라도 되게 하기 위함.

### `ApiRequest`
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/model/ApiRequest.kt`
- 역할: 실행 대상 요청 모델(method/path/query/header/body).
- 의도: IDE 입력/템플릿 치환/HTTP 실행이 공통으로 쓰는 표준 모델.

### `ApiResponse`
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/model/ApiResponse.kt`
- 역할: 응답 모델(status/headers/body/duration).
- 의도: UI 표시와 후속 Step 활용을 위한 transport-neutral 응답 표현.

### `EndpointDescriptor`
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/model/EndpointDescriptor.kt`
- 역할: PSI 분석 결과의 중립 모델.
- 의도: IDE 분석 결과를 생성기/실행기로 안전하게 넘기기 위한 DTO.

### `TemplateResolver` (object)
- 파일: `apirunner-core/src/main/kotlin/com/apirunner/core/util/TemplateResolver.kt`
- 역할:
  - path 변수 `{id}` 치환
  - `{{var}}` 컨텍스트 변수 치환(query/header/body)
  - `globalHeaders + request.headers` 병합
- 의도: 요청 최종화를 한 지점으로 모아 사이드이펙트/중복을 줄임.

### `CoreError`, `CoreException`
- 파일:
  - `apirunner-core/src/main/kotlin/com/apirunner/core/error/CoreError.kt`
  - `apirunner-core/src/main/kotlin/com/apirunner/core/error/CoreException.kt`
- 역할: Core 도메인 오류 모델.
- 현재 상태: main 흐름에서 적극 사용되지는 않음(확장 대비).

---

## 4) apirunner-generator 클래스 문서

### `BodyGenerator` (interface)
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/BodyGenerator.kt`
- 역할: 타입명 기반 body 생성 계약.

### `DefaultBodyGenerator`
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/DefaultBodyGenerator.kt`
- 역할:
  - typeName을 순회해 객체 노드 생성
  - primitive/enum/collection/object 처리
  - pretty JSON 문자열 반환
- 의도: 사용자 입력 초안 생성을 자동화해 첫 실행 진입 장벽을 낮춤.

### `GeneratorConfig`
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/GeneratorConfig.kt`
- 역할: 깊이/컬렉션 크기 등 생성 정책.
- 주의: 기본 `collectionSize=0`이라 리스트는 기본적으로 빈 배열.

### `TypeInspector` (interface)
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/TypeInspector.kt`
- 역할: 타입 시스템 질의 추상화(primitive/enum/field/collection).

### `ReflectionTypeInspector`
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/ReflectionTypeInspector.kt`
- 역할: reflection 기반으로 필드/enum/컬렉션 제네릭 추출.
- 의도: IDE/PSI에 의존하지 않고 JVM reflection만으로 동작.

### `DefaultValueStrategy` (interface)
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/DefaultValueStrategy.kt`
- 역할: primitive 기본값 정책 추상화.

### `SimpleDefaultValueStrategy`
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/SimpleDefaultValueStrategy.kt`
- 역할: 문자열/숫자/불린 기본값 제공.

### `DefaultParamGenerator` (object)
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/DefaultParamGenerator.kt`
- 역할: path/query 파라미터 기본값 생성(`"1"`).

### `TypeField`
- 파일: `apirunner-generator/src/main/kotlin/com/apirunner/generator/model/TypeField.kt`
- 역할: 필드 메타(name/type) DTO.

---

## 5) apirunner-http 클래스 문서

### `OkHttpClientProvider` (object)
- 파일: `apirunner-http/src/main/kotlin/com/apirunner/http/OkHttpClientProvider.kt`
- 역할: timeout, redirect, cookieJar, auth interceptor가 설정된 OkHttpClient 생성.
- 의도: 클라이언트 정책을 한 곳에서 구성.

### `HttpConfig`
- 파일: `apirunner-http/src/main/kotlin/com/apirunner/http/HttpConfig.kt`
- 역할: 클라이언트 설정 값 모델(timeout 등).

### `AuthInterceptor`
- 파일: `apirunner-http/src/main/kotlin/com/apirunner/http/AuthInterceptor.kt`
- 역할:
  - 요청에 Authorization이 이미 있으면 그대로 통과
  - 없고 `context.variables["jwt"]`가 있으면 `Bearer` 자동 주입
- 의도: 로그인 후 후속 호출 자동화를 위한 최소한의 편의.

### `SessionCookieJar`
- 파일: `apirunner-http/src/main/kotlin/com/apirunner/http/SessionCookieJar.kt`
- 역할: host 단위 쿠키 저장/재사용.
- 의도: 세션 기반 API 연속 호출 지원.

### `JwtExtractor` (object)
- 파일: `apirunner-http/src/main/kotlin/com/apirunner/http/JwtExtractor.kt`
- 역할: 응답 JSON 문자열에서 `accessToken`/`token`/`jwt` 키를 heuristic으로 추출.
- 의도: 엄격한 계약 없이도 MVP에서 빠르게 토큰 연쇄 호출을 지원.

### `OkHttpExecutor`
- 파일: `apirunner-http/src/main/kotlin/com/apirunner/http/OkHttpExecutor.kt`
- 역할:
  - `HttpExecutor` 구현체
  - URL 조립(baseUrl + path + query)
  - 일반 JSON body / multipart body 생성
  - 호출 후 응답 모델 변환 + JWT 재추출
- multipart 규칙:
  - 본문 `key=value` 줄 파싱
  - `@path`는 file part
  - `{...}`/`[...]`는 JSON part
  - 나머지는 text part
- 의도: 요청 모델(`ApiRequest`)을 transport 요청으로 안정 변환하는 adapter.

### `HttpError`
- 파일: `apirunner-http/src/main/kotlin/com/apirunner/http/HttpError.kt`
- 역할: HTTP 계층 예외 모델(`NetworkFailure`).

---

## 6) apirunner-ide 클래스 문서

### A. Entry / Marker

### `ApiRunnerRunLineMarkerContributor`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/marker/ApiRunnerRunLineMarkerContributor.kt`
- 역할:
  - leaf PSI만 처리
  - Spring mapping 메서드에 실행 아이콘 노출
  - 클릭 시 `ApiRunnerExecutor.run()` 호출
- 의도: 에디터 UX 시작점 + 성능 경고 회피.

### `ControllerRunLineMarker`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/marker/ControllerRunLineMarker.kt`
- 역할: `ControllerMethodInfo` 기반 실행 가능 판정.
- 현재 상태: main 런타임에서는 직접 미사용, 테스트/레거시 보조 클래스.

### B. PSI / Descriptor

### `ControllerMethodInfo`, `MethodParameter`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/psi/ControllerMethodInfo.kt`
- 역할: annotation 문자열 기반 중간 표현.

### `EndpointExtractor`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/psi/EndpointExtractor.kt`
- 역할:
  - PsiMethod/UMethod/ControllerMethodInfo 입력 지원
  - class/method mapping path 조합
  - request method 추론
  - path/query/body 타입 메타 추출
- 의도: IntelliJ PSI 복잡도를 descriptor 변환 단계로 격리.

### C. Runtime / Orchestration

### `ApiRunnerExecutor`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerExecutor.kt`
- 역할(핵심 조립자):
  - endpoint 추출
  - multipart 여부 판정 및 body 템플릿 생성
  - draft 병합/저장
  - 요청 다이얼로그 표시 + 검증
  - multipart 상대경로를 프로젝트 절대경로로 정규화
  - 백그라운드에서 orchestrator 실행
- 의도: IDE-specific 정책(다이얼로그/저장/포트 입력)을 집중 관리.

### `ApiExecutionOrchestrator`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/orchestration/ApiExecutionOrchestrator.kt`
- 역할:
  - baseUrl 확보(`ServerManager.ensureServerRunning()`)
  - `ExecutionPlan(HttpCallStep)` 실행
  - 성공/실패 결과를 `ResultPresenter`로 전달
- 의도: 실행 파이프라인의 경계(실행/표시 분리).

### `ApiRunnerProjectService`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerProjectService.kt`
- 역할:
  - 프로젝트 단위 상태 저장소
  - `ExecutionContext`, `SessionCookieJar` 유지
  - 마지막 포트, 선택 앱 클래스, endpoint draft 저장/복원
- 의도: 실행 세션을 프로젝트 생명주기에 맞춰 지속.

### D. Server

### `ServerProcess` (interface)
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/server/ServerProcess.kt`
- 역할: 서버 포트 획득 계약.

### `IntellijServerProcess`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/IntellijServerProcess.kt`
- 역할: 현재는 저장된 `lastPort`를 서버 포트로 간주하는 간단 구현.
- 의도: 실제 서버 제어가 붙기 전 MVP 형태의 process adapter.

### `ServerManager`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/server/ServerManager.kt`
- 역할: process 탐지 실패 시 fallback 포트 입력으로 baseUrl 생성.

### `PortParser`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/server/PortParser.kt`
- 역할: 로그 라인에서 포트 정규식 파싱.
- 현재 상태: main 흐름에서 직접 미사용(테스트/예비 유틸).

### E. UI

### `ApiRequestDialogWrapper`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ApiRequestDialogWrapper.kt`
- 역할:
  - path/query/header/body 입력 UI
  - `RequestDialog` 검증 연결
  - 입력값을 `ApiRequest`로 재조립
- 의도: 실행 전 사용자 개입 지점.

### `RequestDialog`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/RequestDialog.kt`
- 역할:
  - path variable 필수값 검증
  - JSON body 형식 검증
  - multipart 라인 형식 및 파일 경로 정책 검증
- 의도: 요청 실패를 네트워크 호출 전에 차단.

### `ApiRunnerResultService`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ApiRunnerResultService.kt`
- 역할:
  - ToolWindow content 구성
  - status/request/response 렌더링
  - request/response pretty formatting 및 길이 제한
- 의도: 결과 가시성 강화(응답뿐 아니라 요청 재현 가능성 확보).

### `ResultPresenter`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ResultPresenter.kt`
- 역할:
  - 도메인 결과(`ApiResponse`, `ApiRequest`, baseUrl)를 UI 모델(`ResultViewModel`)로 변환
  - 요청 URL 조립 및 query 인코딩
- 의도: UI 포맷 로직을 실행 로직에서 분리.

### `ResultViewModel`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ResultViewModel.kt`
- 역할: 렌더링 전용 모델(status/duration/response + request snapshot + error).

### `ResultSink` (interface)
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ResultSink.kt`
- 역할: 결과 업데이트 추상화.
- 구현:
  - 런타임: `ApiRunnerExecutor` 내부 익명 sink -> `ApiRunnerResultService`
  - 테스트: `ResultToolWindowFactory`

### `ResultToolWindowFactory`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ResultToolWindowFactory.kt`
- 역할: 마지막 결과를 메모리에 보관하는 test sink 역할.
- 현재 상태: 실제 ToolWindow factory 아님(이름과 역할 괴리 존재).

### `ApiRunnerToolWindowFactory`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ApiRunnerToolWindowFactory.kt`
- 역할: plugin.xml에서 등록되는 실제 ToolWindow factory.

### F. Other

### `RunControllerAction`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/action/RunControllerAction.kt`
- 역할: `ControllerMethodInfo` -> `ApiRequest` 변환 후 orchestrator 실행.
- 현재 상태: main 런타임에서 미사용(초기/대체 경로로 남아있는 클래스).

### `ModuleResolver`
- 파일: `apirunner-ide/src/main/kotlin/com/apirunner/ide/util/ModuleResolver.kt`
- 역할: 경로 문자열에서 모듈명 추출.
- 현재 상태: main 런타임에서 미사용(예비 유틸).

---

## 7) "왜 이렇게 작성했는가" 요약

1. Core/HTTP/Generator/IDE를 분리한 이유
- IntelliJ API, HTTP transport, reflection 생성 로직을 분리해 변경 영향을 국소화하려는 의도.

2. `ExecutionContext` 중심 설계 이유
- JWT, 공통 헤더, 마지막 요청/응답처럼 "호출 간 공유 상태"를 Step 간에 일관되게 전달하기 위해서.

3. `ApiRunnerExecutor`에 IDE 정책을 집중한 이유
- 입력 UI/저장 정책/포트 입력/multipart 경로 정규화는 도메인 로직이 아니라 IDE 통합 로직이기 때문.

4. `ResultPresenter`/`ResultViewModel` 분리 이유
- 실행 성공/실패 처리와 UI 포맷팅을 분리해 테스트 가능성과 가독성을 높이기 위해서.

5. 요청 정보까지 결과에 표시한 이유
- 응답만 보면 재현이 어려워서, 요청 method/url/header/body를 함께 보여 디버깅 시간을 줄이기 위함.

---

## 8) 유지보수 우선 정리 포인트 (권장)

1. 미사용/레거시 클래스 정리
- 후보: `RunControllerAction`, `ControllerRunLineMarker`, `ModuleResolver`, `PortParser`, `ResultToolWindowFactory` (네이밍 포함)

2. 테마 일관성 정책 문서화
- 모든 UI 컴포넌트는 IntelliJ LAF/Editor scheme를 따르고 하드코딩 색상을 금지.

3. 실행 정책 문서 최신화
- 현재 결과창은 요청/응답 동시 표시이므로 기존 문서(`14_MVP1_CODE_HANDOVER.md`) 일부와 차이가 있음.
