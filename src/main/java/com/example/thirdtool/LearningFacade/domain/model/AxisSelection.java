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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Selection 컨테이너 (Story-LT-E3-S3-9, 이슈 #16 · 이슈 #11 정책 계승).
 *
 * <p>이슈 #11 정책 (컨테이너 레벨):
 * <ul>
 *   <li>name UNIQUE per axis (활성 컨테이너만)</li>
 *   <li>created_at DESC 정렬 (조회는 최신 우선)</li>
 *   <li>hard delete (자식 노드 CASCADE)</li>
 *   <li>soft delete 없음</li>
 * </ul>
 *
 * <p>이슈 #16 신설 (자식 노드):
 * <ul>
 *   <li>{@link AxisSelectionNode} — 챕터 first-class</li>
 *   <li>컨테이너 hard delete 시 orphanRemoval로 자식 하드 삭제</li>
 * </ul>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "axis_selection",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_axis_selection_axis_name",
                columnNames = {"axis_id", "name"}
        )
)
public class AxisSelection {

    public static final int NAME_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "axis_id", nullable = false, updatable = false)
    private LearningAxis axis;

    @Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @OneToMany(
            mappedBy      = "selection",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    private final List<AxisSelectionNode> nodes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private AxisSelection(LearningAxis axis, String name) {
        this.axis = axis;
        this.name = name;
    }

    /**
     * 정적 팩토리. {@link LearningAxis#addSelection}에서만 호출된다.
     */
    static AxisSelection create(LearningAxis axis, String name) {
        if (axis == null) {
            throw LearningFacadeDomainException.of(ErrorCode.INVALID_INPUT, "axis는 null일 수 없습니다.");
        }
        return new AxisSelection(axis, validateAndTrimName(name));
    }

    /**
     * 컨테이너 이름 in-place update. 이슈 #11 정책 계승.
     */
    public void updateName(String newName) {
        this.name = validateAndTrimName(newName);
    }

    // ─── 자식 노드 도메인 API (Story-LT-E3-S3-9) ─────────

    public AxisSelectionNode addNode(String title, String rationale, String body) {
        int nextOrder = nodes.size() + 1;
        AxisSelectionNode node = AxisSelectionNode.create(this, nextOrder, title, rationale, body);
        nodes.add(node);
        return node;
    }

    public void reorderNodes(List<Long> orderedNodeIds) {
        validateNodeReorderIds(orderedNodeIds);
        IntStream.range(0, orderedNodeIds.size()).forEach(i -> {
            AxisSelectionNode node = findNode(orderedNodeIds.get(i));
            node.updateDisplayOrder(i + 1);
        });
    }

    /**
     * 노드 hard delete (컨테이너 정책 계승 — soft delete 없음).
     * {@code orphanRemoval=true}로 컬렉션에서 제거 시 자동 DELETE.
     */
    public void removeNode(Long nodeId) {
        AxisSelectionNode target = findNode(nodeId);
        nodes.remove(target);
    }

    public AxisSelectionNode findNode(Long nodeId) {
        return nodes.stream()
                .filter(n -> n.getId() != null && n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.SELECTION_NODE_NOT_FOUND,
                        "nodeId=" + nodeId));
    }

    public List<AxisSelectionNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    private void validateNodeReorderIds(List<Long> orderedNodeIds) {
        if (orderedNodeIds == null) {
            throw LearningFacadeDomainException.of(ErrorCode.SELECTION_NODE_ORDER_MISMATCH);
        }
        Set<Long> currentIds = nodes.stream()
                .map(AxisSelectionNode::getId)
                .collect(Collectors.toSet());

        if (orderedNodeIds.size() != currentIds.size()) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.SELECTION_NODE_ORDER_MISMATCH,
                    "전달된 노드 id 수(" + orderedNodeIds.size()
                            + ")가 현재 노드 수(" + currentIds.size() + ")와 다릅니다.");
        }

        Set<Long> incomingIds = new HashSet<>(orderedNodeIds);
        if (!currentIds.equals(incomingIds)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.SELECTION_NODE_ORDER_MISMATCH,
                    "전달된 노드 id 목록이 현재 노드 id 집합과 일치하지 않습니다.");
        }
    }

    private static String validateAndTrimName(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.AXIS_SELECTION_NAME_BLANK);
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "Selection 이름은 " + NAME_MAX_LENGTH + "자 이내여야 합니다. length=" + trimmed.length());
        }
        return trimmed;
    }
}
