# 0.0.2v / Progressive Review — Story 별 리뷰 세션 결과

> **목적**: `.claude/rules/review.md` 규칙에 따라 Story 완료 → push 전 강제 발동되는 **5관점 병렬 reviewer 세션 결과**를 Story 단위로 누적한다.
>
> **원칙**: 리뷰는 push 직전. 조치 결정 이력이 이 파일에 남는다.
>
> **범위**: 5개 관점 — Domain / Architecture / API·Exception / Test / Sceptical (사용자 통찰 반영).

---

## 요약 대시보드

> D7 종료 시 집계.

| 지표 | 실측 |
| --- | --- |
| 리뷰 발동 Story 수 | ___ |
| Critical 지적 총 건수 | ___ |
| Major 지적 총 건수 | ___ |
| Minor / Nit 총 건수 | ___ |
| 조치 완료 (Critical + Major) | ___건 |
| 사용자 거부 (지적 무시 결정) | ___건 |
| 스킵 리뷰 (docs / chore 등 자동 스킵) | ___건 |

---

## 리뷰 세션 로그

> Story push 직전 발동 시점 순으로 누적. 각 세션은 아래 템플릿을 따른다.

---

### [세션 #1] Story {번호}: {제목}

- **발동 시각**: 2026-07-__ __:__
- **커밋 범위**: `{start-sha}..{end-sha}` (`git diff --stat`)
- **변경 파일 수**: 신규 __ / 수정 __ / 삭제 __
- **사용자 사전 요약 응답**: ☐ 그대로 진행 / ☐ 추가 의심 영역 알림 (내용: ___) / ☐ 정정 요청

#### Domain Reviewer

| 심각도 | 파일:라인 | 지적 | 근거 | 권장 조치 |
| --- | --- | --- | --- | --- |
| | | | | |

**종합 1문장**: ___

#### Architecture Reviewer

| 심각도 | 파일:라인 | 지적 | 근거 | 권장 조치 |
| --- | --- | --- | --- | --- |
| | | | | |

**종합 1문장**: ___

#### API / Exception Reviewer

| 심각도 | 파일:라인 | 지적 | 근거 | 권장 조치 |
| --- | --- | --- | --- | --- |
| | | | | |

**종합 1문장**: ___

#### Test Reviewer

| 심각도 | 파일:라인 | 지적 | 근거 | 권장 조치 |
| --- | --- | --- | --- | --- |
| | | | | |

**종합 1문장**: ___

#### Sceptical Reviewer (사용자 통찰 반영)

| 심각도 | 파일:라인 | 지적 | 근거 | 권장 조치 |
| --- | --- | --- | --- | --- |
| | | | | |

**종합 1문장**: ___

#### 메인 종합 & 사용자 결정

**심각도별 상위 5건**:
1. [Critical / Major / Minor / Nit] [관점] {지적} → {권장 조치}
2. ___
3. ___
4. ___
5. ___

**사용자 결정**:
- ☐ 전체 수용 → 조치 후 push
- ☐ 부분 수용 → 조치 항목: ___ / 이관: ___
- ☐ 전체 이관 → 다음 버전 후보
- ☐ 그대로 push

**조치 커밋**: `{sha}` (있는 경우)

---

### [세션 #2] Story {번호}: {제목}

> 위 템플릿 복사해서 이어 붙임.

...

---

## Reviewer 관점 정의 (참조)

| 관점 | 점검 초점 | 근거 파일 |
| --- | --- | --- |
| **Domain** | `docs/DOMAIN.md` 해당 BC vs 변경 도메인 코드 — 불변식·캡슐화·VO/팩토리 패턴·상태 전이 멱등성 | `.claude/rules/conventions.md` §1 |
| **Architecture** | `docs/PACKAGE.md` 의존 규칙 위반 — application↔presentation 누수, BC↔BC 양방향, Aggregate가 Repository 호출, Common→BC 의존 | `docs/PACKAGE.md` |
| **API / Exception** | Controller / DTO 컨벤션 — URL prefix, 메서드 매핑, ErrorCode 등록, GlobalExceptionHandler 우회, find-or-create 패턴 | `.claude/rules/conventions.md` §2 |
| **Test** | 변경된 도메인 행위 vs 테스트 매트릭스 — 해피/엣지/예외 3구분 누락, Classist/Mockist 전략 적정성, 슬라이스 테스트 누락 | `.claude/rules/conventions.md` §4 |
| **Sceptical** | 사용자 입장 비판적 검토 — "이 코드가 정말 Story AC를 충족하나", "엣지 케이스에서 깨질 만한 곳", "성급한 추상화", "불필요한 복잡도", **사용자가 §3 요약 시 알린 추가 의심 영역** | 사용자 실시간 통찰 |

