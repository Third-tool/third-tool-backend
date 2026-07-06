# [SDD-lite] 양식 정의

> 풀 SDD 이전의 약한 버전. **변경 사실 + 작업 체크리스트 + 검증 절차** 3축으로 압축.
> 핸드오프(BE→FE, BC→BC), 마일스톤 검증, 외부 동기화 같은 "정합성 시험" 성격의 문서에 쓴다.
> 첫 운영 사례: `../../../../fe-handoff/0.0.1v.md`.

---

## 사용 시점 (트리거)

다음 조건 중 **2개 이상**을 만족할 때 본 양식을 쓴다.

- [ ] 변경 사실을 **다른 저장소/팀/계층에 인계**해야 함 (BE→FE 핸드오프, BC→BC 어댑터 인계, 백엔드→인프라 요청)
- [ ] 여러 Story 묶음의 결과를 **한 번에 검증할 Runbook**이 필요 (마일스톤 종료 검증, 첫 dev 배포 검증)
- [ ] **호출 가능 / 호출 금지 인벤토리**를 화이트리스트로 정리해야 함 (Controller 미공개 포트가 섞임)
- [ ] **계약**을 명세화해야 함 (응답 래퍼, 에러 포맷, 헤더, 인증 매체)
- [ ] PES만으로는 부족하지만 풀 SDD까지는 과함 — 아키텍처 다이어그램·실패 모드 매트릭스·롤아웃 그래프가 **부분적으로만** 필요

**졸업 신호 → sdd(풀)로**:
- Product 단위 의도·아키텍처가 흔들리는 큰 결정 필요 (대안 검토 3+, 컴포넌트 배치 다이어그램 등)
- 인프라(VPC/RDS/ECS) + 도메인 변경이 한 묶음으로 진행
- 1~2개월 이상의 도메인 재설계로 확장

---

## 양식 골격

```
# [<type>] {제목} — {version 또는 식별자}

> **본 문서의 역할**: {왜 이 문서가 필요한가 — 한 단락}
> **본 양식**: sdd-lite (`workspectrum/sdd-lite/sdd-lite.md`)
> **단방향/양방향**: {예: 백엔드 → 프론트엔드 단방향}

작성 시점: {YYYY-MM-DD} · 기준 브랜치/태그: {feat/... 또는 v0.0.1}

---

## 1. Context — 왜 이 문서가 필요한가
{인계 트리거 / 마일스톤 위치 / 검증해야 할 정합성 / 다음 행동}

## 2. 스냅샷
### 2.1 영향 항목 표
| 항목 | 카테고리 | 상태 | 영향 | 행동 |
| --- | --- | --- | --- | --- |
| ... | ... | ✅ 머지 / 진행 중 / 계획 | ... | 섹션 N에서 처리 |

### 2.2 진실 소스 (Source of Truth)
| 영역 | 진실 소스 경로 |
| --- | --- |
| API 시그니처 | {Controller 경로 + Swagger URL} |
| Request/Response DTO | {DTO 경로} |
| ErrorCode enum | {enum 경로} |
| ... | ... |

## 3. 인벤토리 (해당 시)
### 3.1 호출 가능 (화이트리스트)
| METHOD | PATH | Request DTO | Response DTO | 인증 | 비고 |
| --- | --- | --- | --- | --- | --- |
| ... |

### 3.2 호출 금지 (미공개)
> 내부 포트만 추가되고 Controller 미공개 항목. 호출 훅·서비스를 만들지 말 것.
> - {Port명} ({Story 번호}) — 다음 버전 노출 예정

## 4. 계약 (해당 시)
### 4.1 성공 응답
{JSON 예시 박스 1~3개}

### 4.2 에러 응답
{JSON 예시 + ErrorCode 분기표}

### 4.3 헤더 / 인증 매체 / CORS
{표 또는 박스}

## 5. 작업 체크리스트
형식: `[ ] {path} — {what} — {why} — {check}`

### 5.1 {큰 작업명}
- [ ] {경로} — {무엇} — {왜} — {확인 방법}
- [ ] {경로} — {무엇} — {왜} — {확인 방법}
{큰 변경에는 코드 스니펫 박스}

### 5.2 ...

## 6. 검증 절차 / Runbook
### 6.1 사전 부팅 신호
{curl 또는 명령 + 기대 응답}

### 6.2 N 시나리오
{각 시나리오: curl + 기대 status/body/header}

### 6.3 통과 신호
- [ ] 모든 신호 OK
- [ ] 사전 부팅 통과
- [ ] N 시나리오 통과

## 7. Open Questions / 다음 버전 인계
### 7.1 {미결 항목}
{한 단락 — 다음 버전에서 결정·마이그레이션 예상}

## 8. 자가 점검 (작성자 통과 확인)
- [ ] 인계 완결성 — 받는 측이 본 문서만으로 N 시나리오 실행 가능
- [ ] 사실 정합성 — 인용된 endpoint·DTO·ErrorCode가 실제 코드와 일치
- [ ] 작업 실행성 — 섹션 5의 모든 항목이 `path/what/why/check` 4축 정합
- [ ] 다음 버전 연결 — 섹션 7이 다음 sdd-lite의 자연스러운 시작점

## 참고 링크
- 관련 마일스톤 / Product / Epic / Story 링크
```

