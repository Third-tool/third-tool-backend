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

    // ─── 커버리지 상태 ────────────────────────────────────
    // 학습 자료 매핑 이벤트 또는 동사 수정에 의해서만 변경된다.
    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_status", nullable = false, length = 20)
    private CoverageStatus coverageStatus;

    // ─── 수정 횟수 ────────────────────────────────────────
    // 동일 값 재입력 시 증가하지 않는다.
    @Column(name = "revision_count", nullable = false)
    private int revisionCount;

    // ─── 수정 이력 ────────────────────────────────────────
    // 행동 삭제 시 orphanRemoval로 함께 삭제된다.
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

    /**
     * 행동을 생성한다. {@link LearningAxis#addAction(String)} 내부에서만 호출한다.
     *
     * <p>커버리지는 {@link CoverageStatus#NO_MATERIAL}로, {@code revisionCount}는 0으로 초기화된다.
     */
    static AxisAction create(LearningAxis axis, String description) {
        requireNonNull(axis, "axis");
        validateDescription(description);
        return new AxisAction(axis, description.trim());
    }

    // ─── 행위 ─────────────────────────────────────────────

    /**
     * 행동 설명(동사)을 변경한다.
     *
     * <p>기존 값과 동일한 경우 상태를 변경하지 않고 {@code unchanged} 결과를 반환한다.
     * 커버리지 초기화와 {@code revisionCount} 증가도 발생하지 않는다.
     *
     * <p>실제 변경이 발생하면:
     * <ul>
     *   <li>커버리지를 {@link CoverageStatus#NO_MATERIAL}로 초기화한다.</li>
     *   <li>{@code revisionCount}를 1 증가시킨다.</li>
     * </ul>
     *
     * @return 수정 결과를 담은 {@link ActionChangeRecord}.
     *         Application Service는 이 객체로 이력 생성 여부와 저장 실행 여부를 판단한다.
     */
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

    /**
     * 커버리지 상태를 변경한다.
     *
     * <p>학습 자료 매핑 이벤트에 의해서만 호출된다.
     * Application Service가 직접 호출하지 않는다.
     */
    void updateCoverageStatus(CoverageStatus status) {
        requireNonNull(status, "coverageStatus");
        this.coverageStatus = status;
    }

    // ─── 보조 조회 ────────────────────────────────────────

    /**
     * 수정 횟수가 임계값({@value REFINEMENT_THRESHOLD}) 이상이면 {@code true}를 반환한다.
     *
     * <p>Application Service가 "이 행동이 아직 단련 중이에요" 안내 플래그로 사용한다.
     */
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