package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "topic_material",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_topic_material",
                columnNames = {"axis_topic_id", "learning_material_id"}
        )
)
public class TopicMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topic_material_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "axis_topic_id", nullable = false, updatable = false)
    private AxisTopic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_material_id", nullable = false, updatable = false)
    private LearningMaterial material;

    @CreationTimestamp
    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    private TopicMaterial(AxisTopic topic, LearningMaterial material) {
        this.topic    = topic;
        this.material = material;
    }

    public static TopicMaterial create(AxisTopic topic, LearningMaterial material) {
        requireNonNull(topic, "topic");
        requireNonNull(material, "material");
        return new TopicMaterial(topic, material);
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
