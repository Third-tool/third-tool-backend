package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "axis_topic")
public class AxisTopic {

    /**
     * 주제가 "단련 중"으로 간주되는 이름 수정 횟수 임계값.
     * 이 값 이상이면 {@link #isRefinementSuggested()}가 true를 반환한다.
     * 운영 중 동적 조정이 필요해지면 Admin BC 시스템 설정으로 이관한다.
     */
    public static final int REFINEMENT_THRESHOLD = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "axis_topic_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_axis_id", nullable = false, updatable = false)
    private LearningAxis axis;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "coverage_status", nullable = false, length = 20)
    private CoverageStatus coverageStatus;

    /**
     * 이름 수정 누적 횟수. 생성 시 0. {@link #updateName(String)}이 실제 변경을 일으킬 때마다 +1.
     * 동일 값(trim 후 같은 값) 입력 또는 description 단독 수정 시 증가하지 않는다.
     */
    @Column(name = "revision_count", nullable = false)
    private int revisionCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private AxisTopic(LearningAxis axis, String name, String description, int displayOrder) {
        this.axis           = axis;
        this.name           = name;
        this.description    = description;
        this.displayOrder   = displayOrder;
        this.coverageStatus = CoverageStatus.NO_MATERIAL;
        this.revisionCount  = 0;
    }

    static AxisTopic create(LearningAxis axis, String name, String description, int displayOrder) {
        requireNonNull(axis, "axis");
        validateName(name);
        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder
            );
        }
        return new AxisTopic(axis, name.trim(), normalizeDescription(description), displayOrder);
    }

    /**
     * 이름을 갱신한다. 동일 값(trim 후)이 들어오면 상태를 바꾸지 않고 {@code false}를 반환한다.
     * 실제 변경이 발생한 경우에만 {@code revisionCount}를 1 증가시킨다.
     */
    public boolean updateName(String newName) {
        validateName(newName);
        String trimmed = newName.trim();
        if (this.name.equals(trimmed)) {
            return false;
        }
        this.name = trimmed;
        this.revisionCount++;
        return true;
    }

    /**
     * 이름 수정 누적 횟수가 임계값(REFINEMENT_THRESHOLD) 이상인지 여부.
     * Application Service가 응답에 안내 플래그로 포함한다 (UX는 강제가 아닌 안내).
     */
    public boolean isRefinementSuggested() {
        return this.revisionCount >= REFINEMENT_THRESHOLD;
    }

    /**
     * 부연 설명을 갱신한다. 동일 값이 들어오면 상태를 바꾸지 않고 {@code false}를 반환한다.
     * 빈 문자열은 null로 정규화되며, null 입력은 설명 제거 의미다.
     */
    public boolean updateDescription(String newDescription) {
        String normalized = normalizeDescription(newDescription);
        if (java.util.Objects.equals(this.description, normalized)) {
            return false;
        }
        this.description = normalized;
        return true;
    }

    void updateDisplayOrder(int newOrder) {
        if (newOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. newOrder=" + newOrder
            );
        }
        this.displayOrder = newOrder;
    }

    public void updateCoverageStatus(CoverageStatus status) {
        requireNonNull(status, "coverageStatus");
        this.coverageStatus = status;
    }

    /**
     * displayOrder가 {@code focusThreshold} 이하면 "지금 집중 중"으로 본다.
     * Application Service가 상위 N개 주제에 isFocused 플래그를 표시할 때 사용한다.
     */
    public boolean isFocused(int focusThreshold) {
        return this.displayOrder <= focusThreshold;
    }

    private static String normalizeDescription(String description) {
        if (description == null) return null;
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_TOPIC_NAME_BLANK);
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
