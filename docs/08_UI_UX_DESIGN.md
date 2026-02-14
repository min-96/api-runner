# 08_UI_UX_DESIGN.md
# Spring Controller API Runner — UI / UX Design

---

# 1. 목적

이 문서는 IntelliJ Plugin의 사용자 경험(UI/UX)을 정의한다.

UI의 목표는 단 하나다:

> “Controller에서 ▶ 누르면, 최소한의 수정만으로 API를 즉시 실행할 수 있어야 한다.”

이 문서는 다음을 정의한다:

- 전체 사용자 흐름(User Flow)
- Input Dialog 설계
- Result ToolWindow 설계
- 반복 실행 UX
- 오류 처리 UX
- 향후 확장(Scenario) 대비 UI 구조

---

# 2. 전체 사용자 흐름

## 2.1 MVP 기본 흐름

```
Controller 코드
↓
메서드 옆 ▶ 클릭
↓
자동 입력 채워진 Dialog 표시
↓
사용자가 값 수정
↓
Run 클릭
↓
서버 자동 실행 (필요 시)
↓
API 호출
↓
하단 ToolWindow에 결과 표시
```

---

## 2.2 반복 실행 흐름

```
결과 확인
↓
같은 Controller에서 ▶ 재클릭
↓
이전 입력값 유지
↓
값 일부 수정
↓
재실행
```

UX 목표:

- 반복 호출이 매우 빠르고 자연스러워야 한다.
- 인증(JWT/세션)은 재입력 필요 없어야 한다.

---

# 3. Input Dialog 설계

## 3.1 선택 이유

MVP에서는 Dialog 방식을 선택한다.

이유:

- 단발 실행 중심
- 구현 난이도 낮음
- 직관적
- Postman 느낌 유지

---


## 3.2 Dialog 레이아웃 구조
```
| POST /api/users/{id} |
| [Path / Query] [Headers] [Body] |
| |
| (선택된 탭 내용 표시 영역) |
| |
| Cancel Run |
```
---

## 3.3 상단 정보 영역

표시 내용:

- HTTP Method (색상 강조)
- PathTemplate
- 현재 baseUrl (있다면 표시)

예:

POST /api/users/{id}


---

# 4. Path / Query 탭 설계

## 4.1 구조

테이블 형태:

| Type  | Key      | Value |
|-------|----------|-------|
| PATH  | id       | 1     |
| QUERY | page     | 1     |
| QUERY | size     | 10    |

---



## 4.2 UX 원칙

- Key는 수정 불가 (자동 생성)
- Value는 자유롭게 수정 가능
- 빈 값 허용
- 추가 QueryParam 수동 추가 허용 (확장)

---

## 4.3 동작 규칙

- PathVariable은 필수 값 (비어있으면 경고)
- QueryParam은 비어도 허용
- 숫자 타입이라도 문자열 입력 허용 (HTTP는 문자열 기반)

---

# 5. Headers 탭 설계

## 5.1 기본 헤더

자동 삽입:

- Content-Type: application/json (body 존재 시)
- Authorization: Bearer xxx (JWT 존재 시)

---


## 5.2 구조

| Key            | Value              |
|----------------|--------------------|
| Content-Type   | application/json   |
| Authorization  | Bearer eyJhbGci... |

---

## 5.3 UX 규칙

- Key/Value 모두 수정 가능
- Authorization은 자동 삽입되지만 수정 가능
- 사용자가 Authorization을 삭제하면 자동 삽입하지 않음

---


# 6. Body 탭 설계

## 6.1 JSON Editor 사용

- IntelliJ `EditorTextField`
- Syntax highlighting(JSON)
- Pretty format 유지

---

## 6.2 기본값 예시

```json
{
  "name": "string",
  "age": 1,
  "active": true
}
```

## 6.3 UX 원칙

- JSON 형식 오류 시 Run 차단
- 에러 위치 하이라이트
- 자동 포맷 버튼 (선택적)

---

# 7. Run 버튼 동작

Run 클릭 시:

1. 입력값 검증
2. ApiRequest 구성
3. Orchestrator 호출
4. Dialog 닫힘
5. ToolWindow 결과 표시

---

# 8. Result ToolWindow 설계

## 8.1 위치

하단(anchor=bottom)

---

## 8.2 레이아웃

```
---------------------------------------------------
| Status: 200       Time: 42ms                   |
---------------------------------------------------
| [Headers] [Body]                               |
---------------------------------------------------
|                                                 |
| (선택된 탭 내용 표시 영역)                     |
|                                                 |
---------------------------------------------------
```

---

## 8.3 Status 표시

- 2xx: 초록
- 4xx: 주황
- 5xx: 빨강

---

## 8.4 Headers 탭

- key-value 목록 표시
- 정렬 가능

---

## 8.5 Body 탭

- Pretty JSON 렌더링
- JSON 아닐 경우 Raw Text 표시
- 큰 응답은 접기 가능

---

# 9. 오류 UX 설계

## 9.1 서버 실행 실패

ToolWindow에 메시지:

```
Server failedtostart.
Pleasecheck RunConfiguration.
```

---

## 9.2 포트 파싱 실패

Dialog:

```
Couldnotdetectserverport.Please enter port manually:
[8080 ]
```

---

## 9.3 네트워크 오류

ToolWindow 표시:

```
Connection refused:localhost:8080
```

---

## 9.4 JSON 파싱 오류

Body 탭 상단:

```
InvalidJSONformat.
```

---

# 10. 반복 실행 UX

## 10.1 입력 유지 전략

- 같은 Controller 재실행 시 마지막 입력 유지
- JWT는 자동 유지
- 세션 쿠키 자동 유지

---

## 10.2 빠른 재실행

향후 추가 가능:

- ToolWindow에 "Re-run" 버튼
- 최근 요청 히스토리

---

# 11. 향후 Scenario 확장 대비 UI

MVP에서는 Dialog 방식이지만,

Scenario 확장 시:

- ToolWindow를 중심 UI로 전환 가능
- Step 리스트 표시
- Step별 요청 수정 가능

UI 구조는 이를 수용할 수 있도록 설계한다.

---

# 12. UX 목표 요약

| 목표 | 설명 |
| --- | --- |
| 최소 입력 | 자동 생성 + 값만 수정 |
| 빠른 반복 | JWT/세션 자동 유지 |
| 명확한 결과 | Status + Time + JSON |
| 안전한 실패 | 친절한 오류 표시 |
| 확장 가능 | Scenario 대비 구조 |

---

# 13. 다음 문서

- 09_TASK_BREAKDOWN.md
    - Phase 기반 개발 단계 정리
    - 체크리스트
    - 우선순위
    - 리팩토링 타이밍