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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    @OneToMany(
            mappedBy      = "facade",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
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

        int nextOrder = axes.size(); // 0-based: 현재 크기가 곧 다음 인덱스
        LearningAxis axis = LearningAxis.create(this, name, nextOrder);
        axes.add(axis);
        return axis;
    }

    public void removeAxis(Long axisId) {
        LearningAxis target = findAxis(axisId);
        axes.remove(target);
    }

    public void reorderAxes(List<Long> orderedAxisIds) {
        validateReorderIds(orderedAxisIds);

        IntStream.range(0, orderedAxisIds.size()).forEach(i -> {
            LearningAxis axis = findAxis(orderedAxisIds.get(i));
            axis.updateDisplayOrder(i);
        });
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    // ─── 보조 조회 ────────────────────────────────────────

    public boolean isAxisCountExceedsRecommended() {
        return axes.size() > RECOMMENDED_AXIS_LIMIT;
    }

    public List<LearningAxis> getAxes() {
        return Collections.unmodifiableList(axes);
    }

    // ─── 내부 유틸 ────────────────────────────────────────

    LearningAxis findAxis(Long axisId) {
        return axes.stream()
                   .filter(a -> a.getId().equals(axisId))
                   .findFirst()
                   .orElseThrow(() -> LearningFacadeDomainException.of(
                           ErrorCode.LEARNING_AXIS_NOT_FOUND,
                           "axisId=" + axisId
                                                                      ));
    }

    // ─── 내부 검증 ────────────────────────────────────────

    private void validateAxisNameDuplicate(String name) {
        if (name == null) return; // validateName이 이후에 처리

        String trimmed = name.trim();
        boolean duplicated = axes.stream()
                                 .anyMatch(a -> a.getName().equals(trimmed));
        if (duplicated) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_DUPLICATE_NAME,
                    "name=" + trimmed
                                                  );
        }
    }

    private void validateReorderIds(List<Long> orderedAxisIds) {
        Set<Long> currentIds = axes.stream()
                                   .map(LearningAxis::getId)
                                   .collect(Collectors.toSet());
        Set<Long> incomingIds = Set.copyOf(orderedAxisIds);

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