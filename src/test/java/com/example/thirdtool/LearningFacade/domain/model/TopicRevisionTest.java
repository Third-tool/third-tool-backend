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

@DisplayName("TopicRevision")
class TopicRevisionTest {

    private AxisTopic topic;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal(
                "tester-1", "encoded-pw", "닉네임-1", "tester1@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        LearningAxis axis = facade.addAxis("API 설계");
        topic = axis.addTopic("REST API 설계 원칙", null);
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("정상 생성 — 이전/이후 이름 + 이유 라벨 스냅샷이 보존된다")
        void create_valid() {
            TopicRevision revision = TopicRevision.of(
                    topic, "REST API 설계 원칙", "REST API 설계 가이드", "더 정확한 표현을 찾았다");

            assertThat(revision.getTopic()).isSameAs(topic);
            assertThat(revision.getPreviousName()).isEqualTo("REST API 설계 원칙");
            assertThat(revision.getNewName()).isEqualTo("REST API 설계 가이드");
            assertThat(revision.getRevisionReasonLabel()).isEqualTo("더 정확한 표현을 찾았다");
        }

        @Test
        @DisplayName("revisionReasonLabel은 null 허용 — 이유 미선택 이력")
        void create_reasonLabel_null_허용() {
            TopicRevision revision = TopicRevision.of(
                    topic, "이전", "이후", null);
            assertThat(revision.getRevisionReasonLabel()).isNull();
        }

        @Test
        @DisplayName("topic이 null이면 예외")
        void create_topic_null_예외() {
            assertThatThrownBy(() -> TopicRevision.of(null, "이전", "이후", null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("previousName이 blank면 예외")
        void create_previousName_blank_예외() {
            assertThatThrownBy(() -> TopicRevision.of(topic, "  ", "이후", null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }

        @Test
        @DisplayName("newName이 null이면 예외")
        void create_newName_null_예외() {
            assertThatThrownBy(() -> TopicRevision.of(topic, "이전", null, null))
                    .isInstanceOf(LearningFacadeDomainException.class);
        }
    }
}
