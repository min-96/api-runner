# 10_RISK_AND_LIMITATIONS.md
# Spring Controller API Runner — Risk & Limitations

---

# 1. 목적

이 문서는 다음을 명확히 정의한다:

- 기술적 리스크
- 구조적 한계
- MVP 범위 내 제약
- 확장 시 예상 문제
- 대응 전략

이 문서는 “완벽한 시스템”을 가정하지 않는다.  
현실적인 제약을 명확히 인지하는 것이 목적이다.

---

# 2. 기술적 리스크

---

## R1. PSI 파싱 복잡성 (특히 Kotlin)

### 문제

- Java PSI는 비교적 안정적
- Kotlin PSI는 복잡하고 UAST 사용이 필요할 수 있음
- @RequestMapping 조합 패턴 다양

### 영향

- 일부 Endpoint 추출 실패 가능
- Kotlin 프로젝트에서 오동작 가능성

### MVP 대응

- Java + 기본 Kotlin 케이스 우선 지원
- UAST 확장은 Phase 2 이후

---

## R2. Reflection 기반 BodyGenerator 한계

### 문제

- Generic 타입 정확한 파싱 어려움
- Kotlin metadata 정보 누락 가능
- Inner class/익명 클래스 처리 어려움

### 영향

- JSON 자동 생성이 완벽하지 않을 수 있음

### MVP 대응

- Raw type 기반 단순 생성
- Depth 제한으로 순환 참조 방지
- 복잡한 타입은 null 처리 허용

---

## R3. 순환 참조 객체

### 문제

예:
```kotlin
class A(val b: B)
class B(val a: A)
```

무한 재귀 위험

### 대응

- maxDepth 제한
- Depth 초과 시 null 반환

---

## R4. JWT 자동 추출(Heuristic)의 불완전성

### 문제

- 응답 JSON 구조 다양
- token/accessToken 외 키 사용 가능
- nested JSON에서 추출 실패 가능

### 영향

- JWT 자동 유지 실패 가능

### MVP 대응

- 간단한 regex 기반 추출
- 실패 시 사용자 수동 입력 허용
- Phase 2: JSONPath 기반 ExtractStep 도입

---

## R5. 서버 로그 기반 포트 파싱 실패

### 문제

- 로그 형식 변경
- 커스텀 로그 포맷
- Reactive/Netty 로그 패턴 차이

### 영향

- baseUrl 자동 설정 실패

### MVP 대응

- Tomcat/Netty 기본 패턴 지원
- 실패 시 포트 수동 입력 Dialog 제공

---

## R6. 멀티 모듈 프로젝트에서 실행 대상 모호성

### 문제

- 여러 SpringBootApplication 존재
- 어떤 모듈을 실행해야 할지 불명확

### MVP 대응

- 최초 실행 시 선택 Dialog 제공
- 선택 결과 프로젝트 단위 저장

---

## R7. 기업망/프록시 환경

### 문제

- IDE 프록시 설정과 OkHttp 설정 불일치
- SSL 인증서 문제

### MVP 대응

- 기본 OkHttp 설정 사용
- Phase 2: IDE 설정 연동 고려

---

## R8. WebFlux/Reactive 지원 한계

### 문제

- WebFlux 서버 로그 패턴 상이
- Reactive 특성상 로그 타이밍 이슈

### MVP 대응

- WebMVC 중심 지원
- WebFlux는 best-effort 지원

---

# 3. 구조적 한계

---

## L1. Dialog 기반 UX의 확장성

### 문제

- Scenario UI로 확장 시 Dialog 구조는 한계 존재

### 대응

- MVP는 Dialog 유지
- Phase 2: ToolWindow 중심 UI 전환

---

## L2. 단일 실행 중심 설계

### 문제

- 현재 구조는 단일 HttpCallStep 중심

### 대응

- Core는 Step 기반 구조 유지
- Scenario 확장 시 Plan 확장 가능

---

## L3. HTTP 요청은 동기 실행

### 문제

- 대용량 응답 시 UI 블로킹 위험

### 대응

- IDE에서 background task 실행
- ToolWindow 업데이트는 UI thread에서

---

# 4. 성능 관련 리스크

---

## P1. 대용량 JSON Body 생성

- Reflection + JSON 직렬화 비용 존재
- 대부분 DTO 크기는 작음 → 문제 낮음

---

## P2. 대용량 Response 처리

- Body 전체 메모리 로딩
- 스트리밍 미지원(MVP)

---

# 5. UX 리스크

---

## U1. 자동 입력이 실제 값과 다를 가능성

- 기본값은 항상 placeholder
- 사용자가 반드시 확인 필요

---

## U2. 인증 자동화가 오히려 혼란 유발 가능

- JWT가 자동 삽입되는데 사용자는 모를 수 있음

### 대응

- Headers 탭에 Authorization 표시
- JWT 상태 표시(Phase 2)

---

# 6. 보안 고려사항

---

## S1. JWT 메모리 저장

- ExecutionContext.variables에 저장
- IDE 세션 종료 시 삭제

