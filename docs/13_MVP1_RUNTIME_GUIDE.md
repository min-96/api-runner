# 13_MVP1_RUNTIME_GUIDE.md
# API Runner MVP 1 운영/사용/유지보수 가이드

## 1. 목적
이 문서는 현재 코드 기준의 실제 동작 정책을 정리한다.
기존 설계 문서의 "자동 서버 실행/로그 기반 포트 감지" 내용과 다를 수 있으며,
배포/운영/유지보수 시 이 문서를 우선 기준으로 사용한다.

## 2. 현재 동작 정책 (As-Is)
- 서버 자동 실행/재시작을 하지 않는다.
- API 호출 시 서버 포트는 사용자 입력(기본 8080)으로 받는다.
- 입력된 포트는 프로젝트 단위로 저장되어 다음 호출에 재사용된다.
- 요청 Dialog는 엔드포인트별 마지막 draft를 자동 복원한다.
  - 키: `METHOD + pathTemplate`
  - 저장 항목: path/query/headers/body
- 응답 Body가 JSON이면 결과 화면에서 자동 Pretty Print한다.
- multipart/form-data를 지원한다.
  - Body 포맷: `key=value` 라인 기반
  - 파일: `file=@relative/path`
  - JSON part: `metadata={"userId":1}`
- multipart 파일 경로는 프로젝트 루트 기준 상대경로만 허용한다.
  - 절대경로 금지
  - `../` 루트 탈출 금지

## 3. 사용자 사용법
### 3.1 일반 JSON API
1. Controller 메서드 옆 ▶ 클릭
2. Path/Query/Header/Body 수정
3. Run 실행

### 3.2 Multipart API
Header:
- `Content-Type: multipart/form-data`

Body 예시:
```text
file=@testdata/test.png
metadata={"userId":1,"name":"test"}
```

주의:
- `@`는 파일 파트를 의미한다.
- 경로는 프로젝트 루트 상대경로만 허용한다.

## 4. 배포 전 체크리스트
- `./gradlew :apirunner-http:test :apirunner-ide:test`
- `./gradlew :apirunner-ide:buildPlugin`
- 산출물 확인:
  - `apirunner-ide/build/distributions/apirunner-ide-0.1.0.zip`
- 내부 설치 테스트:
  - IntelliJ `Install Plugin from Disk`

## 5. 유지보수 핵심 포인트
### 5.1 포트 정책
- 진입 지점: `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerExecutor.kt`
- 포트 저장소: `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerProjectService.kt`
- 서버 실행 자동화 코드는 의도적으로 비활성(정책상 미사용) 상태.

### 5.2 요청 draft 복원
- 저장/복원 키: `METHOD + pathTemplate`
- 저장 위치: IntelliJ `PropertiesComponent` (프로젝트 로컬 상태)
- 서버나 외부 저장소에는 저장하지 않는다.

### 5.3 Multipart 구현
- 전송 구현: `apirunner-http/src/main/kotlin/com/apirunner/http/OkHttpExecutor.kt`
- UI 생성/정규화: `apirunner-ide/src/main/kotlin/com/apirunner/ide/runtime/ApiRunnerExecutor.kt`
- 검증: `apirunner-ide/src/main/kotlin/com/apirunner/ide/ui/RequestDialog.kt`

## 6. 알려진 제한
- API별 draft는 "최근 1개"만 저장된다(히스토리 저장 아님).
- 포트 자동탐지/자동실행은 MVP 1에서 제외.
- 파일 업로드 경로는 프로젝트 내부 상대경로만 허용.

## 7. 2차 MVP(시나리오)로 넘어갈 때 권장
- draft를 단일값에서 다중 preset으로 확장
- 포트를 전역 설정 페이지로 분리
- multipart 입력을 텍스트 모드 유지 + 유효성 강화
