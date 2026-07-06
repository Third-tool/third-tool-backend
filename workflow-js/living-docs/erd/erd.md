# ERD (Living)

> **성격**: living-docs — 항상 최신본. Flyway `V*.sql` + JPA `@Entity` 매핑이 진실 소스, 본 문서는 팀 참조용 관계 지도.
> **성장 방향**: 테이블이 많아지면 BC별 파일로 분화 (`erd/card.md`, `erd/deck.md`, `erd/learning.md` …).
> **관련 topology**: `workflow/topologys/versions/{Nv}/persistence.md` — PK 전략·enum 저장·soft delete 규칙은 topology.
> **소스**: `src/main/resources/db/migration/V*.sql` (현재 V1~V30).

---

## 1. BC별 테이블 그루핑

### User BC
```
user_entity ─┬─ social_member ─┬─ kakao_member
             │                 └─ naver_member
             └─ jwt_refresh_entity
```
| 테이블 | Since | 역할 |
| --- | --- | --- |
| `user_entity` | V1 | 사용자 계정 (로컬 · 소셜 통합) |
| `social_member` | V1 | 소셜 계정 상세 |
| `kakao_member` | V1 | 카카오 프로필 |
| `naver_member` | V1 | 네이버 프로필 |
| `jwt_refresh_entity` | V1 | Refresh Token 화이트리스트 |

### Deck BC
```
deck (self-referencing: parent_deck_id)
  ↳ deck_axis_material_link (V7, mapping)
  ↳ deck_progress_status (V8)
```
| 테이블 | Since | 역할 |
| --- | --- | --- |
| `deck` | V1 | 덱 (Aggregate Root, softDelete, hierarchy) |

### Card BC — **M4 재편**
```
card ─┬─ keyword_cue (1:N)
      ├─ card_status_history (V10, 상태 전이 이력)
      ├─ card_status_and_budget (V9 · budget 컬럼 V24에서 폐기)
      └─ card_tag ─── tag (V11, M:N)

M4 신규 컬럼:
- card.axis_id BIGINT NOT NULL (V28/V29/V30 · FK to learning_axis · RESTRICT)
- card.created_mode VARCHAR(20) NOT NULL (V25/V26/V27 · CHECK IN MODE_7D/14D/28D/60D)

M4 폐기 컬럼:
- card.budget_max_view (V24)
- card.budget_max_duration (V24)
- card.topic_id (V30에서 soft-deprecate · 컬럼 유지 · 다음 릴리스 DROP 예정)

M4 재정의:
- card.archive_reason CHECK 재작성 (V24) — MANUAL/SCHEDULE_EXHAUSTED/MODE_DOWNGRADED
```
| 테이블 | Since | 역할 |
| --- | --- | --- |
| `card` | V1 · **M4 재편** | 카드 (Aggregate Root, softDelete · axis_id NOT NULL · created_mode · ArchiveReason 3값) |
| `keyword_cue` | V1 | 카드 키워드 단서 (1:N with card) |
| `card_status_history` | V10 | 카드 상태 전이 이력 |
| `tag` | V11 | 태그 (시스템 전역 유니크 value) |
| `card_tag` | V11 | 카드 ↔ 태그 매핑 (M:N, 카드당 최대 3개) |

### Review BC
| 테이블 | Since | 역할 |
| --- | --- | --- |
| (Review 관련 테이블은 별도 V 파일 스캔 필요) | — | ReviewSession · ReviewCard |

### LearningFacade BC
```
learning_facade (softDelete)
├── learning_facade_concept (V16, 1~5개)
├── learning_layer (V18, softDelete, @SQLRestriction)
│   └── learning_axis (V2, softDelete since V14, layer_id FK since V19)
│       ├── axis_topic (V2)
│       │   ├── axis_topic_deletion (V5)
│       │   ├── axis_roadmap_node (V20, 챕터 노드 first-class)
│       │   ├── axis_selection (V21, 판례 컨테이너)
│       │   │   └── axis_selection_node (V22)
│       │   └── topic_revision (V3)
│       │       └── revision_reason_option (V3, 사유 옵션)
│       └── (axis_topic_revision_count: V4)
└── learning_material (V2, softDelete since V6)
    └── topic_material (V2, M:N with axis_topic, 매핑)
```
| 테이블 | Since | 역할 |
| --- | --- | --- |
| `learning_facade` | V2 | LearningFacade Aggregate Root (user당 1개, softDelete) |
| `learning_facade_concept` | V16 | concepts[] (1~5개, displayOrder 1-based) |
| `learning_layer` | V18 | Axis 상위 그룹핑 계층 (softDelete, `default Uncategorized` 앵커) |
| `learning_axis` | V2 | 세부 축 (softDelete since V14, layer_id FK since V19) |
| `axis_topic` | V2 | 주제 (displayOrder 1-based) |
| `axis_topic_deletion` | V5 | 주제 삭제 이력 |
| `axis_roadmap_node` | V20 | Roadmap 챕터 노드 (V5의 axis_roadmap.content TEXT SUPERSEDED) |
| `axis_selection` | V21 | Selection 컨테이너 (name UNIQUE per axis, hard delete) |
| `axis_selection_node` | V22 | Selection 노드 (컨테이너 hard delete 정책 계승) |
| `topic_revision` | V3 | 주제 이름 변경 이력 |
| `revision_reason_option` | V3 | 수정 사유 옵션 마스터 |
| `learning_material` | V2 | 학습 자료 (별도 도메인, softDelete) |
| `topic_material` | V2 | 주제 ↔ 자료 매핑 (M:N) |

