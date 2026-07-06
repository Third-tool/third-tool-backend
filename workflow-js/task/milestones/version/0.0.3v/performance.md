# 0.0.3v / Performance — 스킵 사유 명시

> **결정**: 0.0.3v 스코프는 **정밀 성능 측정 미수행**. 사유는 아래 §"왜 스킵인가".
> **본 파일 역할**: 스킵 결정 근거 · M3 신규 자산에서 관찰된 정성 지표 · 다음 버전 재측정 트리거 · 로컬 관찰 요령 기록.
> 상세 배포·k6 성능 baseline은 배포 재개 마일스톤(M7 이후)에서 실측 예정.

---

## 왜 스킵인가

M2와 동일한 스킵 정책 유지 + M3 특성 반영:

1. **배포 미포함** — Prometheus / Grafana / k6 부하 테스트 등 정밀 도구가 dev·prod 환경에 있어야 함. 본 버전은 로컬 스코프.
2. **AI 응답이 아직 static** — chapters-outline 응답은 classpath JSON에서 조회. LLM 실호출 대비 성능 특성이 완전히 다름. LLM Adapter (M6) 도입 후 baseline 재실측 필요.
3. **속도 우선** — Reviewer 5관점 세션 재개했지만 정밀 성능 측정은 여전히 rush 정책 유지.
4. **정확도 대비 시간 비용** — 로컬 개발자 하드웨어 종속 실측은 상대값. baseline이 명확한 배포 환경에서 재측정이 훨씬 신뢰도 높음.

---

## 대신 확인한 정성 지표 (본 버전 진행 중 관찰)

| 관찰 | 방법 | 결과 |
| --- | --- | --- |
| `./gradlew test` 전체 통과 | 매 PR 완료 시점 (3회) | ✅ BUILD SUCCESSFUL 유지 |
| 전체 테스트 스위트 소요 | 세션 로그 실측 | ~**3분 20초** (Windows / OpenJDK 21) — M2 (~2분 30초) 대비 ~30% 증가 (신규 100+ 테스트 반영) |
| PR별 관련 테스트 | Epic 완료 시점 | ✅ 각 PR 완료 후 45~55초 (해당 BC만 실행 시) |
| `./gradlew.bat compileJava` 시간 | 각 도메인/Adapter 편집 후 | ✅ 15~45초 유지 (Java 21 + Gradle 8.x) |
| Repository Slice 테스트 시간 | `@DataJpaTest` 클래스별 | ✅ 개별 클래스 3~5초 · V20~V22 마이그레이션 이후에도 유지 |
| Reviewer 5관점 병렬 발사 소요 | Agent tool 실행 로그 | 각 세션 ~40~60초 (5 subagent 동시) · 총 3회 발사 = ~180초 |
| Static Adapter classpath JSON 로드 | 최초 요청 시 (cold cache) | ~ 수십 ms 예상 (실측은 배포 시점) |
| AI 응답 body 크기 | `SuggestionAppServiceTest` 통과 시 검증 | subtree 통짜 최대 ~2KB (backend-developer.json 실측) |

**정성 판정**: 도메인 재편(V20~V22 신설 + 신규 도메인 3종 + 6-Port + 4 Adapter)에 의한 급 회귀 신호 없음. 정밀 수치 baseline은 배포 재개 후.

---

## 관찰 가능한 성능 영향 지점 (다음 버전 실측 대상)

### 1. SuggestionCatalogLoader 캐시 재로드 오버헤드

- **로직**: `ConcurrentHashMap<role, SuggestionCatalog>` · `load(role)`에서 `computeIfAbsent`로 파일 1회 로드 후 캐시. `backend-developer.json` ~90 라인.
- **예상 첫 로드**: ~수십 ms (Jackson 파싱 + classpath 리소스 스트림).
- **최악 케이스**: role 다각화 시 (planner/designer/problem-solver 신설) 각 첫 요청마다 다른 파일 로드.
- **재측정**: 배포 재개 시 `SuggestionCatalog 로드 1회당 ms` 지표 추가 검토.

### 2. 3계층 Aggregate 로딩 N+1 위험 (Facade → Axis → RoadmapNodes / Selections → SelectionNodes)

- **위험 지점**: `LearningAxis` 안에 `topics`, `roadmapNodes`, `selections` 3개 컬렉션. 각각 `@OneToMany(fetch=LAZY)`. `AxisSelection` 안엔 `nodes` (LAZY).
- **worst case 시나리오**: `GET /facades/me`가 layers→axes→(roadmapNodes+selections+selectionNodes) 전부 fetch 시 SQL이 폭발 가능.
- **현재 상태**: `LearningFacadeQueryService`는 axes/topics만 반환하는 기존 응답 유지. Roadmap/Selection은 별도 REST endpoint (`GET /axes/{axisId}/roadmap-nodes` 등)로 분리되어 있어 N+1 문제 제어됨.
- **재측정**: 배포 재개 시 Hibernate SQL 카운트 (`hibernate.session.metrics`) 활성화 · `spring.jpa.properties.hibernate.generate_statistics=true`.

### 3. AxisRoadmapNode `@SQLRestriction` deleted 필터 인덱스 활용

- **로직**: 모든 조회에 `WHERE deleted_at IS NULL` 자동 추가. V20에 `idx_axis_roadmap_node_deleted (deleted_at)` 인덱스 배치.
- **예상 부하**: 활성 노드 수 (사용자당 최대 ~50) 매우 작음 · index range scan 유지.
- **재측정**: 이후 삭제 노드 축적 시 (수개월 후) MySQL `EXPLAIN` 실측.

