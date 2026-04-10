package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "axis_action")
public class AxisAction {

    private static final int REFINEMENT_THRESHOLD = 3;

    // ─── 식별자 ───────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "axis_action_id")
    private Long id;

    // ─── 소속 축 ──────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_axis_id", nullable = false, updatable = false)
    private LearningAxis axis;

    // ─── 행동 설명 ────────────────────────────────────────
    @Column(name = "description", nullable = false, length = 200)
    private String description;

    // 학습 자료 매핑 이벤트 또는 동사 수정에 의해서만 변경된다.
    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_status", nullable = false, length = 20)
    private CoverageStatus coverageStatus;

    @Column(name = "revision_count", nullable = false)
    private int revisionCount;

    @OneToMany(
            mappedBy      = "action",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    private final List<ActionRevision> revisions = new ArrayList<>();

    // ─── 시각 ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** create() 내부 전용 생성자. */
    private AxisAction(LearningAxis axis, String description) {
        this.axis           = axis;
        this.description    = description;
        this.coverageStatus = CoverageStatus.NO_MATERIAL;
        this.revisionCount  = 0;
    }

    // ─── 생성 ─────────────────────────────────────────────

    static AxisAction create(LearningAxis axis, String description) {
        requireNonNull(axis, "axis");
        validateDescription(description);
        return new AxisAction(axis, description.trim());
    }

    // ─── 행위 ─────────────────────────────────────────────

    public ActionChangeRecord updateDescription(String newDescription) {
        validateDescription(newDescription);
        String trimmed = newDescription.trim();

        if (this.description.equals(trimmed)) {
            return ActionChangeRecord.unchanged(this.description); // 동일 값 — 변경 없음
        }

        String previous     = this.description;
        this.description    = trimmed;
        this.coverageStatus = CoverageStatus.NO_MATERIAL; // 커버리지 초기화
        this.revisionCount++;                              // 수정 횟수 누적
        return ActionChangeRecord.changed(previous, this.description);
    }

    void updateCoverageStatus(CoverageStatus status) {
        requireNonNull(status, "coverageStatus");
        this.coverageStatus = status;
    }

    // ─── 보조 조회 ────────────────────────────────────────

    public boolean isRefinementSuggested() {
        return revisionCount >= REFINEMENT_THRESHOLD;
    }

    public List<ActionRevision> getRevisions() {
        return Collections.unmodifiableList(revisions);
    }

    // ─── 내부 검증 ────────────────────────────────────────

    private static void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_ACTION_DESCRIPTION_BLANK);
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