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
 * 주제 수정 이유 선택지 관리 엔티티.
 *
 * <p>운영 중 선택지를 추가·수정·비활성화할 수 있도록 DB로 관리한다. v1에서는 Flyway seed +
 * DB 직접 수정으로 운영하며, Admin BC 도입 시 관리 화면으로 이관한다.
 *
 * <p>{@code active = false}로 설정하면 신규 선택 목록에서만 제외된다 — 이미 저장된 이력의
 * {@code revisionReasonLabel} 스냅샷은 영향받지 않는다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "revision_reason_option")
public class RevisionReasonOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "revision_reason_option_id")
    private Long id;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private RevisionReasonOption(String label, int displayOrder, boolean active) {
        this.label        = label;
        this.displayOrder = displayOrder;
        this.active       = active;
    }

    public static RevisionReasonOption of(String label, int displayOrder, boolean active) {
        if (label == null || label.isBlank()) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT, "label은 null 또는 blank일 수 없습니다."
            );
        }
        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT, "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder
            );
        }
        return new RevisionReasonOption(label.trim(), displayOrder, active);
    }

    /**
     * 이유 선택을 이력 스냅샷으로 변환할 때 사용 가능한지 검증.
     * 비활성 선택지는 신규 이력 생성에 사용할 수 없다.
     */
    public boolean isUsableForNewRevision() {
        return active;
    }
}
