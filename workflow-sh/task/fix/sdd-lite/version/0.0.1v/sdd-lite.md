# [Fix · SDD-lite] 양식 정의

> **fix 트랙**: 핵심 **설계 갈림길 또는 계약 변경** 트래킹 문서. sdd-lite 본 정신(변경 사실 + 작업 체크 + 검증 절차) 유지.
> planning은 `../../../../pes/workspectrum/sdd-lite/version/0.0.1v/sdd-lite.md`(시간 기반, 핸드오프·동기화 문서) 참조.
> fix는 **수정 범위·복잡도** 기준 5단계 중 4단계 — 인터페이스/계약 단위의 변경 + 받는 측 인계.

---

## 사용 시점 (트리거)

다음 조건 중 **2개 이상**을 만족할 때 본 양식을 쓴다.

- [ ] 인터페이스 시그니처 변경 (포트, Controller, Service public 메서드)
- [ ] 응답 래퍼 / 에러 포맷 / 헤더 계약 변경
- [ ] 인증 매체·CORS·세션 전략 변경
- [ ] BC 간 어댑터 계약 갱신 — 받는 측에 인벤토리·체크리스트 인계 필요
- [ ] 변경 사실 + 받는 측 작업 + 정합성 검증 Runbook의 **3축 통합** 필요

**졸업 신호 → fix/sdd로**:
- 원본 Product 단위 아키텍처·롤아웃 결정이 함께 번복됨
- 대안 검토(Option A/B/C) 3+ 갈림길이 재발생

---

## 양식 골격

```
# [Fix · SDD-lite] {제목} ({version 또는 식별자}, {YYYY-MM-DD})

> **본 문서의 역할**: {왜 이 변경 인계가 필요한가}
> **변경 트리거**: 원본 SDD / PES / 핸드오프 — {경로}
> **단방향/양방향**: {예: 변경 측 → 받는 측}

작성 시점: {YYYY-MM-DD} · 기준 브랜치: {feat/...}

---

## 1. Context — 왜 이 인계가 필요한가
{원본 작업 안에서 발생한 계약 변경 / 핵심 갈림길 / 받는 측에서 즉시 반영해야 하는 정합성 요구}

## 2. 변경 스냅샷

### 2.1 영향 항목
| 항목 | 카테고리 | 변경 전 | 변경 후 | 행동 |
| --- | --- | --- | --- | --- |
| {Port/Controller/Header명} | 시그니처/계약 | {old} | {new} | 섹션 N에서 처리 |

### 2.2 진실 소스
| 영역 | 변경된 경로 |
| --- | --- |
| {계약 정의 1} | {파일 경로 + 라인} |

---

## 3. 인벤토리

### 3.1 호출 가능 (변경된 화이트리스트)
| METHOD | PATH 또는 시그니처 | 변경 사항 | 비고 |
| --- | --- | --- | --- |
| ... |

### 3.2 호출 금지 / 제거된 항목
- {제거된 endpoint / 폐기된 메서드}

---

## 4. 계약 (변경 부분만)

### 4.1 응답 / 에러 포맷
{변경 전 → 변경 후 JSON 박스}

### 4.2 헤더 / 인증 매체
{변경 전 → 변경 후}

---

## 5. 받는 측 작업 체크리스트
형식: `[ ] {path} — {what} — {why} — {check}`

### 5.1 {큰 작업 1}
- [ ] {경로} — {무엇} — {왜} — {확인 방법}

### 5.2 {큰 작업 2}
- [ ] ...

---

## 6. 검증 Runbook
### 6.1 사전 부팅 신호
{curl 또는 명령}

### 6.2 N 시나리오
{각 시나리오 — curl + 기대 응답}

### 6.3 통과 신호
- [ ] 6.1 OK
- [ ] 6.2 N 시나리오 모두 통과

---

## 7. Open Questions / 다음 fix 인계
### 7.1 {미결}

---

## 8. 자가 점검
- [ ] 인계 완결성 — 받는 측이 본 문서만으로 6.2 시나리오 실행 가능
- [ ] 사실 정합성 — 인용 endpoint·시그니처가 실제 코드와 일치
- [ ] 작업 실행성 — 섹션 5의 모든 항목이 4축(path/what/why/check) 정합
- [ ] 영향 분기 — 본 fix가 fix/pes로 격하되거나 fix/sdd로 격상되는 트리거 미발생

## 9. 원본 명세 갱신
- 원본 SDD / 핸드오프 (`{경로}`) — [명세 변경 이력] 블록에 본 fix 식별자 + 변경 일자 기재
- DOMAIN.md / PACKAGE.md — {갱신 섹션}
```

