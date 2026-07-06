# Index Catalog (Living)

> **성격**: living-docs — 항상 최신본. 현재 DB에 존재하는 인덱스 카탈로그.
> **성장 방향**: 인덱스가 많아지면 테이블별 파일로 분화.
> **관련 topology**: `workflow/topologys/index/v1-index.md` (설계 원칙·CREATE 기준 pin).
> **원본**: `src/main/resources/db/migration/V*.sql`.

---

## 규칙 요약 (topology 발췌)

- 컬럼 순서: **등가 필터 → 범위 필터 → 정렬**.
- FK 컬럼은 인덱스 필수 (MySQL은 자동 인덱스 X).
- Repository 메서드 시그니처가 인덱스 존재의 1차 근거.
- Partial 인덱스는 MySQL 미지원 → `@SQLRestriction` + 일반 인덱스.
- 인덱스 CREATE/DROP은 Flyway V 파일로만.

---

## 인덱스 카탈로그 (BC · 테이블별)

### User BC
| 테이블 | 인덱스 이름 | 컬럼 | 종류 | 도입 V | 용도 |
| --- | --- | --- | --- | --- | --- |
| `user_entity` | `uk_user_entity_username` | (username) | UNIQUE | V1 | 로그인 · 중복 방지 |
| `social_member` | `uk_social_member_social_id` | (social_id) | UNIQUE | V1 | 소셜 계정 중복 방지 |

### Deck BC
| 테이블 | 인덱스 이름 | 컬럼 | 종류 | 도입 V | 용도 |
| --- | --- | --- | --- | --- | --- |
| `deck` | `uk_deck_user_name` | (user_id, name) | UNIQUE | V1 | 사용자별 덱 이름 중복 방지 |
| `deck` | `idx_deck_axis` | (axis_id) | 단독 | V7 | FK 조회 · axis→deck 매핑 |
| `deck` | `idx_deck_learning_material` | (learning_material_id) | 단독 | V7 | FK 조회 (레거시, 삭제 검토 대상) |
| `deck` | `idx_deck_progress_status` | (progress_status) | enum 필터 | V8 | 진행 상태별 조회 |

### Card BC — **M4 신규 인덱스**
| 테이블 | 인덱스 이름 | 컬럼 | 종류 | 도입 V | 용도 |
| --- | --- | --- | --- | --- | --- |
| `card` | `idx_card_status` | (status) | enum 필터 | V9 | ON_FIELD/ARCHIVE 필터 |
| `card` | `idx_card_last_viewed_at` | (last_viewed_at) | 범위 필터 | V9 | soft schedule 조회 |
| `card` | `idx_card_axis_id` | (axis_id) | 단독 (FK) | **V30 (M4 LT E4)** | axis 하위 카드 조회 · `findByAxisIdAndStatus` |
| `card` | `idx_card_axis_status` | (axis_id, status) | composite (등가+등가) | **V30 (M4 LT E4)** | axis별 활성 카드 조회 · Coverage 재계산 소스 |
| `card_status_history` | `idx_card_status_history_card_id` | (card_id) | 단독 | V10 | FK 조회 |
| `card_status_history` | `idx_card_status_history_changed_at` | (changed_at) | 범위 | V10 | 시간축 조회 |
| `tag` | (UNIQUE는 value 컬럼) | (value) | UNIQUE | V11 | 시스템 전역 태그 유니크 |
| `card_tag` | `idx_card_tag_card_id` | (card_id) | 단독 | V11 | 양방향 M:N 조회 |
| `card_tag` | `idx_card_tag_tag_id` | (tag_id) | 단독 | V11 | 양방향 M:N 조회 |

**M4 폐기 예정 인덱스** (다음 릴리스): `idx_card_topic_id` (있었다면) · `card.topic_id` 컬럼 DROP 시 함께.

