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
@Table(name = "action_revision")
public class ActionRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "action_revision_id")
    private Long id;

    // ─── 소속 행동 ────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "axis_action_id", nullable = false, updatable = false)
    private AxisAction action;

    // ─── 수정 전후 설명 ───────────────────────────────────
    @Column(name = "previous_description", nullable = false, length = 200, updatable = false)
    private String previousDescription;

    @Column(name = "new_description", nullable = false, length = 200, updatable = false)
    private String newDescription;

    // ─── 수정 이유 스냅샷 ─────────────────────────────────
    @Column(name = "revision_reason_label", length = 100, updatable = false)
    private String revisionReasonLabel;

    // ─── 시각 ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "revised_at", nullable = false, updatable = false)
    private LocalDateTime revisedAt;

    private ActionRevision(AxisAction action,
                           String previousDescription,
                           String newDescription,
                           String revisionReasonLabel) {
        this.action              = action;
        this.previousDescription = previousDescription;
        this.newDescription      = newDescription;
        this.revisionReasonLabel = revisionReasonLabel;
    }

    // ─── 생성 ─────────────────────────────────────────────

    public static ActionRevision create(AxisAction action,
                                        String previousDescription,
                                        String newDescription,
                                        String revisionReasonLabel) {
        requireNonNull(action, "action");
        if (previousDescription == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT, "previousDescription은(는) null일 수 없습니다.");
        }
        if (newDescription == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT, "newDescription은(는) null일 수 없습니다.");
        }
        return new ActionRevision(action, previousDescription, newDescription, revisionReasonLabel);
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