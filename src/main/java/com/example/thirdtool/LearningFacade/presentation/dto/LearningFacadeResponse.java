package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LearningFacadeResponse {

    // ──────────────────────────────────────────────────────
    // 공유 내부 레코드
    // ──────────────────────────────────────────────────────

    public record TopicItem(
            Long topicId,
            String name,
            String description,
            int displayOrder,
            String coverageStatus
    ) {
        public static TopicItem of(AxisTopic topic) {
            return new TopicItem(
                    topic.getId(),
                    topic.getName(),
                    topic.getDescription(),
                    topic.getDisplayOrder(),
                    topic.getCoverageStatus().name()
            );
        }
    }

    public record AxisItem(
            Long axisId,
            String name,
            int displayOrder,
            List<TopicItem> topics
    ) {
        public static AxisItem of(LearningAxis axis) {
            return new AxisItem(
                    axis.getId(),
                    axis.getName(),
                    axis.getDisplayOrder(),
                    axis.getTopics().stream()
                            .map(TopicItem::of)
                            .collect(Collectors.toList())
            );
        }
    }

    public record CoverageSummary(
            int totalTopics,
            int uncoveredTopics
    ) {}

    // ──────────────────────────────────────────────────────
    // 1. CreateFacade
    // ──────────────────────────────────────────────────────

    public record CreateFacade(
            Long facadeId,
            String concept,
            List<AxisItem> axes,
            LocalDateTime createdAt
    ) {
        public static CreateFacade of(LearningFacade facade) {
            return new CreateFacade(
                    facade.getId(),
                    facade.getConcept(),
                    List.of(),
                    facade.getCreatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 2. FacadeDetail
    // ──────────────────────────────────────────────────────

    public record FacadeDetail(
            Long facadeId,
            String concept,
            CoverageSummary coverageSummary,
            boolean isAxisCountExceedsRecommended,
            List<AxisItem> axes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static FacadeDetail of(LearningFacade facade) {
            List<AxisTopic> allTopics = facade.getAxes().stream()
                    .flatMap(axis -> axis.getTopics().stream())
                    .collect(Collectors.toList());

            int totalTopics = allTopics.size();
            int uncoveredTopics = (int) allTopics.stream()
                    .filter(t -> t.getCoverageStatus() == CoverageStatus.NO_MATERIAL)
                    .count();

            return new FacadeDetail(
                    facade.getId(),
                    facade.getConcept(),
                    new CoverageSummary(totalTopics, uncoveredTopics),
                    facade.isAxisCountExceedsRecommended(),
                    facade.getAxes().stream().map(AxisItem::of).collect(Collectors.toList()),
                    facade.getCreatedAt(),
                    facade.getUpdatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 3. UpdateConcept
    // ──────────────────────────────────────────────────────

    public record UpdateConcept(
            Long facadeId,
            String concept,
            boolean isConceptChanged,
            boolean isDrifted,
            LocalDateTime updatedAt
    ) {
        public static UpdateConcept of(LearningFacade facade, ConceptChangeRecord record) {
            return new UpdateConcept(
                    facade.getId(),
                    facade.getConcept(),
                    record.isChanged(),
                    record.isDrifted(),
                    facade.getUpdatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 4. AddAxis
    // ──────────────────────────────────────────────────────

    public record AddAxis(
            Long axisId,
            String name,
            int displayOrder,
            List<TopicItem> topics,
            boolean isAxisCountExceedsRecommended
    ) {
        public static AddAxis of(LearningAxis axis, boolean isAxisCountExceedsRecommended) {
            return new AddAxis(
                    axis.getId(),
                    axis.getName(),
                    axis.getDisplayOrder(),
                    List.of(),
                    isAxisCountExceedsRecommended
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 5. UpdateAxisName
    // ──────────────────────────────────────────────────────

    public record UpdateAxisName(
            Long axisId,
            String name,
            int displayOrder
    ) {
        public static UpdateAxisName of(LearningAxis axis) {
            return new UpdateAxisName(axis.getId(), axis.getName(), axis.getDisplayOrder());
        }
    }

    // ──────────────────────────────────────────────────────
    // 7. ReorderAxes
    // ──────────────────────────────────────────────────────

    public record AxisOrderItem(
            Long axisId,
            String name,
            int displayOrder
    ) {
        public static AxisOrderItem of(LearningAxis axis) {
            return new AxisOrderItem(axis.getId(), axis.getName(), axis.getDisplayOrder());
        }
    }

    public record ReorderAxes(
            List<AxisOrderItem> axes
    ) {
        public static ReorderAxes of(List<LearningAxis> axes) {
            return new ReorderAxes(
                    axes.stream().map(AxisOrderItem::of).collect(Collectors.toList())
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 8. AddTopic
    // ──────────────────────────────────────────────────────

    public record AddTopic(
            Long topicId,
            Long axisId,
            String name,
            String description,
            int displayOrder,
            String coverageStatus
    ) {
        public static AddTopic of(AxisTopic topic) {
            return new AddTopic(
                    topic.getId(),
                    topic.getAxis().getId(),
                    topic.getName(),
                    topic.getDescription(),
                    topic.getDisplayOrder(),
                    topic.getCoverageStatus().name()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 9. UpdateTopic
    // ──────────────────────────────────────────────────────

    public record UpdateTopic(
            Long topicId,
            Long axisId,
            String name,
            String description,
            int displayOrder,
            String coverageStatus,
            LocalDateTime updatedAt
    ) {
        public static UpdateTopic of(AxisTopic topic) {
            return new UpdateTopic(
                    topic.getId(),
                    topic.getAxis().getId(),
                    topic.getName(),
                    topic.getDescription(),
                    topic.getDisplayOrder(),
                    topic.getCoverageStatus().name(),
                    topic.getUpdatedAt()
            );
        }
    }
}