### LearningFacade BC
| 테이블 | 인덱스 이름 | 컬럼 | 종류 | 도입 V | 용도 |
| --- | --- | --- | --- | --- | --- |
| `learning_facade` | (UNIQUE user_id) | (user_id) | UNIQUE | V2 | 유저당 Facade 1개 |
| `learning_axis` | `idx_learning_axis_facade_order` | (learning_facade_id, display_order) | composite (parent+order) | V2 | facade 하위 축 목록 · 커버링 |
| `learning_axis` | (UNIQUE `uk_learning_axis_facade_name`) | (facade_id, name, deleted_at) | UNIQUE 3-col composite | V14 (softDelete 승격) | softDeleted 이름 재사용 허용 |
| `learning_axis` | `idx_learning_axis_deleted` | (deleted_at) | 단독 | V14 | `@SQLRestriction` 대응 |
| `learning_axis` | `idx_learning_axis_layer` | (learning_layer_id) | 단독 (FK) | V19 | Layer 하위 axis 조회 |
| `axis_topic` | `idx_axis_topic_axis_order` | (learning_axis_id, display_order) | composite (parent+order) | V2 | 축 하위 주제 목록 · 커버링 |
| `axis_topic` | `idx_axis_topic_coverage` | (coverage_status) | enum 필터 | V2 | 커버리지 상태별 필터 |
| `learning_material` | `idx_learning_material_facade` | (learning_facade_id) | 단독 (FK) | V2 | Facade 자료 조회 |
| `topic_material` | `idx_topic_material_topic` | (axis_topic_id) | 단독 | V2 | 양방향 M:N |
| `topic_material` | `idx_topic_material_material` | (learning_material_id) | 단독 | V2 | 양방향 M:N |
| `topic_material` | (UNIQUE) | (topic_id, material_id) | UNIQUE composite | V2 | 매핑 중복 방지 |
| `topic_revision` | `idx_topic_revision_topic_revised` | (axis_topic_id, revised_at) | composite (parent+time) | V3 | 주제별 이력 시간순 |
| `revision_reason_option` | `idx_revision_reason_active_order` | (active, display_order) | composite (필터+정렬) | V3 | 활성 옵션 정렬 조회 |
| `axis_topic_deletion` | `idx_axis_topic_deletion_axis_deleted_at` | (learning_axis_id, deleted_at) | composite (parent+time) | V5 | 축별 삭제 이력 시간순 |
| `learning_facade_concept` | `idx_learning_facade_concept_facade_order` | (learning_facade_id, display_order) | composite (parent+order) | V16 | facade 컨셉 순서 · 커버링 |
| `learning_facade_concept` | (UNIQUE) | (facade_id, concept_value) | UNIQUE composite | V16 | 컨셉 중복 방지 |
| `learning_layer` | `idx_learning_layer_facade_order` | (learning_facade_id, display_order) | composite (parent+order) | V18 | facade 레이어 순서 |
| `learning_layer` | `idx_learning_layer_deleted` | (deleted_at) | 단독 | V18 | `@SQLRestriction` 대응 |
| `learning_layer` | (UNIQUE) | (facade_id, name, deleted_at) | UNIQUE 3-col composite | V18 | softDeleted 이름 재사용 허용 |
| `axis_roadmap_node` | `idx_axis_roadmap_node_axis_order` | (axis_id, display_order) | composite (parent+order) | V20 | 축 하위 챕터 순서 |
| `axis_roadmap_node` | `idx_axis_roadmap_node_deleted` | (deleted_at) | 단독 | V20 | soft delete 대응 |
| `axis_selection` | `idx_axis_selection_axis_created` | (axis_id, created_at DESC) | composite (parent+time DESC) | V21 | 축 하위 Selection 최신순 |
| `axis_selection` | (UNIQUE) | (axis_id, name) | UNIQUE composite | V21 | Selection 이름 중복 방지 (hard delete 정책) |
| `axis_selection_node` | `idx_axis_selection_node_selection_order` | (selection_id, display_order) | composite (parent+order) | V22 | 컨테이너 하위 노드 순서 |

### UserSchedule BC
| 테이블 | 인덱스 이름 | 컬럼 | 종류 | 도입 V | 용도 |
| --- | --- | --- | --- | --- | --- |
| `user_schedule_config` | (UNIQUE user_id) | (user_id) | UNIQUE | V12 | 유저당 config 1개 |
| `user_schedule_config_history` | `idx_user_schedule_config_history_config_id` | (config_id) | 단독 (FK) | V12 | config 이력 조회 |
| `user_schedule_config_history` | `idx_user_schedule_config_history_changed_at` | (changed_at) | 범위 | V12 | 시간축 조회 |

---

## Composite 인덱스 컬럼 순서 원칙 준수 확인

topology 원칙: **등가 필터 → 범위 → 정렬**.

| 인덱스 | 컬럼 순서 | 원칙 준수 |
| --- | --- | --- |
| `idx_learning_axis_facade_order` | (facade_id 등가, display_order 정렬) | ✅ |
| `idx_axis_topic_axis_order` | (axis_id 등가, display_order 정렬) | ✅ |
| `idx_learning_facade_concept_facade_order` | (facade_id 등가, display_order 정렬) | ✅ |
| `idx_topic_revision_topic_revised` | (topic_id 등가, revised_at 범위/정렬) | ✅ |
| `idx_axis_topic_deletion_axis_deleted_at` | (axis_id 등가, deleted_at 범위) | ✅ |
| `idx_axis_selection_axis_created` | (axis_id 등가, created_at DESC 정렬) | ✅ |
| `idx_revision_reason_active_order` | (active 등가, display_order 정렬) | ✅ |
| `uk_learning_axis_facade_name` | (facade_id 등가, name 등가, deleted_at 등가) | ✅ (UNIQUE 정합) |

---

## 커버링 인덱스 후보

Repository 메서드가 자주 SELECT하는 컬럼이 인덱스에 다 포함되면 커버링. 현재 명시적 커버링 인덱스는 다음:

- `idx_learning_axis_facade_order` — `SELECT id, learning_facade_id, display_order` 시 커버링 가능
- `idx_learning_layer_facade_order` — 동상
- 대부분 composite (parent+order)는 목록 조회 시 커버링 후보

**주의**: 커버링 여부는 SELECT 컬럼 목록에 의존. `EXPLAIN`으로 실제 확인 필요.

---

## 정리 후보 (삭제·재검토)

| 인덱스 | 이유 | 조치 |
| --- | --- | --- |
| `idx_deck_learning_material` | Deck.learningMaterialId 자체가 레거시. Fix-Story 1 이후 신규 Deck은 항상 null. | 컬럼 삭제 시 함께 |
| — | (다른 미사용 후보는 EXPLAIN + Repository 메서드 대조로 발굴) | |

---

## 감시 포인트

topology `v1-index.md` §2 Invariants:
- FK 컬럼에 인덱스 존재 여부 (`SHOW INDEX` diff)
- composite 순서 원칙 위반 (등가 → 범위 → 정렬)
- Repository 참조 없는 인덱스 신설 방지
- `@Index` JPA vs Flyway 정의 drift

---

## 참조

- 원본 SQL: `src/main/resources/db/migration/V*.sql`
- 관련 topology: `workflow/topologys/index/v1-index.md`
- 관련 living-docs: `erd/erd.md` (테이블 관계 지도)

*최신 갱신: 2026-07-21 · **M4 반영** — Flyway V23~V30 인덱스 추가 (`idx_card_axis_id`·`idx_card_axis_status` M4 LT E4 신설) · 총 ~32개 인덱스*
