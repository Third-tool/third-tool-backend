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

    // ─── 식별자 ───────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_facade_id")
    private Long id;

    // ─── 소유 사용자 ──────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    @Column(name = "concept", nullable = false, length = 100)
    private String concept;

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
        return new LearningFacade(user, concept.trim());
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

    public LearningAxis addAxis(String name) {
        validateAxisNameDuplicate(name);

        // Fix — Axis↔Deck 완전 통합: displayOrder는 활성 축 기준으로 부여한다.
        // 소프트 삭제된 축은 in-memory 컬렉션에 남아 있으나 사용자 관점의 "N번째 축"에서는 제외.
        int nextOrder = (int) axes.stream().filter(a -> !a.isDeleted()).count() + 1;
        LearningAxis axis = LearningAxis.create(this, name, nextOrder);
        axes.add(axis);
        return axis;
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