---

## S2. 민감 정보 로그 노출

- ToolWindow에 Body 전체 표시
- 운영 환경 사용은 권장하지 않음

---

# 

무한 재귀 위험

### 대응

- maxDepth 제한
- Depth 초과 시 null 반환

---

## R4. JWT 자동 추출(Heuristic)의 불완전성

### 문제

- 응답 JSON 구조 다양
- token/accessToken 외 키 사용 가능
- nested JSON에서 추출 실패 가능

### 영향

- JWT 자동 유지 실패 가능

### MVP 대응

- 간단한 regex 기반 추출
- 실패 시 사용자 수동 입력 허용
- Phase 2: JSONPath 기반 ExtractStep 도입

---

## R5. 서버 로그 기반 포트 파싱 실패

### 문제

- 로그 형식 변경
- 커스텀 로그 포맷
- Reactive/Netty 로그 패턴 차이

### 영향

- baseUrl 자동 설정 실패

### MVP 대응

- Tomcat/Netty 기본 패턴 지원
- 실패 시 포트 수동 입력 Dialog 제공

---

## R6. 멀티 모듈 프로젝트에서 실행 대상 모호성

### 문제

- 여러 SpringBootApplication 존재
- 어떤 모듈을 실행해야 할지 불명확

### MVP 대응

- 최초 실행 시 선택 Dialog 제공
- 선택 결과 프로젝트 단위 저장

---

## R7. 기업망/프록시 환경

### 문제

- IDE 프록시 설정과 OkHttp 설정 불일치
- SSL 인증서 문제

### MVP 대응

- 기본 OkHttp 설정 사용
- Phase 2: IDE 설정 연동 고려

---

## R8. WebFlux/Reactive 지원 한계

### 문제

- WebFlux 서버 로그 패턴 상이
- Reactive 특성상 로그 타이밍 이슈

### MVP 대응

- WebMVC 중심 지원
- WebFlux는 best-effort 지원

---

# 3. 구조적 한계

---

## L1. Dialog 기반 UX의 확장성

### 문제

- Scenario UI로 확장 시 Dialog 구조는 한계 존재

### 대응

- MVP는 Dialog 유지
- Phase 2: ToolWindow 중심 UI 전환

---

## L2. 단일 실행 중심 설계

### 문제

- 현재 구조는 단일 HttpCallStep 중심

### 대응

- Core는 Step 기반 구조 유지
- Scenario 확장 시 Plan 확장 가능

---

## L3. HTTP 요청은 동기 실행

### 문제

- 대용량 응답 시 UI 블로킹 위험

### 대응

- IDE에서 background task 실행
- ToolWindow 업데이트는 UI thread에서

---

# 4. 성능 관련 리스크

---

## P1. 대용량 JSON Body 생성

- Reflection + JSON 직렬화 비용 존재
- 대부분 DTO 크기는 작음 → 문제 낮음

---

## P2. 대용량 Response 처리

- Body 전체 메모리 로딩
- 스트리밍 미지원(MVP)

---

# 5. UX 리스크

---

## U1. 자동 입력이 실제 값과 다를 가능성

- 기본값은 항상 placeholder
- 사용자가 반드시 확인 필요

---

## U2. 인증 자동화가 오히려 혼란 유발 가능

- JWT가 자동 삽입되는데 사용자는 모를 수 있음

### 대응

- Headers 탭에 Authorization 표시
- JWT 상태 표시(Phase 2)

---

# 6. 보안 고려사항

---

## S1. JWT 메모리 저장

- ExecutionContext.variables에 저장
- IDE 세션 종료 시 삭제

---

## S2. 민감 정보 로그 노출

- ToolWindow에 Body 전체 표시
- 운영 환경 사용은 권장하지 않음

---

## 7. MVP 한계 요약

| 항목 | MVP 수준 |
| --- | --- |
| JWT 추출 | 단순 heuristic |
| Generic 타입 | 제한적 |
| WebFlux | 제한적 |
| Scenario | 미지원 |
| Retry | 미지원 |
| JSONPath | 미지원 |

---

# 8. 확장 전략 요약

| 리스크 | Phase 2 대응 |
| --- | --- |
| JWT heuristic | JSONPath 기반 추출 |
| Generic 타입 | Kotlin metadata 분석 |
| Dialog 확장성 | ToolWindow 기반 Scenario UI |
| 멀티 앱 실행 | 앱 선택 캐싱 |

---

# 9. 결론

이 플러그인은 MVP 기준에서:

- 개발 생산성 향상에 초점을 둔 도구
- 완전한 API 테스트 프레임워크가 아님
- 복잡한 케이스는 확장 Phase에서 해결

리스크는 존재하지만,

구조적으로 확장 가능한 형태로 설계되어 있다.

---

# 10. 다음 문서

- 11_FUTURE_ROADMAP.md
    - Phase 2 ~ Phase 4 기능 확장 계획
    - Scenario 설계 방향
    - 장기 아키텍처 전략