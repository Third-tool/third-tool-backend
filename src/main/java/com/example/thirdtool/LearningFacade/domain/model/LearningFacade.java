package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "learning_facade")
public class LearningFacade {

    private static final int RECOMMENDED_AXIS_LIMIT = 5;

    // ─── concepts[] 정책 상수 (Story-LT-E1-S2 / S5) ────────
    // Application Service · Controller · FE 어디도 재정의 금지 — 도메인 단일 진실 소스.
    public static final int MIN_CONCEPT_COUNT        = 1;
    public static final int MAX_CONCEPT_COUNT        = 5;
    public static final int MAX_CONCEPT_VALUE_LENGTH = 100;

    // ─── 식별자 ───────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_facade_id")
    private Long id;

    // ─── 소유 사용자 ──────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    /**
     * 단일 concept 컬럼 (Legacy, Story-LT-E1-S1 이후 deprecated).
     * <p>NOT NULL 유지 상태이며 컬럼 DROP은 별도 릴리스에서 처리한다.
     * 도메인은 {@link #concepts} 컬렉션을 진실 소스로 사용하고,
     * 이 필드는 backfill/backward-compat 용으로 첫 concept 값과 동기화된다.
     */
    @Column(name = "concept", nullable = false, length = MAX_CONCEPT_VALUE_LENGTH)
    private String concept;

    // ─── concepts[] 컬렉션 (Story-LT-E1-S2) ────────────────
    // learning_facade_concept 자식 테이블 매핑. 노출은 getConcepts()의 unmodifiableList로만.
    @OneToMany(
            mappedBy      = "facade",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    private final List<LearningFacadeConcept> concepts = new ArrayList<>();

    // ─── Layer 목록 (Story-LT-E2-S1/S2) ────────────────────
    // learning_layer 자식 테이블 매핑. 소프트 삭제된 layer는 @SQLRestriction으로 자동 제외.
    @OneToMany(
            mappedBy      = "facade",
            cascade       = CascadeType.ALL,
            orphanRemoval = false,
            fetch         = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    private final List<LearningLayer> layers = new ArrayList<>();

    // ─── 세부 축 목록 ─────────────────────────────────────
    // ⚠️ orphanRemoval=false (Fix — Axis↔Deck 완전 통합, SDD §12 Q1 결정):
    //   LearningAxis는 Soft Delete 정책 (deleted_at). removeAxis()는 axes.remove()가 아닌
    //   target.softDelete()만 호출한다. orphanRemoval=true를 유지하면 미래의 리팩토링에서
    //   axes.removeIf(LearningAxis::isDeleted) 같은 코드가 조용히 hard delete를 발동시켜
    //   Soft Delete 데이터를 손실할 위험이 있다 (Reviewer Sceptical 지적).
    //   → axes 컬렉션에서 element를 remove하는 코드를 넣지 말 것. 삭제는 오직 softDelete()로.
    @OneToMany(
            mappedBy      = "facade",
            cascade       = CascadeType.ALL,
            orphanRemoval = false,
            fetch         = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    private final List<LearningAxis> axes = new ArrayList<>();

    // ─── 시각 ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private LearningFacade(UserEntity user, String concept) {
        this.user    = user;
        this.concept = concept;
    }

    // ─── 생성 ─────────────────────────────────────────────

    public static LearningFacade create(UserEntity user, String concept) {
        requireNonNull(user, "user");
        validateConcept(concept);
        LearningFacade facade = new LearningFacade(user, concept.trim());
        // Story-LT-E1-S2: 단수 concept 생성 시 concepts 컬렉션에도 첫 항목으로 동기 추가한다.
        // 도메인은 concepts를 진실 소스로 사용하므로 생성 시점에 collection과 legacy 필드가
        // 언제나 일관된 상태여야 한다.
        facade.concepts.add(LearningFacadeConcept.of(facade, concept, 1));
        // Story-LT-E2-S3: 신규 Facade에 default Uncategorized Layer 1건 자동 생성.
        facade.layers.add(LearningLayer.of(facade, LearningLayer.DEFAULT_LAYER_NAME, 1));
        return facade;
    }

    /**
     * concepts 다건 진입점 (Story-LT-E1-S4).
     * size/blank/중복 검증 후 legacy {@code concept}은 첫 값으로 초기화되고 컬렉션은 1..N 순서로 채워진다.
     */
    public static LearningFacade create(UserEntity user, List<String> newConcepts) {
        requireNonNull(user, "user");
        if (newConcepts == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "concepts는 null일 수 없습니다."
            );
        }
        if (newConcepts.size() < MIN_CONCEPT_COUNT || newConcepts.size() > MAX_CONCEPT_COUNT) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID,
                    "size=" + newConcepts.size()
            );
        }
        List<String> normalized = new ArrayList<>();
        for (String v : newConcepts) {
            String n = LearningFacadeConcept.normalizeValue(v);
            if (normalized.contains(n)) {
                throw LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_FACADE_CONCEPT_DUPLICATE,
                        "value=" + n
                );
            }
            normalized.add(n);
        }
        LearningFacade facade = new LearningFacade(user, normalized.get(0));
        for (int i = 0; i < normalized.size(); i++) {
            facade.concepts.add(LearningFacadeConcept.of(facade, normalized.get(i), i + 1));
        }
        // Story-LT-E2-S3: 신규 Facade에 default Uncategorized Layer 1건 자동 생성.
        facade.layers.add(LearningLayer.of(facade, LearningLayer.DEFAULT_LAYER_NAME, 1));
        return facade;
    }

    // ─── 행위 ─────────────────────────────────────────────

    public ConceptChangeRecord updateConcept(String newConcept) {
        validateConcept(newConcept);

        String trimmed = newConcept.trim();
        if (this.concept.equals(trimmed)) {
            return ConceptChangeRecord.unchanged(this.concept);
        }

        String previous = this.concept;
        this.concept = trimmed;
        return ConceptChangeRecord.changed(previous, this.concept);
    }

    // ─── concepts[] 컬렉션 API (Story-LT-E1-S2) ───────────

    /**
     * 컨셉 1건을 컬렉션 끝에 추가한다.
     *
     * <p>규칙:
     * <ul>
     *   <li>value는 trim + blank 거부 + 길이 검증 ({@link LearningFacadeConcept#normalizeValue})</li>
     *   <li>trim 후 값 기준 중복 거부 → {@link ErrorCode#LEARNING_FACADE_CONCEPT_DUPLICATE}</li>
     *   <li>추가 후 {@code concepts.size() > MAX_CONCEPT_COUNT}이면 거부 → {@link ErrorCode#LEARNING_FACADE_CONCEPTS_SIZE_INVALID}</li>
     *   <li>displayOrder는 현재 concepts 크기 + 1 (1-based)</li>
     * </ul>
     */
    public LearningFacadeConcept addConcept(String value) {
        String normalized = LearningFacadeConcept.normalizeValue(value);
        validateConceptDuplicate(normalized);
        if (concepts.size() + 1 > MAX_CONCEPT_COUNT) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID,
                    "현재 " + concepts.size() + "개 상태에서 추가 시 최대(" + MAX_CONCEPT_COUNT + ")를 초과합니다."
            );
        }
        LearningFacadeConcept concept = LearningFacadeConcept.of(this, normalized, concepts.size() + 1);
        concepts.add(concept);
        return concept;
    }

    /**
     * 컨셉 1건 제거. 최소 개수({@link #MIN_CONCEPT_COUNT}) 미만이 되는 제거는 거부한다.
     * displayOrder는 제거 후 1-based로 재부여된다.
     */
    public void removeConcept(Long conceptId) {
        if (conceptId == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "conceptId는 null일 수 없습니다."
            );
        }
        if (concepts.size() - 1 < MIN_CONCEPT_COUNT) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID,
                    "최소 " + MIN_CONCEPT_COUNT + "개는 유지해야 합니다."
            );
        }
        LearningFacadeConcept target = concepts.stream()
                .filter(c -> conceptId.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.INVALID_INPUT,
                        "conceptId=" + conceptId + " 는 현재 concepts에 존재하지 않습니다."
                ));
        concepts.remove(target);
        // 남은 항목의 displayOrder를 1..N으로 재부여.
        for (int i = 0; i < concepts.size(); i++) {
            concepts.get(i).updateDisplayOrder(i + 1);
        }
    }

    /**
     * 컨셉 순서 재배치. 전달된 id 목록이 현재 concepts id 집합과 정확히 일치해야 한다.
     * 불일치 시 {@link ErrorCode#LEARNING_FACADE_CONCEPTS_REORDER_MISMATCH}.
     * 빈 리스트는 concepts가 비어있을 때만 정상(no-op).
     */
    public void reorderConcepts(List<Long> orderedConceptIds) {
        if (orderedConceptIds == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "orderedConceptIds는 null일 수 없습니다."
            );
        }
        Set<Long> currentIds  = concepts.stream()
                                        .map(LearningFacadeConcept::getId)
                                        .collect(Collectors.toSet());
        Set<Long> incomingIds = new HashSet<>(orderedConceptIds);
        if (orderedConceptIds.size() != currentIds.size() || !currentIds.equals(incomingIds)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_FACADE_CONCEPTS_REORDER_MISMATCH,
                    "전달 id 수=" + orderedConceptIds.size() + " · 현재 concepts 수=" + currentIds.size()
            );
        }
        IntStream.range(0, orderedConceptIds.size()).forEach(i -> {
            Long id = orderedConceptIds.get(i);
            LearningFacadeConcept target = concepts.stream()
                    .filter(c -> id.equals(c.getId()))
                    .findFirst()
                    .orElseThrow(); // 위 validation에서 방지됨
            target.updateDisplayOrder(i + 1);
        });
        // 내부 컬렉션도 displayOrder 순으로 정렬해 in-memory 조회 시 순서 일관성 보장.
        // (@OrderBy는 DB에서 load 시점만 적용되므로 in-memory 변경 후엔 수동 정렬 필요)
        concepts.sort(java.util.Comparator.comparingInt(LearningFacadeConcept::getDisplayOrder));
    }

    /**
     * concepts를 전달된 리스트로 통째 교체한다 (다건 부분성공 불허).
     *
     * <p>규칙:
     * <ul>
     *   <li>null 리스트 → {@code INVALID_INPUT}</li>
     *   <li>size가 {@link #MIN_CONCEPT_COUNT}~{@link #MAX_CONCEPT_COUNT} 범위 밖 → {@code LEARNING_FACADE_CONCEPTS_SIZE_INVALID}</li>
     *   <li>각 값은 trim + blank 거부 + 길이 검증</li>
     *   <li>정규화 후 리스트 내 중복 → {@code LEARNING_FACADE_CONCEPT_DUPLICATE}</li>
     *   <li>기존 concepts 전체를 제거 후 신규 리스트 순서대로 1-based displayOrder 부여</li>
     *   <li>legacy {@code concept} 컬럼은 첫 항목과 동기화</li>
     * </ul>
     *
     * @return {@link ConceptsChangeRecord} — previous / current / added / removed / kept
     */
    public ConceptsChangeRecord updateConcepts(List<String> newValues) {
        if (newValues == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "concepts는 null일 수 없습니다."
            );
        }
        if (newValues.size() < MIN_CONCEPT_COUNT || newValues.size() > MAX_CONCEPT_COUNT) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_FACADE_CONCEPTS_SIZE_INVALID,
                    "size=" + newValues.size()
            );
        }
        List<String> normalized = new ArrayList<>();
        for (String v : newValues) {
            String n = LearningFacadeConcept.normalizeValue(v);
            if (normalized.contains(n)) {
                throw LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_FACADE_CONCEPT_DUPLICATE,
                        "value=" + n
                );
            }
            normalized.add(n);
        }

        List<String> previous = getConceptValues();
        List<String> added    = normalized.stream()
                                          .filter(n -> !previous.contains(n))
                                          .toList();
        List<String> removed  = previous.stream()
                                        .filter(p -> !normalized.contains(p))
                                        .toList();
        List<String> kept     = normalized.stream()
                                          .filter(previous::contains)
                                          .toList();

        // 통째 교체 — orphanRemoval=true 로 detach된 자식은 flush 시 삭제된다.
        concepts.clear();
        for (int i = 0; i < normalized.size(); i++) {
            concepts.add(LearningFacadeConcept.of(this, normalized.get(i), i + 1));
        }
        // legacy 단수 concept 필드 동기화 (컬럼 DROP 전까지 유지)
        this.concept = normalized.get(0);

        return ConceptsChangeRecord.of(previous, normalized, added, removed, kept);
    }

    /**
     * 현재 concepts를 displayOrder ASC 순으로 반환. 반환된 리스트는 unmodifiable.
     */
    public List<LearningFacadeConcept> getConcepts() {
        return Collections.unmodifiableList(concepts);
    }

    /**
     * concepts 컬렉션의 값 문자열만 뽑아 순서대로 반환 (API 응답 편의).
     */
    public List<String> getConceptValues() {
        return concepts.stream()
                       .map(LearningFacadeConcept::getValue)
                       .toList();
    }

    // ─── LearningAxis API ─────────────────────────────────

    public LearningAxis addAxis(String name) {
        validateAxisNameDuplicate(name);

        // Story-LT-E2-S3: axis는 default Uncategorized Layer 소속으로 자동 라우팅.
        // Layer 도메인 승격 이전 코드 경로(레거시 addAxis)는 여전히 사용되므로,
        // 사용자가 명시적 layer를 지정하지 않으면 default로 흡수한다.
        LearningLayer target = getDefaultLayer();
        LearningAxis axis = target.addAxis(name);
        // facade-level bidirectional 동기화 — @OneToMany(mappedBy) 매핑 상 조회 편의.
        axes.add(axis);
        return axis;
    }

    /**
     * 지정된 Layer 하위에 axis 추가 (Story-LT-E2-S5 blessed 경로).
     * facade-level 이름 중복 검사 후 layer에 위임.
     */
    public LearningAxis addAxisInLayer(Long layerId, String name) {
        validateAxisNameDuplicate(name);
        LearningLayer target = findLayer(layerId);
        LearningAxis axis = target.addAxis(name);
        axes.add(axis);
        return axis;
    }

    /**
     * default Uncategorized Layer를 반환. 없으면 도메인 예외 (백필 누락 시그널).
     */
    public LearningLayer getDefaultLayer() {
        return layers.stream()
                .filter(l -> !l.isDeleted())
                .filter(LearningLayer::isDefault)
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_LAYER_NOT_FOUND,
                        "default Uncategorized layer가 없습니다. facadeId=" + this.id
                ));
    }

    LearningLayer findLayer(Long layerId) {
        return layers.stream()
                .filter(l -> !l.isDeleted())
                .filter(l -> l.getId() != null && l.getId().equals(layerId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_LAYER_NOT_FOUND,
                        "layerId=" + layerId
                ));
    }

    /** 활성 Layer 목록 반환 (soft-deleted 제외, displayOrder 정렬은 @OrderBy로 로드 시점 처리). */
    public List<LearningLayer> getLayers() {
        return layers.stream()
                .filter(l -> !l.isDeleted())
                .toList();
    }

    /**
     * 축 소프트 삭제.
     * (Fix — Axis↔Deck 완전 통합, 2026-07-01)
     *
     * <p>기존에는 {@code axes.remove(target)} + {@code orphanRemoval=true}로 hard delete했으나,
     * 이 경로가 "카드 만들 때 축이 인식 안 되고 화면 나가면 사라진다" 이슈의 최유력 원인이었다.
     * 이제 {@code target.softDelete()}만 호출하고 컬렉션에서는 제거하지 않는다 —
     * {@code orphanRemoval}은 유지되지만 명시 remove가 없으므로 발동하지 않는다.
     *
     * <p>축에 속한 Deck 연쇄 소프트 삭제는 Application Service가 조율한다
     * ({@code LearningFacadeCommandService.removeAxis}가 {@code DeckCommandService.softDeleteByAxisId} 호출).
     */
    public void removeAxis(Long axisId) {
        LearningAxis target = findAxis(axisId);
        target.softDelete();
    }

    public void reorderAxes(List<Long> orderedAxisIds) {
        validateReorderIds(orderedAxisIds);

        // [BUG FIX] displayOrder는 1-based다.
        // 이전: updateDisplayOrder(i) → 첫 번째 축에 0이 할당됐다.
        // 수정: updateDisplayOrder(i + 1) → 전달 순서대로 1, 2, 3... 을 할당한다.
        IntStream.range(0, orderedAxisIds.size()).forEach(i -> {
            LearningAxis axis = findAxis(orderedAxisIds.get(i));
            axis.updateDisplayOrder(i + 1);
        });
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    // ─── 보조 조회 ────────────────────────────────────────

    public boolean isAxisCountExceedsRecommended() {
        // 활성 축만 권장 한도 카운트에 포함. 소프트 삭제된 축은 제외.
        return getAxes().size() > RECOMMENDED_AXIS_LIMIT;
    }

    /**
     * 소속 모든 주제의 커버리지 분포를 인메모리로 집계한다.
     * Aggregate Root가 단일 진입점으로 제공한다 — 외부에서 axes→topics를 직접 순회해 합산하지 않는다.
     * 빈 Facade(축 0개 또는 모든 축의 주제 0개)는 모든 카운트가 0인 CoverageSummary를 반환한다.
     */
    public CoverageSummary getCoverageSummary() {
        int uncovered = 0;
        int partial   = 0;
        int covered   = 0;
        for (LearningAxis axis : getAxes()) {
            for (AxisTopic topic : axis.getTopics()) {
                switch (topic.getCoverageStatus()) {
                    case NO_MATERIAL -> uncovered++;
                    case PARTIAL     -> partial++;
                    case COVERED     -> covered++;
                }
            }
        }
        return CoverageSummary.of(uncovered, partial, covered);
    }

    /**
     * 소속 주제 중 coverageStatus == NO_MATERIAL 인 것이 하나라도 있으면 true.
     * getCoverageSummary().hasGap()과 동치지만 존재 여부 판단용 단축 경로.
     */
    public boolean hasUncoveredTopics() {
        return getAxes().stream().anyMatch(LearningAxis::hasUncoveredTopics);
    }

    /**
     * 활성 축만 반환. 소프트 삭제된 축은 제외한다.
     * (Fix — Axis↔Deck 완전 통합: 소프트 삭제된 축은 in-memory 컬렉션에 남아있으므로 접근자에서 필터링)
     */
    public List<LearningAxis> getAxes() {
        return axes.stream()
                   .filter(a -> !a.isDeleted())
                   .toList();
    }

    // ─── 내부 유틸 ────────────────────────────────────────

    /**
     * 활성 축 단건 조회. 이미 소프트 삭제된 축은 조회 대상에서 제외된다 — 재삭제 시도는
     * {@link ErrorCode#LEARNING_AXIS_NOT_FOUND}가 우선. Domain 검증 순서: findAxis → softDelete.
     */
    LearningAxis findAxis(Long axisId) {
        return axes.stream()
                   .filter(a -> !a.isDeleted())
                   .filter(a -> a.getId().equals(axisId))
                   .findFirst()
                   .orElseThrow(() -> LearningFacadeDomainException.of(
                           ErrorCode.LEARNING_AXIS_NOT_FOUND,
                           "axisId=" + axisId
                                                                      ));
    }

    // ─── 내부 검증 ────────────────────────────────────────

    private void validateConceptDuplicate(String normalizedValue) {
        // trim된 값 기준 대소문자 정확 매칭으로 중복 판정.
        boolean duplicated = concepts.stream()
                                     .anyMatch(c -> c.getValue().equals(normalizedValue));
        if (duplicated) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_FACADE_CONCEPT_DUPLICATE,
                    "value=" + normalizedValue
            );
        }
    }

    private void validateAxisNameDuplicate(String name) {
        if (name == null) return; // null 방어는 LearningAxis.create() 내부 validateName()이 처리

        String trimmed = name.trim();
        // 활성 축만 중복 대상. 소프트 삭제된 축의 이름은 재사용 가능(단, DB UNIQUE 제약이 별도로 걸릴 수 있음 — Story 2에서 확인).
        boolean duplicated = axes.stream()
                                 .filter(a -> !a.isDeleted())
                                 .anyMatch(a -> a.getName().equals(trimmed));
        if (duplicated) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_DUPLICATE_NAME,
                    "name=" + trimmed
                                                  );
        }
    }

    private void validateReorderIds(List<Long> orderedAxisIds) {
        // 활성 축 기준 id 집합. 소프트 삭제된 축은 순서 변경 대상 아님.
        Set<Long> currentIds = axes.stream()
                                   .filter(a -> !a.isDeleted())
                                   .map(LearningAxis::getId)
                                   .collect(Collectors.toSet());

        if (orderedAxisIds.size() != currentIds.size()) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_REORDER_MISMATCH,
                    "전달된 축 id 수(" + orderedAxisIds.size() + ")가 현재 axes 수(" + currentIds.size() + ")와 다릅니다."
                                                  );
        }

        Set<Long> incomingIds = new HashSet<>(orderedAxisIds);
        if (!currentIds.equals(incomingIds)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_REORDER_MISMATCH,
                    "전달된 축 id 목록이 현재 axes id 집합과 일치하지 않습니다."
                                                  );
        }
    }

    private static void validateConcept(String concept) {
        if (concept == null || concept.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_CONCEPT_BLANK);
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은(는) null일 수 없습니다."
                                                  );
        }
    }
}