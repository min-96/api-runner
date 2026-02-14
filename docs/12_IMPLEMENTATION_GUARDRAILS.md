# 12_IMPLEMENTATION_GUARDRAILS.md
# Spring Controller API Runner — Implementation Guardrails

---

# 1. 목적

이 문서는 다음을 위한 것이다:

- IDE 멈춤 방지
- 메모리 누수 방지
- 스레딩 실수 방지
- 성능 저하 방지
- 구조 붕괴 방지

이 문서는 “절대 깨면 안 되는 규칙”을 정의한다.

---

# 2. UI Thread (EDT) 절대 규칙

## 🚨 Rule 1 — 절대 EDT에서 네트워크 실행 금지

❌ 금지:

- OkHttp 실행
- 서버 실행 대기
- 로그 대기
- JSON 대용량 파싱

✔ 반드시:

```kotlin
Task.Backgroundable(project, "Running API") {
    ...
}
```
또는 
```kotlin
ApplicationManager.getApplication().executeOnPooledThread {
    ...
}

```
## 🚨 Rule 2 — UI 업데이트는 반드시 invokeLater

```kotlin
ApplicationManager.getApplication().invokeLater {
    updateToolWindow()
}
```

EDT에서만 UI 조작.

---

# 3. 서버 실행 관련 규칙

---

## 🚨 Rule 3 — 이미 실행 중이면 절대 재실행하지 말 것

- RunConfiguration 재사용
- 중복 실행 금지
- 매번 서버 기동 금지

---

## 🚨 Rule 4 — ProcessListener 반드시 제거

```kotlin
processHandler.removeProcessListener(listener)
```

제거하지 않으면:

- 메모리 누수
- 포트 파싱 중복
- 이벤트 폭증

---

## 🚨 Rule 5 — 서버 기동 타임아웃 필수

- 30초 이상 대기 금지
- 타임아웃 시 사용자에게 명확히 알릴 것

---

# 4. HTTP 실행 규칙

---

## 🚨 Rule 6 — HTTP는 항상 Background Thread

- OkHttp 호출은 절대 UI Thread에서 하지 말 것
- 결과 처리만 UI Thread

---

## 🚨 Rule 7 — 대용량 응답 보호

- 2MB 이상이면 Pretty Print 금지
- Raw 모드 제공
- ToolWindow 렌더링은 Lazy 처리

---

# 5. PSI 분석 규칙

---

## 🚨 Rule 8 — 전체 프로젝트 스캔 금지

- 현재 파일의 PsiMethod만 검사
- annotation 존재 여부만 확인
- heavy resolve 금지

PSI 탐색이 무거워지면 에디터 렉 발생.

---

# 6. Generator 규칙

---

## 🚨 Rule 9 — Depth 제한 절대 제거 금지

```kotlin
maxDepth =3
```

순환 참조 방지용.

---

## 🚨 Rule 10 — Reflection 실패 시 예외 던지지 말 것

- null 반환 허용
- 생성 실패해도 실행은 가능해야 함

---

# 7. Core 아키텍처 보호 규칙

---

## 🚨 Rule 11 — Core는 IntelliJ API를 몰라야 한다

Core에서 다음 import 금지:

- com.intellij.*
- okhttp.*
- jackson.*

Core는 순수 JVM 모듈.

---

## 🚨 Rule 12 — Step 기반 구조 깨지 말 것

ExecutionEngine → Step → Context 구조 유지.

단일 실행이라도 Step 패턴 유지.

---

# 8. 메모리 관리 규칙

---

## 🚨 Rule 13 — static 전역 상태 금지

- ExecutionContext는 요청 단위
- 프로젝트 단위 캐시는 최소화

---

## 🚨 Rule 14 — ToolWindow에 무한 로그 누적 금지

- 이전 결과 clear
- 대용량 응답 truncate

---

# 9. UX 안정성 규칙

---

## 🚨 Rule 15 — JWT 자동 삽입은 항상 UI에 보여줄 것

- Headers 탭에서 Authorization 확인 가능해야 함
- 자동 삽입을 숨기지 말 것

---

## 🚨 Rule 16 — 실패는 조용히 삼키지 말 것

- 서버 실행 실패
- 포트 파싱 실패
- 네트워크 실패

모두 ToolWindow에 명확히 표시.

---

# 10. 개발 단계별 체크

---

## MVP 완료 전 필수 확인

- [ ]  HTTP 실행이 EDT에서 돌지 않는가?
- [ ]  서버 실행 대기 로직이 EDT가 아닌가?
- [ ]  ProcessListener 제거하는가?
- [ ]  대용량 응답에서 UI 멈춤 없는가?
- [ ]  반복 실행 시 서버 재기동하지 않는가?

---

# 11. 절대 하지 말아야 할 것

❌ IntelliJ Lifecycle 무시하고 Thread 직접 생성

❌ Blocking call을 UI Thread에서 실행

❌ Core에 IntelliJ 의존성 추가

❌ 서버 실행을 매번 새로 시작

❌ 로그 리스너 제거 안 함

---

# 12. 결론

이 플러그인은 구조적으로 안전하다.

하지만 IntelliJ Plugin은:

- 스레드 실수
- UI 블로킹
- 리스너 누수

에서 가장 많이 망한다.

이 문서의 규칙을 지키면:

- IDE 멈춤 없음
- 메모리 누수 없음
- 성능 문제 없음
- 구조 붕괴 없음
