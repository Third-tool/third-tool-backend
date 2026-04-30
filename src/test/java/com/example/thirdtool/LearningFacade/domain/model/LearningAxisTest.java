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

@DisplayName("LearningAxis")
class LearningAxisTest {

    private LearningFacade facade;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        facade = LearningFacade.create(user, "백엔드 개발자");
    }

    private LearningAxis createAxis() {
        return facade.addAxis("API 설계");
    }

    private static AxisTopic addTopicWithId(LearningAxis axis, String name, Long id) {
        AxisTopic topic = axis.addTopic(name, null);
        ReflectionTestUtils.setField(topic, "id", id);
        return topic;
    }

    @Nested
    @DisplayName("이름 수정")
    class UpdateName {

        @Test
        @DisplayName("유효한 새 이름으로 수정하면 name이 갱신된다")
        void updateName_valid() {
            LearningAxis axis = createAxis();
            axis.updateName("REST API 설계");
            assertThat(axis.getName()).isEqualTo("REST API 설계");
        }

        @Test
        @DisplayName("공백 문자열로 수정하면 예외가 발생한다")
        void updateName_blank_예외() {
            LearningAxis axis = createAxis();
            assertThatThrownBy(() -> axis.updateName("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("null로 수정하면 예외가 발생한다")
        void updateName_null_예외() {
            LearningAxis axis = createAxis();
            assertThatThrownBy(() -> axis.updateName(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    @Nested
    @DisplayName("주제 추가")
    class AddTopic {

        @Test
        @DisplayName("정상 추가 시 반환된 topic이 topics 컬렉션에 포함된다")
        void addTopic_topics컬렉션에포함() {
            LearningAxis axis = createAxis();
            AxisTopic topic = axis.addTopic("REST API 설계 원칙", null);
            assertThat(axis.getTopics()).contains(topic);
        }

        @Test
        @DisplayName("정상 추가 시 커버리지 초기 상태는 NO_MATERIAL이다")
        void addTopic_coverageStatus_NO_MATERIAL() {
            LearningAxis axis = createAxis();
            AxisTopic topic = axis.addTopic("REST API 설계 원칙", null);
            assertThat(topic.getCoverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL);
        }

        @Test
        @DisplayName("displayOrder는 현재 topics 수 + 1로 1-based로 자동 부여된다")
        void addTopic_displayOrder_1based() {
            LearningAxis axis = createAxis();
            AxisTopic first = axis.addTopic("REST API 설계 원칙", null);
            AxisTopic second = axis.addTopic("OpenAPI 문서화", "스펙 작성");
            assertThat(first.getDisplayOrder()).isEqualTo(1);
            assertThat(second.getDisplayOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("description은 nullable이고 빈 문자열은 null로 정규화된다")
        void addTopic_description_빈문자열_null로정규화() {
            LearningAxis axis = createAxis();
            AxisTopic topic = axis.addTopic("REST API 설계 원칙", "   ");
            assertThat(topic.getDescription()).isNull();
        }

        @Test
        @DisplayName("name이 null이면 예외가 발생한다")
        void addTopic_name_null_예외() {
            LearningAxis axis = createAxis();
            assertThatThrownBy(() -> axis.addTopic(null, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("name이 공백 문자열이면 예외가 발생한다")
        void addTopic_name_blank_예외() {
            LearningAxis axis = createAxis();
            assertThatThrownBy(() -> axis.addTopic("  ", null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("공백 포함 명사구 (예: 'REST API 설계 원칙')도 허용된다 — 단일 동사 강제 폐지")
        void addTopic_공백포함_허용() {
            LearningAxis axis = createAxis();
            AxisTopic topic = axis.addTopic("REST API 설계 원칙", null);
            assertThat(topic.getName()).isEqualTo("REST API 설계 원칙");
        }
    }

    @Nested
    @DisplayName("주제 다건 추가")
    class AddTopics {

        @Test
        @DisplayName("commands 순서대로 displayOrder가 1, 2, 3으로 부여된다")
        void addTopics_순차_displayOrder() {
            LearningAxis axis = createAxis();
            java.util.List<AxisTopic> added = axis.addTopics(java.util.List.of(
                    LearningAxis.TopicCommand.of("A", null),
                    LearningAxis.TopicCommand.of("B", "설명B"),
                    LearningAxis.TopicCommand.of("C", null)
            ));
            assertThat(added).hasSize(3);
            assertThat(added.get(0).getDisplayOrder()).isEqualTo(1);
            assertThat(added.get(1).getDisplayOrder()).isEqualTo(2);
            assertThat(added.get(2).getDisplayOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("빈 리스트나 null이 들어와도 예외 없이 빈 리스트를 반환한다")
        void addTopics_empty_빈리스트반환() {
            LearningAxis axis = createAxis();
            assertThat(axis.addTopics(null)).isEmpty();
            assertThat(axis.addTopics(java.util.List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("주제 삭제")
    class RemoveTopic {

        @Test
        @DisplayName("존재하는 topicId로 삭제하면 topics에서 제거된다")
        void removeTopic_valid() {
            LearningAxis axis = createAxis();
            addTopicWithId(axis, "REST API 설계 원칙", 100L);
            axis.removeTopic(100L);
            assertThat(axis.getTopics()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 topicId로 삭제하면 예외가 발생한다")
        void removeTopic_존재하지않는topicId_예외() {
            LearningAxis axis = createAxis();
            assertThatThrownBy(() -> axis.removeTopic(999L))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    @Nested
    @DisplayName("주제 순서 변경")
    class ReorderTopics {

        @Test
        @DisplayName("전달된 id 순서대로 displayOrder가 1부터 재부여된다")
        void reorderTopics_valid_순서재부여() {
            LearningAxis axis = createAxis();
            AxisTopic t1 = addTopicWithId(axis, "A", 100L);
            AxisTopic t2 = addTopicWithId(axis, "B", 101L);
            AxisTopic t3 = addTopicWithId(axis, "C", 102L);

            axis.reorderTopics(java.util.List.of(t3.getId(), t1.getId(), t2.getId()));

            assertThat(t3.getDisplayOrder()).isEqualTo(1);
            assertThat(t1.getDisplayOrder()).isEqualTo(2);
            assertThat(t2.getDisplayOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("topics가 없을 때 빈 리스트를 전달하면 정상 처리된다")
        void reorderTopics_빈목록_정상() {
            LearningAxis axis = createAxis();
            axis.reorderTopics(java.util.List.of());
        }

        @Test
        @DisplayName("전달된 id 수가 현재 topics 수와 다르면 예외가 발생한다")
        void reorderTopics_id수_불일치_예외() {
            LearningAxis axis = createAxis();
            AxisTopic t1 = addTopicWithId(axis, "A", 100L);
            addTopicWithId(axis, "B", 101L);
            addTopicWithId(axis, "C", 102L);

            assertThatThrownBy(() -> axis.reorderTopics(java.util.List.of(t1.getId())))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("현재 topics에 없는 id가 포함되면 예외가 발생한다")
        void reorderTopics_존재하지않는id_예외() {
            LearningAxis axis = createAxis();
            AxisTopic t1 = addTopicWithId(axis, "A", 100L);
            AxisTopic t2 = addTopicWithId(axis, "B", 101L);

            assertThatThrownBy(() -> axis.reorderTopics(java.util.List.of(t1.getId(), t2.getId(), 999L)))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    @Nested
    @DisplayName("권장 상한 안내")
    class TopicCountRecommended {

        @Test
        @DisplayName("topics가 10개 이하이면 isTopicCountExceedsRecommended가 false")
        void exceeds_10이하_false() {
            LearningAxis axis = createAxis();
            for (int i = 1; i <= 10; i++) axis.addTopic("주제 " + i, null);
            assertThat(axis.isTopicCountExceedsRecommended()).isFalse();
        }

        @Test
        @DisplayName("topics가 11개이면 isTopicCountExceedsRecommended가 true")
        void exceeds_11_true() {
            LearningAxis axis = createAxis();
            for (int i = 1; i <= 11; i++) axis.addTopic("주제 " + i, null);
            assertThat(axis.isTopicCountExceedsRecommended()).isTrue();
        }
    }
}
