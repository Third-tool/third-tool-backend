package com.example.thirdtool.LearningFacade.presentation.dto;

import com.example.thirdtool.LearningFacade.domain.model.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LearningMaterialResponse {

    // ──────────────────────────────────────────────────────
    // 공유 내부 레코드
    // ──────────────────────────────────────────────────────

    public record LinkedTopicItem(
            Long topicId,
            String name,
            Long axisId,
            String axisName,
            String coverageStatus
    ) {
        public static LinkedTopicItem of(TopicMaterial mapping) {
            AxisTopic topic = mapping.getTopic();
            LearningAxis axis = topic.getAxis();
            return new LinkedTopicItem(
                    topic.getId(),
                    topic.getName(),
                    axis.getId(),
                    axis.getName(),
                    topic.getCoverageStatus().name()
            );
        }
    }

    public record LinkedTopicSummary(
            Long topicId,
            String name,
            Long axisId,
            String axisName
    ) {
        public static LinkedTopicSummary of(TopicMaterial mapping) {
            AxisTopic topic = mapping.getTopic();
            LearningAxis axis = topic.getAxis();
            return new LinkedTopicSummary(
                    topic.getId(),
                    topic.getName(),
                    axis.getId(),
                    axis.getName()
            );
        }
    }

    public record TopicCoverageItem(
            Long topicId,
            String name,
            String coverageStatus
    ) {
        public static TopicCoverageItem of(AxisTopic topic) {
            return new TopicCoverageItem(
                    topic.getId(),
                    topic.getName(),
                    topic.getCoverageStatus().name()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 12. CreateMaterial — 부가 속성 5종 + linkedTopics + 커버리지 반영 결과
    // ──────────────────────────────────────────────────────

    public record CreateMaterial(
            Long materialId,
            String name,
            String materialType,
            String url,
            String author,
            String platform,
            String aiProvider,
            String webSource,
            String memo,
            String proficiencyLevel,
            List<LinkedTopicItem> linkedTopics,
            LocalDateTime createdAt,
            // Story-005-1: 자동 생성된 Deck 정보. 항상 true·non-null (Story 5-1 흐름은 Deck 동반 생성을 보장).
            boolean deckCreated,
            Long deckId,
            String deckName
    ) {
        public static CreateMaterial of(LearningMaterial material, Long deckId, String deckName) {
            return new CreateMaterial(
                    material.getId(),
                    material.getName(),
                    material.getMaterialType().name(),
                    material.getUrl(),
                    material.getAuthor(),
                    material.getPlatform(),
                    material.getAiProvider(),
                    material.getWebSource(),
                    material.getMemo(),
                    material.getProficiencyLevel().name(),
                    material.getTopicMappings().stream()
                            .map(LinkedTopicItem::of)
                            .collect(Collectors.toList()),
                    material.getCreatedAt(),
                    deckId != null,
                    deckId,
                    deckName
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 13. MaterialSummary (목록) — 부가 속성 5종 동반
    // ──────────────────────────────────────────────────────

    public record MaterialSummary(
            Long materialId,
            String name,
            String materialType,
            String url,
            String author,
            String platform,
            String aiProvider,
            String webSource,
            String memo,
            String proficiencyLevel,
            List<LinkedTopicSummary> linkedTopics,
            LocalDateTime createdAt
    ) {
        public static MaterialSummary of(LearningMaterial material) {
            return new MaterialSummary(
                    material.getId(),
                    material.getName(),
                    material.getMaterialType().name(),
                    material.getUrl(),
                    material.getAuthor(),
                    material.getPlatform(),
                    material.getAiProvider(),
                    material.getWebSource(),
                    material.getMemo(),
                    material.getProficiencyLevel().name(),
                    material.getTopicMappings().stream()
                            .map(LinkedTopicSummary::of)
                            .collect(Collectors.toList()),
                    material.getCreatedAt()
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 14. UpdateMaterialName
    // ──────────────────────────────────────────────────────

    public record UpdateMaterialName(
            Long materialId,
            String name
    ) {
        public static UpdateMaterialName of(LearningMaterial material) {
            return new UpdateMaterialName(material.getId(), material.getName());
        }
    }

    // ──────────────────────────────────────────────────────
    // 15. UpdateProficiency
    // ──────────────────────────────────────────────────────

    public record UpdateProficiency(
            Long materialId,
            String proficiencyLevel,
            List<TopicCoverageItem> updatedCoverages,
            boolean isCardCreationSuggested
    ) {
        public static UpdateProficiency of(LearningMaterial material,
                                           List<TopicCoverageItem> updatedCoverages) {
            boolean isCardCreationSuggested =
                    material.getProficiencyLevel() == ProficiencyLevel.MASTERED;
            return new UpdateProficiency(
                    material.getId(),
                    material.getProficiencyLevel().name(),
                    updatedCoverages,
                    isCardCreationSuggested
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 16. LinkedTopics (연결 추가 응답)
    // ──────────────────────────────────────────────────────

    public record LinkedTopics(
            Long materialId,
            List<LinkedTopicItem> linkedTopics
    ) {
        public static LinkedTopics of(LearningMaterial material) {
            return new LinkedTopics(
                    material.getId(),
                    material.getTopicMappings().stream()
                            .map(LinkedTopicItem::of)
                            .collect(Collectors.toList())
            );
        }
    }

    // ──────────────────────────────────────────────────────
    // 17. UnlinkTopic (연결 해제 응답)
    // ──────────────────────────────────────────────────────

    public record UnlinkTopic(
            Long materialId,
            Long unlinkedTopicId,
            TopicCoverageItem updatedCoverage,
            List<LinkedTopicSummary> remainingLinkedTopics
    ) {}
}
