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

/**
 * 축({@link LearningAxis}) 아래에 위치하는 구체적인 학습 행동.
 *
 * <p>AxisAction은 인터페이스의 메서드 시그니처와 같다.
 * "무엇을 하는가"만 단일 동사로 선언하고, "어떻게 하는가"는 유저의 구현에 맡긴다.
 * 예: "설계하다", "문서화하다", "검증하다"
 *
 * <p><b>단일 동사 원칙</b>: {@code description}에 공백이 포함되면 예외를 발생시킨다.
 * "설계하다"는 허용, "설계하고 분석하다"는 불허.
 *
 * <p>외부에서 직접 생성하지 않는다. 반드시 {@link LearningAxis#addAction(String)}을 통해 생성한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "axis_action")
public class AxisAction {

    private static final int REFINEMENT_THRESHOLD = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "axis_action_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_axis_id", nullable = false, updatable = false)
    private LearningAxis axis;

    // ─── 행동 동사 ────────────────────────────────────────
    // 단일 동사 하나로만 구성한다. 공백 포함 시 도메인이 예외를 발생시킨다. - 중요 규칙
    // 동사형 어미("하다") 여부는 Application Service의 VerbFormValidator가 안내 플래그로 감지한다.
    @Column(name = "description", nullable = false, length = 50)
    private String description;

    // ─── 커버리지 상태 ────────────────────────────────────
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
        this.coverageStatus = CoverageStatus.NO_MATERIAL; // 동사 바뀌면 커버리지 초기화
        this.revisionCount++;
        return ActionChangeRecord.changed(previous, this.description);
    }

    /**
     * 커버리지 상태를 변경한다.
     *
     * <p>학습 자료 매핑 이벤트에 의해서만 호출된다. package-private.
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
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_ACTION_DESCRIPTION_BLANK
                                                  );
        }
        // 단일 동사 원칙: 공백이 포함되면 복수 동사로 간주해 차단한다. - 추후 추가 업그레이드 예정
        // trim 이전에 검사한다. 앞뒤 공백은 trim으로 허용하되, 중간 공백은 불허.
        if (description.trim().contains(" ")) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_ACTION_DESCRIPTION_MULTI_VERB
                                                  );
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