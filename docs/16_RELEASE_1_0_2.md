# 16_RELEASE_1_0_2.md
# API Runner 1.0.2 Release Notes

## 1. 범위
1. ToolWindow를 `Result` / `Settings` 탭으로 분리
2. 사용자 지정 Port 저장/재사용 기능 제공
3. 결과 화면에 `Request` 섹션 추가
4. IDE/UI 레이어 중심 리팩토링 및 책임 분리

## 2. 주요 기능 추가
### 2.1 사용자 지정 Port 설정
- 위치: ToolWindow `Settings` 탭
- 동작:
  - `Server Port` 입력 후 `Apply` 또는 Enter
  - 유효성 검사: `1..65535`
  - 프로젝트 단위 저장 (`PropertiesComponent`)
- 저장 키: `apiRunner.lastServerPort`

### 2.2 Request 섹션 추가
- 위치: ToolWindow `Result` 탭
- 표시 내용:
  - method
  - 최종 request URL(path/query 반영)
  - request headers
  - request body(JSON이면 보기 좋게 정리)

## 3. 포트 적용 동작 개선
- 실행 시점마다 `baseUrl`을 최신 Port로 재계산하도록 변경
- Settings에서 Port를 바꾼 직후 다음 실행부터 즉시 반영
- 기존 `ExecutionContext.baseUrl` 캐시로 인해 이전 포트가 유지되던 문제 수정

## 4. 리팩토링 구조
### 4.1 UI 책임 분리
- `ApiRunnerToolWindowFactory`
  - Result/Settings 탭 조립
- `ApiRunnerResultService`
  - 결과 렌더링 전담
- `ApiRunnerSettingsService`
  - 포트 입력/검증/저장 전담
- `ResultPresenter` + `ResultViewModel`
  - 요청/응답 데이터 전달 모델 확장

### 4.2 입력/표시 유틸 분리
- `RequestInputCodec`
  - Query/Header 문자열 <-> Map 변환
- `RequestHeaderSupport`
  - multipart header 판별
- `ResultTextFormatter`
  - Request 섹션 및 JSON pretty/truncate 포맷
- `ResultSectionSpec`
  - Result 섹션 정의 모델
- `InMemoryResultSink`
  - 테스트/비UI 결과 캡처

### 4.3 Core 연계 변경
- `ExecutionContext`에 `lastRequest` 저장
- `HttpCallStep`에서 template resolve 이후 `lastRequest`를 기록
- `ApiExecutionOrchestrator`가 최신 `baseUrl`을 매 실행 갱신

## 5. 검증 포인트
1. `./gradlew :apirunner-ide:test`
2. `Settings` 탭에서 Port 변경 후 연속 실행 시 요청 URL Port 반영 확인
3. `Result` 탭 `Request`/`Response Body` 섹션 렌더링 확인

## 6. 배포 산출물
- plugin zip: `apirunner-ide/build/distributions/apirunner-ide-1.0.2.zip`