---

## 세션 통계 (Epic 별)

> 각 Epic 완료 시 집계.

### LT Epic 1 (concepts[])

| Story | 세션 발동 | Critical | Major | Minor / Nit | 조치 |
| --- | --- | --- | --- | --- | --- |
| 1-1 | ☐ | ___ | ___ | ___ | ___ |
| 1-2 | ☐ | ___ | ___ | ___ | ___ |
| 1-3 | ☐ | ___ | ___ | ___ | ___ |
| 1-4 | ☐ | ___ | ___ | ___ | ___ |
| 1-5 | ☐ | ___ | ___ | ___ | ___ |

### LT Epic 2 (Layer)

| Story | 세션 발동 | Critical | Major | Minor / Nit | 조치 |
| --- | --- | --- | --- | --- | --- |
| 2-1 | ☐ | ___ | ___ | ___ | ___ |
| 2-2 | ☐ | ___ | ___ | ___ | ___ |
| 2-3 | ☐ | ___ | ___ | ___ | ___ |
| 2-4 | ☐ | ___ | ___ | ___ | ___ |
| 2-5 | ☐ | ___ | ___ | ___ | ___ |

### AS Epic 1 (Port 스켈레톤)

| Story | 세션 발동 | Critical | Major | Minor / Nit | 조치 |
| --- | --- | --- | --- | --- | --- |
| 11 | ☐ | ___ | ___ | ___ | ___ |

### 문서 (ADR022 · DOMAIN.md · PACKAGE.md)

> 리뷰 스킵 규칙 §6 — 문서 전용 변경은 스킵 가능. 스킵 시 사유 명시.

| 항목 | 세션 여부 | 스킵 사유 |
| --- | --- | --- |
| ADR022 발행 | ☐ / 스킵 | 문서 전용 |
| DOMAIN.md §LearningFacade concepts[] | ☐ / 스킵 | ___ |
| DOMAIN.md §Layer 절 | ☐ / 스킵 | ___ |
| PACKAGE.md 갱신 | ☐ / 스킵 | ___ |

---

## 반복 지적 패턴 (본 버전에서 자주 나온 지적)

> 3회 이상 반복된 지적은 다음 버전 진입 전 룰·컨벤션·문서 갱신 후보.

| 지적 패턴 | 발생 횟수 | 관점 | 다음 버전 조치 |
| --- | --- | --- | --- |
| | | | |
| | | | |

---

## 사용자 거부 결정 (지적을 수용 안 한 이력)

> 리뷰어 지적이 있었으나 명시적으로 무시한 이력. 근거 남김.

| 세션 | 지적 | 거부 사유 | 리스크 |
| --- | --- | --- | --- |
| | | | |

---

## 이관된 지적 (다음 버전 후보)

> Minor / Nit / Sceptical 중 본 버전에서 조치하지 않고 이관한 항목.

| Story | 지적 | 대상 버전 | 우선순위 |
| --- | --- | --- | --- |
| | | | |

---

## 참고

- 리뷰 규칙: `.claude/rules/review.md`
- 5관점 subagent: Domain (Explore) · Architecture (Explore) · API/Exception (Explore) · Test (Explore) · Sceptical (general-purpose)
- 병렬 발사 원칙: 한 메시지에서 5개 Agent 동시 호출
- 사전 요약 원칙: 리뷰 발사 전 사용자에게 변경 요약본 1회 제시
- 스킵 조건: 문서 전용 · 단순 chore · 자동 생성 산출물

*작성일: 2026-07-01 | 갱신 주기: Story push 직전 리뷰 세션 발동 시 | 완료 판정: D7 저녁 (요약 대시보드 집계)*