### UserSchedule BC — **M4 재편**
| 테이블 | Since | 역할 |
| --- | --- | --- |
| `user_schedule_config` | V12 · **M4 재편** | 학습 모드 (**7D/14D/28D/60D · V23 재정의**) · Budget 매핑 **V23에서 제거** |
| `user_schedule_config_history` | V12 | 스케줄 변경 이력 |
| (daily_target 컬럼) | V13 | 일일 목표 카드 수 |

---

## 2. Flyway 마이그레이션 스택 (V1~V30)

| V | 파일명 | 목적 |
| --- | --- | --- |
| V1 | `init.sql.sql` | 초기 — user_entity · social_member · kakao_member · naver_member · deck · card · keyword_cue · jwt_refresh_entity |
| V2 | `learning_facade.sql` | learning_facade · learning_axis · axis_topic · learning_material · topic_material |
| V3 | `topic_revision.sql` | revision_reason_option · topic_revision |
| V4 | `axis_topic_revision_count.sql` | axis_topic revision 카운터 |
| V5 | `axis_topic_deletion.sql` | axis_topic 삭제 이력 |
| V6 | `learning_material_type_extension.sql` | 자료 유형 확장 |
| V7 | `deck_axis_material_link.sql` | 덱-축-자료 링크 |
| V8 | `deck_progress_status.sql` | 덱 진행 상태 |
| V9 | `card_status_and_budget.sql` | 카드 상태 + budget |
| V10 | `card_status_history.sql` | 카드 상태 전이 이력 |
| V11 | `tag_and_card_tag.sql` | tag · card_tag |
| V12 | `user_schedule_config.sql` | user_schedule_config + history |
| V13 | `user_schedule_config_daily_target.sql` | daily_target 컬럼 |
| V14 | `learning_axis_soft_delete.sql` | learning_axis softDelete (ADR021) |
| V15 | `deck_axis_not_null_and_orphan_softdelete.sql` | deck_axis NOT NULL + orphan soft delete |
| V16 | `learning_facade_concept.sql` | learning_facade_concept 테이블 신설 |
| V17 | `learning_facade_concept_backfill.sql` | 기존 concept 값 → 자식 테이블 백필 (NOT EXISTS 서브쿼리, idempotent) |
| V18 | `learning_layer.sql` | learning_layer 신설 (`UNIQUE(facade_id, name, deleted_at)` 3-col composite) |
| V19 | `learning_axis_layer_fk.sql` | learning_axis.learning_layer_id FK 재배선 (3-phase 안전 이관) |
| V20 | `axis_roadmap_node.sql` | axis_roadmap_node (챕터 노드 first-class) |
| V21 | `axis_selection.sql` | axis_selection (판례 컨테이너) |
| V22 | `axis_selection_node.sql` | axis_selection_node |
| V23 | `reorganize_learning_mode.sql` | **M4 CARD E1** — LearningMode 4개 값 재정의 · 기존 `MODE_10D` → `MODE_7D` 자동 마이그레이션 · user_schedule_config budget 컬럼 제거 |
| V24 | `abolish_onfieldbudget.sql` | **M4 CARD E2** — OnFieldBudget 컬럼 제거 · ArchiveReason CHECK 재작성 (MANUAL/SCHEDULE_EXHAUSTED/MODE_DOWNGRADED) · 기존 archived 카드 재매핑 |
| V25 | `add_card_created_mode_1_nullable.sql` | **M4 CARD E3 3-phase 1단계** — `card.created_mode` NULL 컬럼 추가 |
| V26 | `add_card_created_mode_2_backfill.sql` | **M4 CARD E3 3-phase 2단계** — 백필 (기본값 `MODE_28D`) |
| V27 | `add_card_created_mode_3_notnull.sql` | **M4 CARD E3 3-phase 3단계** — NOT NULL 승격 + CHECK IN (MODE_7D/14D/28D/60D) |
| V28 | `card_axis_id_1_nullable.sql` | **M4 LT E4 3-phase 1단계** — `card.axis_id BIGINT NULL` 컬럼 추가 |
| V29 | `card_axis_id_2_backfill.sql` | **M4 LT E4 3-phase 2단계** — `topic_id → axis_id` JOIN 백필 + 검증 SQL |
| V30 | `card_axis_id_3_notnull.sql` | **M4 LT E4 3-phase 3단계** — NOT NULL 승격 · `topic_id` soft-deprecate · FK RESTRICT to learning_axis |

