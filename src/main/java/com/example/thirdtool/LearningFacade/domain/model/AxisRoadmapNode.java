package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Roadmap 챕터 노드 (Story-LT-E3-S3-6, 이슈 #15).
 *
 * <p>이슈 #6의 {@code axis_roadmap.content TEXT} 통짜 저장 방식은 SUPERSEDED.
 * 챕터 단위 first-class Entity로 승격되어 각 챕터가 독립적으로 저장·조회·순서변경·재생성 가능.
 *
 * <p>body에는 챕터 subtree (`├── 1-1. 정의와 본질\n...` 형태 ASCII 통짜)를 저장한다.
 * 편집·diff는 사용자 텍스트 편집으로 자연스럽게 성립.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "axis_roadmap_node")
@SQLRestriction("deleted_at IS NULL")
@SQLDelete(sql = "UPDATE axis_roadmap_node SET deleted_at = NOW(6) WHERE id = ?")
public class AxisRoadmapNode {

    public static final int TITLE_MAX_LENGTH = 200;
    public static final int RATIONALE_MAX_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "axis_id", nullable = false, updatable = false)
    private LearningAxis axis;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "title", nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(name = "rationale", length = RATIONALE_MAX_LENGTH)
    private String rationale;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private AxisRoadmapNode(LearningAxis axis, int displayOrder, String title, String rationale, String body) {
        this.axis = axis;
        this.displayOrder = displayOrder;
        this.title = title;
        this.rationale = rationale;
        this.body = body;
    }

    /**
     * 정적 팩토리. {@link LearningAxis#addRoadmapNode}에서만 호출된다.
     *
     * <p>trim + blank 검증은 이 팩토리에서 담당한다 — Application Service가 정규화하지 않는다.
     */
    static AxisRoadmapNode create(LearningAxis axis, int displayOrder, String title, String rationale, String body) {
        if (axis == null) {
            throw LearningFacadeDomainException.of(ErrorCode.INVALID_INPUT, "axis는 null일 수 없습니다.");
        }
        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder);
        }
        String normalizedTitle = validateAndTrimTitle(title);
        String normalizedBody = validateAndTrimBody(body);
        String normalizedRationale = normalizeRationale(rationale);

        return new AxisRoadmapNode(axis, displayOrder, normalizedTitle, normalizedRationale, normalizedBody);
    }

    public void updateTitle(String newTitle) {
        this.title = validateAndTrimTitle(newTitle);
    }

    /**
     * rationale는 nullable. blank/null 입력 시 null로 정규화한다 (선택 필드 컨벤션).
     */
    public void updateRationale(String newRationale) {
        this.rationale = normalizeRationale(newRationale);
    }

    public void updateBody(String newBody) {
        this.body = validateAndTrimBody(newBody);
    }

    /**
     * Roadmap 노드 자체 논리 삭제.
     * {@code @SQLDelete}에 의해 UPDATE로 변환되어 {@code deleted_at}이 설정된다.
     */
    public void softDelete() {
        if (this.deletedAt != null) {
            return; // 멱등 (재삭제 no-op)
        }
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
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
            throw LearningFacadeDomainException.of(ErrorCode.ROADMAP_NODE_TITLE_BLANK);
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
            throw LearningFacadeDomainException.of(ErrorCode.ROADMAP_NODE_BODY_BLANK);
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
