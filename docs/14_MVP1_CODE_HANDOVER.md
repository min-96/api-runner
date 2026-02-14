# 14_MVP1_CODE_HANDOVER.md

이 문서는 MVP1 기준으로 구현된 코드의 유지보수 관점 핸드오버 문서다.
대상은 `apirunner-core`, `apirunner-generator`, `apirunner-http`, `apirunner-ide` 전체 모듈이다.

## 1. 현재 동작 정책(기준선)
- 서버 자동 시작/자동 재시작은 하지 않는다.
- 실행 시 포트 입력 팝업을 띄우고, 마지막 포트를 프로젝트 단위로 재사용한다.
- 결과창 응답 본문은 JSON이면 Pretty Print로 표시한다.
- `multipart/form-data`를 지원한다.
- multipart 파일 경로는 프로젝트 루트 기준 상대경로만 허용한다.
- 요청 draft는 엔드포인트(`METHOD + pathTemplate`)별 최근 1개만 저장/복원한다.

## 2. 전체 실행 흐름
1. 거터 화살표 클릭
2. 엔드포인트 분석(HTTP method/path/pathVariable/query/requestBodyType)
3. 초기 요청 생성(JSON 또는 multipart 템플릿)
4. 저장된 draft 병합 후 요청 다이얼로그 표시
5. 사용자 편집값 저장
6. multipart 파일 경로를 프로젝트 절대경로로 정규화
7. 포트 확보(기본 8080 입력 팝업)
8. HTTP 실행
9. 결과 ToolWindow 표시(상단 Status/Latency 20%, 하단 Result 80%)

## 3. 모듈별 구현

### 3.1 `apirunner-core`

핵심 파일
- `apirunner-core/src/main/kotlin/com/apirunner/core/model/ApiRequest.kt`
- `apirunner-core/src/main/kotlin/com/apirunner/core/model/ApiResponse.kt`
- `apirunner-core/src/main/kotlin/com/apirunner/core/model/EndpointDescriptor.kt`
- `apirunner-core/src/main/kotlin/com/apirunner/core/engine/ExecutionContext.kt`
- `apirunner-core/src/main/kotlin/com/apirunner/core/engine/ExecutionEngine.kt`
- `apirunner-core/src/main/kotlin/com/apirunner/core/engine/HttpCallStep.kt`
- `apirunner-core/src/main/kotlin/com/apirunner/core/util/TemplateResolver.kt`

책임
- 실행 모델(`ApiRequest`, `ApiResponse`, `EndpointDescriptor`)
- 실행 컨텍스트(`baseUrl`, `variables(jwt)`, `globalHeaders`, `lastResponse`)
- 실행 엔진(순차 Step 실행)
- 템플릿 치환
  - 경로 변수: `/users/{id}` + `pathVariables[id]`
  - 컨텍스트 변수: `{{jwt}}` 형태

유지보수 포인트
- Core는 IntelliJ/OkHttp 의존성이 없어야 한다.
- 템플릿 문법을 바꿀 경우 `TemplateResolver`와 테스트를 같이 수정해야 한다.

관련 테스트
- `apirunner-core/src/test/kotlin/com/apirunner/core/engine/ExecutionEngineTest.kt`
- `apirunner-core/src/test/kotlin/com/apirunner/core/engine/HttpCallStepTest.kt`
- `apirunner-core/src/test/kotlin/com/apirunner/core/util/TemplateResolverTest.kt`

### 3.2 `apirunner-generator`

핵심 파일
- `apirunner-generator/src/main/kotlin/com/apirunner/generator/DefaultBodyGenerator.kt`
- `apirunner-generator/src/main/kotlin/com/apirunner/generator/ReflectionTypeInspector.kt`
- `apirunner-generator/src/main/kotlin/com/apirunner/generator/SimpleDefaultValueStrategy.kt`
- `apirunner-generator/src/main/kotlin/com/apirunner/generator/DefaultParamGenerator.kt`

책임
- RequestBody 기본 JSON 생성
- Path/Query 기본값 생성(기본값 `"1"`)

현재 생성 규칙
- primitive: string/number/boolean 기본값
- enum: 첫 enum 값
- collection: 제네릭 타입 기준 샘플 리스트
- object: 필드 재귀 생성