---

## 3. 공통 매핑 규칙

| 규칙 | 상세 |
| --- | --- |
| PK | 모든 테이블 `BIGINT NOT NULL AUTO_INCREMENT` (ADR001) |
| Enum | `VARCHAR(≤20) + CHECK 제약` + `@Enumerated(EnumType.STRING)` (ADR002). ORDINAL 금지 |
| Soft Delete | `deleted_at DATETIME(6) NULL` + `@SQLRestriction("deleted_at IS NULL")` + `@SQLDelete` (ADR003) |
| Composite UNIQUE | `UNIQUE(parent_id, name, deleted_at)` — softDeleted 이름 재사용 허용 (MySQL NULL 취급 활용) |
| audit | `created_at DATETIME(6) NOT NULL`, `updated_at DATETIME(6) NOT NULL` (수정 있는 엔티티만) |
| displayOrder | `INT NOT NULL CHECK (>= 0)` (안전망). 도메인 의미는 1-based |
| 자연 인덱스 | `(parent_id, display_order)` 커버링, `(fk_column)` 단독 |

---

## 4. Soft Delete 적용 vs 비적용

**적용 (사용자 자산성)**:
- `user_entity`, `deck`, `card`, `learning_facade`, `learning_material`, `learning_axis` (V14 이후, ADR021), `learning_layer` (V18)

**비적용 (연결 사실 · 이력)**:
- `axis_topic`, `topic_material`, `card_tag`, `keyword_cue` — 매핑·구조 편집. hard delete
- `card_status_history`, `topic_revision`, `axis_topic_deletion`, `user_schedule_config_history` — 이력 (append-only)
- `tag` — 시스템 전역 리소스
- `axis_selection`, `axis_selection_node` — 컨테이너 hard delete 정책 (V21~V22 명시)

---

## 5. 관계 다이어그램 (텍스트) — **M4 반영**

```
user_entity ────┬──── deck ──── (card.deck_id 유지 · 단 axis_id가 M4 주 참조) ──── keyword_cue
                │                      │
                │                      ├──── card_status_history
                │                      │
                │                      └──── card_tag ──── tag
                │
                │        deck ─── deck_axis_material_link (M5에 Deck BC 완전 폐기 예정)
                │
                ├──── learning_facade ──── learning_facade_concept
                │            │
                │            ├──── learning_layer
                │            │       │
                │            │       └── learning_axis (FK: layer_id)  ◄── card.axis_id NOT NULL (V30, M4 신설 · FK RESTRICT)
                │            │              │
                │            │              └── axis_topic ── (card.topic_id soft-deprecate · V30)
                │            │                   ├── topic_material ── learning_material
                │            │                   ├── topic_revision ── revision_reason_option
                │            │                   ├── axis_topic_deletion
                │            │                   ├── axis_roadmap_node
                │            │                   └── axis_selection
                │            │                          └── axis_selection_node
                │            │
                │            └──── learning_material (별도)
                │
                └──── user_schedule_config ── user_schedule_config_history
                                                jwt_refresh_entity
```

**M4 핵심 관계 변화**:
- Card → LearningAxis 직접 참조 (V28~V30) · `card.axis_id NOT NULL`
- Card → AxisTopic 간접 참조 (`card.topic_id`) 는 soft-deprecate · 다음 릴리스 DROP
- Card.created_mode 필드 신설 (V25~V27) · 유저 mode 다운 시 하이브리드 판정

---

## 6. 참조

- 진실 소스: `src/main/resources/db/migration/V*.sql` + `@Entity` 매핑
- 관련 topology: `workflow/topologys/versions/{Nv}/persistence.md`
- 관련 living-docs: `architecture-system-design/architecture.md`
- 규범: `.claude/rules/conventions.md` §3 (DB·JPA)
- ADR: ADR001 (PK), ADR002 (Enum), ADR003 (Soft Delete), ADR021 (LearningAxis softDelete 확장)

*최신 갱신: 2026-07-21 · **M4 반영** — Flyway V23~V30 추가 (Card BC 재편 · Card→Axis 3-phase) · `card.axis_id`/`created_mode` 신설 · `budget_*`/`topic_id` 폐기·soft-deprecate · UserSchedule LearningMode 4값 재정의*