### 섹션 가이드

| 섹션 | 핵심 |
| --- | --- |
| **Context** | "왜 받는 측이 본 문서를 읽어야 하나"를 자기 완결적으로 |
| **변경 스냅샷** | 변경 전/후를 표로 정확히. 받는 측이 diff를 한눈에 |
| **인벤토리** | 변경 후 화이트리스트 + 제거된 항목 (호출 금지 명시) |
| **계약** | 변경 부분만. 전체 계약은 원본 핸드오프 참조 |
| **체크리스트** | 4축(path/what/why/check) 강제. 받는 측 실행 단위 |
| **Runbook** | curl + 기대 응답 + 통과 신호 |
| **자가 점검** | 작성자가 4신호로 통과 확정 + fix 단계 격상/격하 트리거 점검 |
| **원본 명세 갱신** | 원본에 [명세 변경 이력] 추가가 의무 |

---

## 예시 (간단 사례)

```
# [Fix · SDD-lite] LearningFacade 응답 ApiResponse<T> 래퍼 도입 (0.0.1v, 2026-06-26)

> **본 문서의 역할**: LearningFacade의 모든 Controller 응답이 DTO 직접 반환에서 ApiResponse<T> 래퍼로 전환.
> **변경 트리거**: `workflow/task/pes/fe-handoff/0.0.1v.md` §8.1 Open Question "ApiResponse 통일 시점" 결정.
> **단방향**: BE LearningFacade → FE (받는 측)

작성 시점: 2026-06-26 · 브랜치: feat/wrap-learning-facade-response

---

## 1. Context
0.0.1v 시점 BE Controller는 DTO를 직접 반환. CLAUDE.md `ApiResponse<T>` 래퍼 명시와 어긋남. 0.0.2v에서 LearningFacade를 첫 적용 BC로 결정. FE는 본 문서를 받아 응답 파싱·MSW handler·Zod 스키마를 갱신.

## 2. 변경 스냅샷
### 2.1 영향 항목
| 항목 | 변경 전 | 변경 후 | 행동 |
| --- | --- | --- | --- |
| GET /api/v1/learning-facade | `LearningFacadeResponse` | `ApiResponse<LearningFacadeResponse>` | FE Zod 스키마에 래퍼 추가 |
| POST /api/v1/learning-facade/axes | `LearningAxisResponse` | `ApiResponse<LearningAxisResponse>` | 동일 |

### 2.2 진실 소스
| 영역 | 변경된 경로 |
| --- | --- |
| ApiResponse 정의 | `Common/response/ApiResponse.java` |
| LearningFacadeController | `LearningFacade/presentation/LearningFacadeController.java` |

## 4. 계약
### 4.1 응답 포맷
**변경 전**:
```jsonc
// GET /api/v1/learning-facade → 200
{ "facadeId": 1, "axes": [...] }
```
**변경 후**:
```jsonc
{
  "success": true,
  "data": { "facadeId": 1, "axes": [...] },
  "timestamp": "2026-06-26T..."
}
```

## 5. 받는 측 작업
- [ ] `src/lib/api/schemas/LearningFacade.ts` — ApiResponseSchema<T> 래퍼 적용 — 응답 파싱 정합 — `npm run test` 그린
- [ ] `src/mocks/handlers/learningFacade.handlers.ts` — MSW 응답을 ApiResponse 형식으로 — 동일 사유 — 그린

## 6. 검증
### 6.2 시나리오 1 — 단일 GET
```bash
curl -s -b /tmp/cookies.txt http://localhost:8080/api/v1/learning-facade | jq .success
# 기대: true
```

## 8. 자가 점검
- [x] 인계 완결성, 사실 정합성, 작업 실행성, 영향 분기 — 모두 본 fix 안에서 처리 가능

## 9. 원본 명세 갱신
- `pes/fe-handoff/0.0.1v.md` §8.1에 본 fix 식별자 기재 (해결됨)
- ADR NNN 후보: ApiResponse 도입 결정 (ADR-CANDIDATES.md에 추가)
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- pes 대응 (planning): `../../../../pes/workspectrum/sdd-lite/version/0.0.1v/sdd-lite.md`
- 첫 운영 사례 (planning 트랙): `../../../../pes/fe-handoff/0.0.1v.md`
- 이전 fix 단계: `../../../pes/version/0.0.1v/pes.md`
- 다음 fix 단계: `../../../sdd/version/0.0.1v/sdd.md`
- 양식 진화: SemVer 진화. 변경 시 `../0.0.2v/`에 새 버전