유지보수 포인트
- reflection 실패 시 빈 결과가 나올 수 있다.
- IDE 모듈에서는 reflection 실패 시 PSI 기반 폴백 생성 로직을 추가로 사용한다.

관련 테스트
- `apirunner-generator/src/test/kotlin/com/apirunner/generator/DefaultBodyGeneratorTest.kt`
- `apirunner-generator/src/test/kotlin/com/apirunner/generator/DefaultParamGeneratorTest.kt`

### 3.3 `apirunner-http`

핵심 파일
- `apirunner-http/src/main/kotlin/com/apirunner/http/OkHttpExecutor.kt`
- `apirunner-http/src/main/kotlin/com/apirunner/http/OkHttpClientProvider.kt`
- `apirunner-http/src/main/kotlin/com/apirunner/http/AuthInterceptor.kt`
- `apirunner-http/src/main/kotlin/com/apirunner/http/JwtExtractor.kt`
- `apirunner-http/src/main/kotlin/com/apirunner/http/SessionCookieJar.kt`

책임
- 실제 HTTP 호출
- JWT 자동 주입/응답에서 재추출
- 세션 쿠키 유지
- multipart 요청 생성

multipart 파싱 규칙
- Body를 줄 단위 `key=value`로 파싱
- `value`가 `@`로 시작하면 파일 파트
- 값이 JSON 형태(`{...}`, `[...]`)면 JSON 파트
- 그 외는 text 파트

유지보수 포인트
- multipart 요청일 때 `Content-Type` 헤더는 수동 전달하지 않고 OkHttp boundary를 사용한다.
- 파일 미존재 시 즉시 예외(`Multipart file not found`)를 던진다.

관련 테스트
- `apirunner-http/src/test/kotlin/com/apirunner/http/OkHttpExecutorTest.kt`
- `apirunner-http/src/test/kotlin/com/apirunner/http/JwtExtractorTest.kt`

### 3.4 `apirunner-ide`

#### A) 플러그인 등록/엔트리
핵심 파일
- `apirunner-ide/src/main/resources/META-INF/plugin.xml`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ApiRunnerToolWindowFactory.kt`

책임
- RunLineMarkerContributor 등록
- 프로젝트 서비스 등록
  - `ApiRunnerProjectService`
  - `ApiRunnerResultService`
- 하단 ToolWindow(`API Runner`) 구성

#### B) 거터 화살표(라인 마커)
핵심 파일
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/marker/ApiRunnerRunLineMarkerContributor.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/marker/ControllerRunLineMarker.kt`

책임
- leaf element만 처리해 성능 경고를 줄이는 방식 적용
- Spring Mapping 메서드에만 화살표 노출
- 클릭 시 `ApiRunnerExecutor.run(psiMethod)` 호출

유지보수 포인트
- `getInfo()`에서 `element.firstChild != null` 체크를 제거하면 leaf 관련 성능 warning이 재발할 수 있다.

관련 테스트
- `apirunner-ide/src/test/kotlin/com/apirunner/ide/marker/ControllerRunLineMarkerTest.kt`

#### C) PSI 엔드포인트 추출
핵심 파일
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/psi/EndpointExtractor.kt`

책임
- 클래스/메서드 Mapping 조합으로 최종 path 생성
- `@PathVariable`, `@RequestParam`, `@RequestBody` 분석
- `@RequestMapping(method=...)` 해석

유지보수 포인트
- annotation 문자열 파싱 기반이라 특이 문법에서는 보강이 필요할 수 있다.

관련 테스트
- `apirunner-ide/src/test/kotlin/com/apirunner/ide/psi/EndpointExtractorTest.kt`

#### D) 실행/오케스트레이션
핵심 파일
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerExecutor.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/orchestration/ApiExecutionOrchestrator.kt`

책임
- 초기 요청 생성
- multipart 여부 자동 판단
- RequestPart 기반 multipart 템플릿 생성
- 다이얼로그 편집값 저장/복원
- 백그라운드 실행

중요 구현 포인트
- 자동 서버 시작/재시작 대신 수동 포트 전략
- multipart 템플릿 예시
  - `file=@/path/to/file.bin`
  - `metadata={"x":1}`
