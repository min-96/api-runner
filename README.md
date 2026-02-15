# API Runner (MVP 1)

Spring Controller 메서드 옆 ▶ 버튼으로 API를 호출하는 IntelliJ Plugin 프로젝트입니다.

## Modules
- `apirunner-core`: 실행 모델/엔진/템플릿 치환
- `apirunner-generator`: RequestBody 기본 JSON 생성
- `apirunner-http`: OkHttp 실행, JWT/Cookie 처리, multipart 전송
- `apirunner-ide`: IntelliJ UI/PSI/Orchestrator/Result 화면

## Current MVP 1 Policy
- 서버 자동 실행/자동 재시작 없음
- ToolWindow `Settings` 탭에서 포트 지정(기본 8080) + 프로젝트 단위 저장 재사용
- 응답 JSON은 결과창에서 자동 Pretty Print
- 결과창 `Request` 섹션에서 실제 전송 요청(method/url/headers/body) 확인 가능
- `multipart/form-data` 지원 (`file=@...`, `metadata={...}`)
- multipart 파일 경로는 프로젝트 루트 기준 상대경로만 허용
- 엔드포인트별 마지막 요청 draft 자동 복원 (`METHOD + pathTemplate`)

정책/사용법/유지보수 포인트는 아래 문서 참고:
- `docs/13_MVP1_RUNTIME_GUIDE.md`
- `docs/14_MVP1_CODE_HANDOVER.md`

## Build / Test
```bash
./gradlew test
./gradlew :apirunner-ide:test
./gradlew :apirunner-ide:buildPlugin
```

## Local Plugin Package
- output: `apirunner-ide/build/distributions/apirunner-ide-1.0.2.zip`
- IntelliJ: `Settings > Plugins > ⚙ > Install Plugin from Disk...`