### 4. AxisSelection.reorderNodes 순회 · CASCADE 삭제 성능

- **reorderNodes 로직**: `orderedNodeIds` 리스트 순회 O(N) + 각 node.updateDisplayOrder → JPA dirty check.
- **삭제 CASCADE**: 컨테이너 hard delete → orphanRemoval=true → 자식 노드 CASCADE. 자식 수 <=20 일반.
- **worst case**: 하드 리셋 (컨테이너 여러 개 · 각 50 노드) 시 삭제 지연 가능. 로컬 무의미.
- **재측정**: k6 시나리오 (컨테이너 재생성 부하) M7 이후.

### 5. Reviewer 세션 병렬 발사 총 소요 (개발 프로세스 관점)

- **로직**: 5 subagent (Domain/Architecture/API/Test/Sceptical) 동시 병렬 발사 · 각각 read-only 파일 스캔.
- **M3 실측**: PR#3 ~50초, PR#4 ~60초, PR#5 ~90초 (파일 수와 SDD 스캔 범위에 비례).
- **관찰 지점**: PR 규모가 커질수록 subagent 스캔 시간 선형 증가. Sceptical Reviewer가 catalog 콘텐츠까지 판정 시 최장.
- **재측정 트리거**: Reviewer 세션이 개발 병목이 되면 (예: PR당 5분 초과) subagent 분업/캐시 검토.

---

## 성능 도구 준비 상태 (다음 버전 활용)

| 도구 | 상태 | 활용 계획 |
| --- | --- | --- |
| Actuator `/actuator/metrics` | 미도입 (M2와 동일) | dev 배포 재개 시 Spring Boot Actuator 등록 |
| Prometheus | dev 환경 가동 (M1 산출) | 배포 재개 후 M2~M3 신규 endpoint 스크레이핑 재개 |
| Grafana | 대시보드 유지 (M1 산출) | M3 신규 18 endpoint 대응 대시보드 추가 필요 |
| k6 부하 테스트 (VU 50) | 미실행 (Product 9) | M7 배포 재개 후 첫 실행 · 신규 API baseline |
| JVM `-verbose:gc` / `jstat` | 로컬 언제든 가능 | 로컬 이슈 발생 시 즉시 |
| `./gradlew test --info` | 상시 활용 | 느린 테스트 클래스 발견 (M3에도 소요 3분 20초 유지) |
| Hibernate SQL count (`spring.jpa.properties.hibernate.generate_statistics=true`) | 미활성 | 3계층 Aggregate 로딩 N+1 감지 시 활성 |
| AI 관측 지표 (`thirdtool.suggestion.*`) | 미배선 (이슈 #20 v1 관찰만) | LLM Adapter 도입 (M6) 동시 배선 |

---

## 트리거 — 다음 버전 성능 재측정 조건

다음 중 최소 1개 성립 시 재측정 착수:

1. **배포 재개 (M7 이후)** — prod-like MySQL 환경에서 V20~V22 실제 실행 시간 baseline
2. **`GET /facades/me` 응답 크기 UX 이슈 부상** — layers + axes + (roadmapNodes/selections) 계층으로 응답이 크다는 사용자 피드백
3. **N+1 SQL 로그 발견** — Aggregate 3계층 로딩 시 SQL count 급증 관찰
4. **LLM Adapter 도입 (AS Epic 4 · M6)** — Static → LLM 교체 시 응답 시간 변동 baseline 필요
5. **k6 부하 테스트 (Product 9 · M7)** — VU 50 · 지속 5분 시나리오 실행
6. **Reviewer 5관점 세션이 개발 병목화** — PR당 소요 5분 초과 시 subagent 최적화 검토

---

## 관찰 원칙 (로컬)

- **회귀 여부는 상대값**: 로컬 개발자 하드웨어에 좌우되므로 절대값 baseline 불신. "이전 실행 대비" 비교만 유효.
- **테스트 통과 = 최소 조건**: 성능 회귀는 통과 여부로 감지 안 됨. 별도 관찰 필요.
- **정밀 성능은 배포 이후**: 로컬 성능 실측은 참고용, prod 실측이 진실 소스.
- **Static Adapter 응답 시간은 무의미**: classpath JSON 조회는 ms 단위. LLM Adapter 도입 후 초 단위 응답 시간 baseline이 실질 관찰 대상.

---

## 참고

- k6 부하 테스트 계획: `../../pes/workspectrum/sdd/in-progress/product-load-test.md` (M7 이후)
- Prometheus 지표 정의: `../../pes/workspectrum/sdd/in-progress/product-op.md` (M1 산출)
- 배포 파이프라인: `../../pes/workspectrum/sdd/in-progress/product-infra-deploy.md`
- AI 관측성 (이슈 #20): `../../pes/workspectrum/sdd/in-progress/product-ai-suggestion.md` §Epic 6 (관측성 · M7)
- 상세 배포 재개 조건: `infra.md` §"M4 이후 인프라 재개 조건"

*작성일: 2026-07-03 | 스킵 사유: 로컬 스코프 + Static Adapter 단계 · 정밀 baseline은 LLM Adapter 도입 후에만 유효 | 재측정 트리거 명시 | 배포 재개 시점(M7+)에 정밀 baseline*