- 실행 직전 `normalizeMultipartFilePaths()`로 상대경로를 프로젝트 절대경로로 변환

#### E) 프로젝트 상태 저장
핵심 파일
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerProjectService.kt`

저장 항목
- 선택된 앱 클래스
- 마지막 포트
- 엔드포인트별 마지막 요청 draft

draft 저장 키 정책
- endpointKey: `"METHOD pathTemplate"`
- Properties key: `apiRunner.requestDraft.` + `SHA-256(endpointKey)`
- 같은 endpointKey는 항상 덮어쓴다(히스토리 저장 아님)

#### F) 서버/포트 전략
핵심 파일
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/server/ServerManager.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/IntellijServerProcess.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/server/PortParser.kt`

책임
- 현재는 `IntellijServerProcess`가 `lastPort`를 반환하는 수동 모드
- `PortParser`는 과거 로그 기반 감지 로직 유틸로 유지 중

주의
- 현재 정책상 로그 파싱에 의존하지 않는다.

#### G) 요청/결과 UI
핵심 파일
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ApiRequestDialogWrapper.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/RequestDialog.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ApiRunnerResultService.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ResultPresenter.kt`

요청 UI
- Body 에디터 높이 확대(`Dimension(640, 260)`)
- Validation
  - JSON 형식 검증
  - multipart `key=value` 검증
  - multipart 파일 경로 제한
    - 절대경로 금지
    - `../` 루트 이탈 금지

결과 UI
- 상/하 분할 비율 `0.2 : 0.8` 유지
- 본문이 JSON이면 Pretty Print
- 2MB 초과 본문은 잘라서 표시

관련 테스트
- `apirunner-ide/src/test/kotlin/com/apirunner/ide/ui/RequestDialogTest.kt`
- `apirunner-ide/src/test/kotlin/com/apirunner/ide/orchestration/ApiExecutionOrchestratorTest.kt`
- `apirunner-ide/src/test/kotlin/com/apirunner/ide/server/ServerManagerTest.kt`
- `apirunner-ide/src/test/kotlin/com/apirunner/ide/server/PortParserTest.kt`

## 4. multipart 사용 규약

요청 예시
```http
POST /upload
Content-Type: multipart/form-data

file=@testdata/test.png
metadata={"userId":1,"name":"test"}
```

경로 규칙
- `@testdata/test.png`: 허용
- `@/Users/.../test.png`: 금지
- `@../../outside.png`: 금지

실행 단계
1. 다이얼로그 검증에서 경로 정책 확인
2. 실행 직전 프로젝트 루트 절대경로로 변환
3. OkHttp multipart body 생성

## 5. 자주 바꾸는 요구사항별 수정 위치

1. 포트 입력 UX 변경
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerExecutor.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerProjectService.kt`

2. draft 저장 정책 변경(히스토리, preset 등)
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerProjectService.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerExecutor.kt`

3. multipart 문법 확장
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/RequestDialog.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerExecutor.kt`
- `apirunner-http/src/main/kotlin/com/apirunner/http/OkHttpExecutor.kt`

4. 결과 화면 비율/표시 변경
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/ApiRunnerResultService.kt`

5. 화살표 노출 조건 변경
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/marker/ApiRunnerRunLineMarkerContributor.kt`
- `apirunner-ide/src/main/kotlin/com/apirunner/ide/psi/EndpointExtractor.kt`

## 6. 배포 전 최소 점검
- `./gradlew test`
- `./gradlew :apirunner-ide:test`
- `./gradlew :apirunner-ide:buildPlugin`
- 샌드박스 IDE에서 다음 수동 확인
  - JSON API 1회 실행
  - 같은 endpoint 재실행 시 draft 복원
  - multipart(file + JSON part) 실행
  - 잘못된 경로(`../`, 절대경로) 차단
  - 결과 Pretty Print 출력

## 7. Known Limitations (MVP1)
- 서버 자동 시작/재시작 미지원
- draft 다중 히스토리/시나리오 프리셋 미지원
- annotation 파싱은 문자열 기반이라 케이스에 따라 보강 필요
- multipart 문법은 텍스트 기반이며 고급 기능(다중 파일 배열 등)은 미지원
