package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TopicDeletionRecord")
class TopicDeletionRecordTest {

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-del", "encoded-pw", "닉네임-del", "del@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        axis = facade.addAxis("API 설계");
        ReflectionTestUtils.setField(axis, "id", 10L);
    }

    private AxisTopic persistedTopic(Long topicId, String name, String description) {
        AxisTopic topic = axis.addTopic(name, description);
        ReflectionTestUtils.setField(topic, "id", topicId);
        return topic;
    }

    @Nested
    @DisplayName("of(AxisTopic) 스냅샷")
    class SnapshotFactory {

        @Test
        @DisplayName("정상 캡처 — 이름·설명·revisionCount + 원본 id·축 id 보존")
        void snapshot_valid() {
            AxisTopic topic = persistedTopic(100L, "REST API 설계 원칙", "자원 중심 URI");
            topic.updateName("REST 가이드"); // revisionCount=1

            TopicDeletionRecord record = TopicDeletionRecord.of(topic);

            assertThat(record.getOriginalTopicId()).isEqualTo(100L);
            assertThat(record.getLearningAxisId()).isEqualTo(10L);
            assertThat(record.getName()).isEqualTo("REST 가이드");
            assertThat(record.getDescription()).isEqualTo("자원 중심 URI");
            assertThat(record.getRevisionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("description이 null인 주제도 스냅샷 가능 (null 보존)")
        void snapshot_description_null() {
            AxisTopic topic = persistedTopic(101L, "주제만", null);
            TopicDeletionRecord record = TopicDeletionRecord.of(topic);
            assertThat(record.getDescription()).isNull();
        }

        @Test
        @DisplayName("revisionCount가 0인 주제는 0이 그대로 저장된다")
        void snapshot_revisionCount_zero() {
            AxisTopic topic = persistedTopic(102L, "건드리지 않은 주제", null);
            TopicDeletionRecord record = TopicDeletionRecord.of(topic);
            assertThat(record.getRevisionCount()).isZero();
        }

        @Test
        @DisplayName("topic이 null이면 예외")
        void snapshot_topic_null_예외() {
            assertThatThrownBy(() -> TopicDeletionRecord.of(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("영속화되지 않은(id=null) 주제는 스냅샷 거부")
        void snapshot_topic_id_null_예외() {
            AxisTopic transientTopic = axis.addTopic("아직 저장 안됨", null);
            // id 주입하지 않음
            assertThatThrownBy(() -> TopicDeletionRecord.of(transientTopic))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }
}