### 섹션 가이드

| 섹션 | 핵심 |
| --- | --- |
| **Context** | 받는 측이 BE 코드를 안 봐도 작업 완수 가능하도록 자기 완결적으로 |
| **진실 소스** | 본 문서가 옛 정보일 수 있다는 사실을 명시 + 결정적 시점 확인할 코드 경로 |
| **인벤토리** | "호출 금지" 박스는 명시적으로. 받는 측이 mock/훅을 만들면 dead code가 됨 |
| **계약** | JSON 예시 박스 — 추정 필드는 "추정"으로 태깅 |
| **작업** | 4축(path/what/why/check) 강제. 어느 하나라도 빠지면 받는 측 실행 불가 |
| **Runbook** | curl 명령 + 기대 응답 + 통과 신호 체크리스트 |
| **자가 점검** | 작성자 본인이 4 신호로 통과 여부 확정 |

---

## 예시 (간단 사례)

```
# [BC Adapter Handoff] LearningFacade → Card BC — 0.0.1v

> **본 문서의 역할**: LearningFacade BC의 새 AxisTopicSuggestionPort를 Card BC가 어떻게 호출할지 어댑터 계약 인계.
> **본 양식**: sdd-lite
> **단방향**: LearningFacade → Card BC

작성 시점: 2026-06-25 · 브랜치: feat/045-suggestion-port

---

## 1. Context
M2 Epic 3에서 Card 자동 생성 흐름이 AxisTopic 제안을 받아 카드를 채운다. 호출 측(Card BC)이 미공개 Port를 직접 부르면 안 되고, 정해진 어댑터 계약으로만 통신해야 한다. 본 문서는 그 계약을 정리.

## 2. 스냅샷
### 2.1 영향 항목
| 항목 | 카테고리 | 상태 | 영향 | 행동 |
| --- | --- | --- | --- | --- |
| AxisTopicSuggestionPort | BE 코드 | ✅ 머지 | Card BC가 의존 | 섹션 5에서 어댑터 구현 |
| StaticAxisTopicAdapter | BE 코드 | ✅ 머지 | 정적 응답 | M3에서 LLM 어댑터로 교체 예정 |

### 2.2 진실 소스
| 영역 | 경로 |
| --- | --- |
| Port 인터페이스 | `src/main/java/com/example/thirdtool/LearningFacade/application/port/out/AxisTopicSuggestionPort.java` |
| 기본 어댑터 | `src/main/java/com/example/thirdtool/LearningFacade/infrastructure/suggestion/StaticAxisTopicAdapter.java` |

## 3. 인벤토리
### 3.1 호출 가능
| 메서드 | 입력 | 출력 | 비고 |
| --- | --- | --- | --- |
| `suggest(LearningContext)` | LearningContext VO | List<AxisTopicSuggestion> | 정렬: relevance desc |

### 3.2 호출 금지
> - `StaticAxisTopicAdapter` 직접 주입 금지. Port 인터페이스로만 의존.

## 5. 작업
- [ ] Card BC 측 어댑터 클라이언트 추가 — `Card/infrastructure/learningfacade/` — Port 의존을 명시 — `./gradlew test --tests "*LearningFacadeAdapterTest"` 그린

## 6. 검증
### 6.1 사전 신호
- ApplicationContext 부팅 시 Port 빈 1개만 — `@Qualifier` 없이 주입되어야 함

## 7. Open Questions
### 7.1 LLM 어댑터 전환 시점
M3 Epic 5에서 Spring AI ChatClient 기반 어댑터로 교체. 본 계약은 그대로 유지될 예정.

## 8. 자가 점검
- [x] 인계 완결성 — Card 팀이 본 문서만 보고 어댑터 클라이언트 작성 가능
- [x] 사실 정합성 — Port 경로 검증 (라인 1)
- [x] 작업 실행성 — 5의 단일 항목 4축 정합
- [x] 다음 버전 연결 — M3 LLM 어댑터 7.1에 예고
```

---

## 참조

- 빈 스켈레톤: `./template.md`
- 첫 운영 사례: `../../../../fe-handoff/0.0.1v.md`
- 이전 스펙트럼: `../../../pes/version/0.0.1v/pes.md`
- 다음 스펙트럼 (풀 SDD): `../../../../sdd/version/0.0.1v/sdd.md`
- 양식 진화: 본 양식은 SemVer로 진화. 변경 시 `../0.0.2v/`에 새 버전을 두고 본 버전은 보존
