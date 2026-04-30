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

@DisplayName("AxisTopic")
class AxisTopicTest {

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        axis = facade.addAxis("API 설계");
    }

    private AxisTopic createTopic() {
        return axis.addTopic("REST API 설계 원칙", "자원 중심 URI");
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("정상 생성 시 name·description·displayOrder가 설정되고 coverageStatus가 NO_MATERIAL이다")
        void create_valid() {
            AxisTopic topic = axis.addTopic("REST API 설계 원칙", "자원 중심 URI");

            assertThat(topic.getName()).isEqualTo("REST API 설계 원칙");
            assertThat(topic.getDescription()).isEqualTo("자원 중심 URI");
            assertThat(topic.getDisplayOrder()).isEqualTo(1);
            assertThat(topic.getCoverageStatus()).isEqualTo(CoverageStatus.NO_MATERIAL);
        }

        @Test
        @DisplayName("description은 null 허용 — 부연 설명 미입력")
        void create_description_null() {
            AxisTopic topic = axis.addTopic("주제", null);
            assertThat(topic.getDescription()).isNull();
        }

        @Test
        @DisplayName("description의 빈 문자열은 null로 정규화된다")
        void create_description_empty_string_normalized_to_null() {
            AxisTopic topic = axis.addTopic("주제", "   ");
            assertThat(topic.getDescription()).isNull();
        }

        @Test
        @DisplayName("name 앞뒤 공백은 trim된다")
        void create_name_trim() {
            AxisTopic topic = axis.addTopic("  주제  ", null);
            assertThat(topic.getName()).isEqualTo("주제");
        }

        @Test
        @DisplayName("name이 null이면 예외")
        void create_name_null_예외() {
            assertThatThrownBy(() -> axis.addTopic(null, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("name이 공백이면 예외")
        void create_name_blank_예외() {
            assertThatThrownBy(() -> axis.addTopic("  ", null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    @Nested
    @DisplayName("이름 수정")
    class UpdateName {

        @Test
        @DisplayName("다른 값으로 수정하면 true 반환 + name 갱신")
        void updateName_changed() {
            AxisTopic topic = createTopic();
            boolean changed = topic.updateName("API 명세 작성");
            assertThat(changed).isTrue();
            assertThat(topic.getName()).isEqualTo("API 명세 작성");
        }

        @Test
        @DisplayName("동일 값(trim 후)으로 수정하면 false 반환 + name 불변")
        void updateName_unchanged() {
            AxisTopic topic = createTopic();
            boolean changed = topic.updateName("  REST API 설계 원칙  ");
            assertThat(changed).isFalse();
            assertThat(topic.getName()).isEqualTo("REST API 설계 원칙");
        }

        @Test
        @DisplayName("null·blank는 예외")
        void updateName_blank_예외() {
            AxisTopic topic = createTopic();
            assertThatThrownBy(() -> topic.updateName(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
            assertThatThrownBy(() -> topic.updateName("  "))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    @Nested
    @DisplayName("설명 수정")
    class UpdateDescription {

        @Test
        @DisplayName("다른 값으로 수정하면 true 반환 + description 갱신")
        void updateDescription_changed() {
            AxisTopic topic = createTopic();
            boolean changed = topic.updateDescription("새 설명");
            assertThat(changed).isTrue();
            assertThat(topic.getDescription()).isEqualTo("새 설명");
        }

        @Test
        @DisplayName("동일 값(trim 후)으로 수정하면 false 반환")
        void updateDescription_unchanged() {
            AxisTopic topic = createTopic();
            boolean changed = topic.updateDescription("  자원 중심 URI  ");
            assertThat(changed).isFalse();
        }

        @Test
        @DisplayName("null로 수정하면 description이 제거된다")
        void updateDescription_null_remove() {
            AxisTopic topic = createTopic();
            boolean changed = topic.updateDescription(null);
            assertThat(changed).isTrue();
            assertThat(topic.getDescription()).isNull();
        }

        @Test
        @DisplayName("빈 문자열은 null로 정규화되어 description이 제거된다")
        void updateDescription_empty_normalized_remove() {
            AxisTopic topic = createTopic();
            boolean changed = topic.updateDescription("   ");
            assertThat(changed).isTrue();
            assertThat(topic.getDescription()).isNull();
        }

        @Test
        @DisplayName("이미 null인 description에 null을 다시 입력하면 false")
        void updateDescription_null_to_null_unchanged() {
            AxisTopic topic = axis.addTopic("주제", null);
            boolean changed = topic.updateDescription(null);
            assertThat(changed).isFalse();
        }
    }

    @Nested
    @DisplayName("커버리지 상태 변경")
    class UpdateCoverageStatus {

        @Test
        @DisplayName("Application Service가 호출하면 상태가 변경된다")
        void updateCoverageStatus_valid() {
            AxisTopic topic = createTopic();
            topic.updateCoverageStatus(CoverageStatus.PARTIAL);
            assertThat(topic.getCoverageStatus()).isEqualTo(CoverageStatus.PARTIAL);
        }

        @Test
        @DisplayName("null이면 예외")
        void updateCoverageStatus_null_예외() {
            AxisTopic topic = createTopic();
            assertThatThrownBy(() -> topic.updateCoverageStatus(null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }

    @Nested
    @DisplayName("isFocused")
    class IsFocused {

        @Test
        @DisplayName("displayOrder가 threshold 이하이면 true")
        void isFocused_within_threshold() {
            AxisTopic t1 = axis.addTopic("A", null);
            AxisTopic t2 = axis.addTopic("B", null);
            AxisTopic t3 = axis.addTopic("C", null);
            assertThat(t1.isFocused(3)).isTrue();
            assertThat(t2.isFocused(3)).isTrue();
            assertThat(t3.isFocused(3)).isTrue();
        }

        @Test
        @DisplayName("displayOrder가 threshold를 초과하면 false")
        void isFocused_above_threshold() {
            for (int i = 1; i <= 4; i++) axis.addTopic("주제 " + i, null);
            AxisTopic last = axis.getTopics().get(3);
            assertThat(last.isFocused(3)).isFalse();
        }
    }
}
