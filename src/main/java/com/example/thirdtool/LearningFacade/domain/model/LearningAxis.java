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
public class LearningAxis {

    private static final int RECOMMENDED_TOPIC_LIMIT = 10;

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
