package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 주제 이름 수정 이력 엔티티.
 *
 * <p>{@link AxisTopic#updateName(String)}이 실제 변경을 일으킨 경우에만 Application Service가 생성한다.
 * 동일 값(trim 후) 재입력으로 변경이 없으면 이력이 생성되지 않는다.
 *
 * <p>{@code revisionReasonLabel}은 {@link RevisionReasonOption}을 FK로 참조하지 않고
 * 선택 시점의 라벨을 스냅샷으로 보관한다 — 관리자가 운영 중 라벨을 변경·비활성화해도
 * 과거 이력은 당시 표현을 유지한다 (회고 가치 보존).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "topic_revision")
public class TopicRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_revision_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "axis_topic_id", nullable = false, updatable = false)
    private AxisTopic topic;

    @Column(name = "previous_name", nullable = false, length = 100)
    private String previousName;

    @Column(name = "new_name", nullable = false, length = 100)
    private String newName;

    @Column(name = "revision_reason_label", length = 100)
    private String revisionReasonLabel;

    @CreationTimestamp
    @Column(name = "revised_at", nullable = false, updatable = false)
    private LocalDateTime revisedAt;

    private TopicRevision(AxisTopic topic, String previousName, String newName, String revisionReasonLabel) {
        this.topic               = topic;
        this.previousName        = previousName;
        this.newName             = newName;
        this.revisionReasonLabel = revisionReasonLabel;
    }

    /**
     * 주제 이름 수정 이력을 생성한다. 이유 라벨은 선택(null 허용).
     *
     * @param topic               이력이 속한 주제
     * @param previousName        수정 이전 이름 (이미 trim된 상태)
     * @param newName             수정 이후 이름 (이미 trim된 상태)
     * @param revisionReasonLabel 선택 시점의 라벨 스냅샷. nullable
     */
    public static TopicRevision of(AxisTopic topic, String previousName, String newName, String revisionReasonLabel) {
        requireNonNull(topic, "topic");
        validateName(previousName, "previousName");
        validateName(newName, "newName");
        return new TopicRevision(topic, previousName, newName, revisionReasonLabel);
    }

    private static void validateName(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은(는) null 또는 blank일 수 없습니다."
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
