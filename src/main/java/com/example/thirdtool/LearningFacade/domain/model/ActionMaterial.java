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
        name = "action_material",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_action_material",
                columnNames = {"axis_action_id", "learning_material_id"}
        )
)
public class ActionMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_material_id")
    private Long id;

    // ─── 연결 대상 ────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "axis_action_id", nullable = false, updatable = false)
    private AxisAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_material_id", nullable = false, updatable = false)
    private LearningMaterial material;

    // ─── 시각 ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    private ActionMaterial(AxisAction action, LearningMaterial material) {
        this.action   = action;
        this.material = material;
        this.linkedAt = LocalDateTime.now();
    }

    // ─── 생성 ─────────────────────────────────────────────

    public static ActionMaterial create(AxisAction action, LearningMaterial material) {
        requireNonNull(action, "action");
        requireNonNull(material, "material");
        return new ActionMaterial(action, material);
    }

    // ─── 내부 검증 ────────────────────────────────────────

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은(는) null일 수 없습니다."
                                                  );
        }
    }
}