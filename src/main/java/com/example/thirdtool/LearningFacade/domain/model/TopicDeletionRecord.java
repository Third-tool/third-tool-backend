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
 * 주제(AxisTopic) 삭제 시점 스냅샷을 보존하는 archive 엔티티.
 *
 * <p>ADR003에 따라 {@code axis_topic}은 soft delete를 적용하지 않는다 (구조 편집 단위).
 * 그러나 삭제 사실 자체는 회고 가치가 있어, 삭제 직전 스냅샷을 별도 archive 테이블에
 * 보존한다. 원본 {@code axis_topic} 행은 cascade로 사라져도 본 record는 영속된다.
 *
 * <p>{@code originalTopicId}, {@code learningAxisId}는 FK가 아닌 단순 기록(snapshot)이다.
 * 원본이 사라진 뒤에도 보존되어야 하므로 참조 무결성을 강제하지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "axis_topic_deletion")
public class TopicDeletionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "axis_topic_deletion_id")
    private Long id;

    @Column(name = "original_topic_id", nullable = false, updatable = false)
    private Long originalTopicId;

    @Column(name = "learning_axis_id", nullable = false, updatable = false)
    private Long learningAxisId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "revision_count", nullable = false)
    private int revisionCount;

    @CreationTimestamp
    @Column(name = "deleted_at", nullable = false, updatable = false)
    private LocalDateTime deletedAt;

    private TopicDeletionRecord(Long originalTopicId, Long learningAxisId,
                                String name, String description, int revisionCount) {
        this.originalTopicId = originalTopicId;
        this.learningAxisId  = learningAxisId;
        this.name            = name;
        this.description     = description;
        this.revisionCount   = revisionCount;
    }

    /**
     * 삭제 직전 {@link AxisTopic}의 스냅샷을 캡처해 archive record를 생성한다.
     * Application Service가 {@code removeTopic} 흐름의 첫 단계로 호출한다.
     */
    public static TopicDeletionRecord of(AxisTopic topic) {
        if (topic == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT, "topic은 null일 수 없습니다.");
        }
        if (topic.getId() == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT, "삭제 스냅샷은 영속화된 주제만 캡처 가능합니다 (id null).");
        }
        if (topic.getAxis() == null || topic.getAxis().getId() == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT, "삭제 스냅샷에는 소속 축 id가 필요합니다.");
        }
        return new TopicDeletionRecord(
                topic.getId(),
                topic.getAxis().getId(),
                topic.getName(),
                topic.getDescription(),
                topic.getRevisionCount()
        );
    }
}
