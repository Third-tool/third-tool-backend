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

/**
 * Selection 챕터 노드 (Story-LT-E3-S3-9, 이슈 #16).
 *
 * <p>{@link AxisSelection} 컨테이너 아래의 자식 Entity. 컨테이너 정책은 이슈 #11 계승
 * (name UNIQUE, created_at DESC, hard delete). 노드 자체는 soft delete 없음 —
 * 컨테이너 hard delete 시 자식 노드 CASCADE.
 *
 * <p>body에 챕터 subtree (`├── 1-1. 정의와 본질\n...` 형태 ASCII 통짜) 저장.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "axis_selection_node")
public class AxisSelectionNode {

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int RATIONALE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selection_id", nullable = false, updatable = false)
    private AxisSelection selection;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "rationale", length = RATIONALE_MAX_LENGTH)
    private String rationale;

    @Column(name = "body", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private AxisSelectionNode(AxisSelection selection, int displayOrder, String title, String rationale, String body) {
        this.selection = selection;
        this.displayOrder = displayOrder;
        this.title = title;
        this.rationale = rationale;
        this.body = body;
    }

    /**
     * 정적 팩토리. {@link AxisSelection#addNode}에서만 호출된다.
     */
    static AxisSelectionNode create(AxisSelection selection, int displayOrder, String title, String rationale, String body) {
        if (selection == null) {
            throw LearningFacadeDomainException.of(ErrorCode.INVALID_INPUT, "selection은 null일 수 없습니다.");
        }
        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder);
        }
        String normalizedTitle = validateAndTrimTitle(title);
        String normalizedBody = validateAndTrimBody(body);
        String normalizedRationale = normalizeRationale(rationale);
        return new AxisSelectionNode(selection, displayOrder, normalizedTitle, normalizedRationale, normalizedBody);
    }

    public void updateTitle(String newTitle) {
        this.title = validateAndTrimTitle(newTitle);
    }

    public void updateRationale(String newRationale) {
        this.rationale = normalizeRationale(newRationale);
    }

    public void updateBody(String newBody) {
        this.body = validateAndTrimBody(newBody);
    }

    void updateDisplayOrder(int newOrder) {
        if (newOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. newOrder=" + newOrder);
        }
        this.displayOrder = newOrder;
    }

    private static String validateAndTrimTitle(String title) {
        if (title == null || title.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.SELECTION_NODE_TITLE_BLANK);
        }
        String trimmed = title.trim();
        if (trimmed.length() > TITLE_MAX_LENGTH) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "title은 " + TITLE_MAX_LENGTH + "자 이내여야 합니다. length=" + trimmed.length());
        }
        return trimmed;
    }

    private static String validateAndTrimBody(String body) {
        if (body == null || body.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.SELECTION_NODE_BODY_BLANK);
        }
        return body.trim();
    }

    private static String normalizeRationale(String rationale) {
        if (rationale == null || rationale.isBlank()) {
            return null;
        }
        String trimmed = rationale.trim();
        if (trimmed.length() > RATIONALE_MAX_LENGTH) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "rationale은 " + RATIONALE_MAX_LENGTH + "자 이내여야 합니다. length=" + trimmed.length());
        }
        return trimmed;
    }
}
