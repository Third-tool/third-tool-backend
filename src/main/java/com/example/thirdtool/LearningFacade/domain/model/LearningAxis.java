package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "learning_axis",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learning_axis_facade_name",
                columnNames = {"learning_facade_id", "name"}
        )
)
@SQLRestriction("deleted_at IS NULL")
public class LearningAxis {

    private static final int RECOMMENDED_TOPIC_LIMIT = 10;

    /**
     * Story 2-3 — 상위 N개 주제에 "지금 집중 중" 뱃지를 표시할 때 사용하는 임계값.
     * {@link AxisTopic#isFocused(int)}가 displayOrder ≤ 이 값일 때 true를 반환한다.
     * 주제 3개 미만이면 모든 주제가 자동으로 focused (displayOrder 1, 2, 3 모두 임계값 이하).
     */
    public static final int FOCUS_TOP_N = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_axis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_facade_id", nullable = false, updatable = false)
    private LearningFacade facade;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @OneToMany(
            mappedBy      = "axis",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    private final List<AxisTopic> topics = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Soft Delete 시각. null이면 활성 상태.
     * 클래스 상단 {@code @SQLRestriction("deleted_at IS NULL")}로 모든 read에서 자동 필터된다.
     * (Fix — Axis↔Deck 완전 통합, 2026-07-01)
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private LearningAxis(LearningFacade facade, String name, int displayOrder) {
        this.facade       = facade;
        this.name         = name;
        this.displayOrder = displayOrder;
    }

    static LearningAxis create(LearningFacade facade, String name, int displayOrder) {
        requireNonNull(facade, "facade");
        validateName(name);

        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder
            );
        }
        return new LearningAxis(facade, name.trim(), displayOrder);
    }

    public void updateName(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

    /**
     * 축 논리 삭제.
     * {@code deleted_at}을 현재 시각으로 설정한다. 클래스 상단 {@code @SQLRestriction}으로
     * 이후 모든 read 쿼리에서 자동 제외된다.
     *
     * <p>이미 삭제된 축을 재삭제하면 {@link ErrorCode#LEARNING_AXIS_ALREADY_DELETED} 예외.
     * (Deck.softDelete()의 DECK_ALREADY_DELETED와 동일 패턴)
     *
     * <p>본 메서드는 자기 자신의 상태만 변경한다. 축에 속한 Deck 연쇄 소프트 삭제는
     * Application Service(LearningFacadeCommandService.removeAxis)가 조율한다.
     */
    public void softDelete() {
        if (this.deletedAt != null) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_ALREADY_DELETED);
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
                    "displayOrder는 1 이상이어야 합니다. newOrder=" + newOrder
            );
        }
        this.displayOrder = newOrder;
    }

    public AxisTopic addTopic(String name, String description) {
        int nextOrder = topics.size() + 1;
        AxisTopic topic = AxisTopic.create(this, name, description, nextOrder);
        topics.add(topic);
        return topic;
    }

    /**
     * 다건 주제 추가 (AI 제안 체크박스 등). 입력 순서대로 displayOrder가 1-based로 부여된다.
     */
    public List<AxisTopic> addTopics(List<TopicCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        List<AxisTopic> added = new ArrayList<>(commands.size());
        for (TopicCommand cmd : commands) {
            added.add(addTopic(cmd.name(), cmd.description()));
        }
        return added;
    }

    public void removeTopic(Long topicId) {
        AxisTopic target = findTopic(topicId);
        topics.remove(target);
    }

    /**
     * 주제 순서 변경. 전달된 id 순서대로 displayOrder를 1-based로 재부여한다.
     * 전달된 id 집합이 현재 topics와 일치해야 한다.
     */
    public void reorderTopics(List<Long> orderedTopicIds) {
        validateReorderTopicIds(orderedTopicIds);
        IntStream.range(0, orderedTopicIds.size()).forEach(i -> {
            AxisTopic topic = findTopic(orderedTopicIds.get(i));
            topic.updateDisplayOrder(i + 1);
        });
    }

    public boolean isTopicCountExceedsRecommended() {
        return topics.size() > RECOMMENDED_TOPIC_LIMIT;
    }

    /**
     * 이 축에 속한 주제 중 자료 미연결 주제가 하나라도 있는지.
     * 축 카드 ⚠️ 경고 뱃지 표시 판단에 사용한다.
     */
    public boolean hasUncoveredTopics() {
        return topics.stream().anyMatch(AxisTopic::isUncovered);
    }

    /**
     * 이 축에 속한 미커버 주제 개수.
     */
    public int countUncoveredTopics() {
        return (int) topics.stream().filter(AxisTopic::isUncovered).count();
    }

    /**
     * 주제가 1개 이상이고 모든 주제가 자료로 커버되어 있으면 true.
     * 빈 축(주제 0개)은 false. PARTIAL 상태 주제만 있어도 미커버가 없으면 완전 커버로 간주한다
     * (도메인 스펙 §13 디폴트 — coverage != NO_MATERIAL 기준).
     */
    public boolean isFullyCovered() {
        return !topics.isEmpty() && topics.stream().noneMatch(AxisTopic::isUncovered);
    }

    public List<AxisTopic> getTopics() {
        return Collections.unmodifiableList(topics);
    }

    public AxisTopic findTopic(Long topicId) {
        return topics.stream()
                .filter(t -> t.getId() != null && t.getId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_AXIS_TOPIC_NOT_FOUND,
                        "topicId=" + topicId
                ));
    }

    private void validateReorderTopicIds(List<Long> orderedTopicIds) {
        if (orderedTopicIds == null) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_TOPIC_REORDER_MISMATCH);
        }
        Set<Long> currentIds = topics.stream()
                .map(AxisTopic::getId)
                .collect(Collectors.toSet());

        if (orderedTopicIds.size() != currentIds.size()) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_TOPIC_REORDER_MISMATCH,
                    "전달된 주제 id 수(" + orderedTopicIds.size()
                            + ")가 현재 topics 수(" + currentIds.size() + ")와 다릅니다."
            );
        }

        Set<Long> incomingIds = new HashSet<>(orderedTopicIds);
        if (!currentIds.equals(incomingIds)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_TOPIC_REORDER_MISMATCH,
                    "전달된 주제 id 목록이 현재 topics id 집합과 일치하지 않습니다."
            );
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_NAME_BLANK);
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

    public record TopicCommand(String name, String description) {
        public static TopicCommand of(String name, String description) {
            return new TopicCommand(name, description);
        }
    }
}
