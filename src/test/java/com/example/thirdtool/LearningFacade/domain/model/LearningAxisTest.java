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
}
