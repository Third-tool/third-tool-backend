package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.RevisionReasonOption;
import com.example.thirdtool.LearningFacade.domain.model.TopicRevision;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicDeletionRecordRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("LearningFacadeCommandService.updateTopic — 주제 이름 수정 + 이력 저장 (Story-003-2)")
class LearningFacadeCommandServiceUpdateTopicTest {

    private LearningFacadeRepository facadeRepository;
    private TopicRevisionRepository topicRevisionRepository;
    private RevisionReasonOptionRepository revisionReasonOptionRepository;
    private TopicDeletionRecordRepository topicDeletionRecordRepository;
    private LearningFacadeCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;
    private AxisTopic topic;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        topicRevisionRepository = mock(TopicRevisionRepository.class);
        revisionReasonOptionRepository = mock(RevisionReasonOptionRepository.class);
        topicDeletionRecordRepository = mock(TopicDeletionRecordRepository.class);

        service = new LearningFacadeCommandService(
                facadeRepository, topicRevisionRepository, revisionReasonOptionRepository,
                topicDeletionRecordRepository);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        axis = facade.addAxis("API 설계");
        topic = axis.addTopic("REST API 설계 원칙", null);
        ReflectionTestUtils.setField(axis, "id", 10L);
        ReflectionTestUtils.setField(topic, "id", 100L);

        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(facade));
    }

    private LearningFacadeCommand.UpdateTopic command(String name, Long reasonOptionId) {
        return new LearningFacadeCommand.UpdateTopic(
                user.getId(), 10L, 100L,
                name, null, reasonOptionId,
                /* namePresent */ true,
                /* descriptionPresent */ false);
    }

    private LearningFacadeCommand.UpdateTopic descriptionOnlyCommand(String description) {
        return new LearningFacadeCommand.UpdateTopic(
                user.getId(), 10L, 100L,
                null, description, null,
                /* namePresent */ false,
                /* descriptionPresent */ true);
    }

    @Nested
    @DisplayName("이름 변경 + 이유 선택")
    class WithReason {

        @Test
        @DisplayName("이름 변경 발생 시 이력에 previousName/newName/reasonLabel이 저장된다")
        void name_changed_with_reason_saves_revision() {
            RevisionReasonOption reason = RevisionReasonOption.of("더 정확한 표현을 찾았다", 3, true);
            ReflectionTestUtils.setField(reason, "id", 30L);
            when(revisionReasonOptionRepository.findActiveById(30L)).thenReturn(Optional.of(reason));

            service.updateTopic(command("REST API 설계 가이드", 30L));

            ArgumentCaptor<TopicRevision> captor = ArgumentCaptor.forClass(TopicRevision.class);
            verify(topicRevisionRepository).save(captor.capture());
            TopicRevision saved = captor.getValue();
            assertThat(saved.getPreviousName()).isEqualTo("REST API 설계 원칙");
            assertThat(saved.getNewName()).isEqualTo("REST API 설계 가이드");
            assertThat(saved.getRevisionReasonLabel()).isEqualTo("더 정확한 표현을 찾았다");
            verify(facadeRepository).save(facade);
        }
    }

    @Nested
    @DisplayName("이름 변경 + 이유 미선택")
    class WithoutReason {

        @Test
        @DisplayName("reasonId가 null이면 reasonLabel은 null로 이력 생성 (수정 자체는 허용)")
        void name_changed_no_reason_saves_revision_with_null_label() {
            service.updateTopic(command("REST API 설계 가이드", null));

            ArgumentCaptor<TopicRevision> captor = ArgumentCaptor.forClass(TopicRevision.class);
            verify(topicRevisionRepository).save(captor.capture());
            assertThat(captor.getValue().getRevisionReasonLabel()).isNull();
            verifyNoInteractions(revisionReasonOptionRepository);
        }
    }

    @Nested
    @DisplayName("이름 미변경")
    class NoChange {

        @Test
        @DisplayName("동일 값(trim 후) 입력 시 이력이 생성되지 않는다")
        void same_name_no_revision() {
            service.updateTopic(command("REST API 설계 원칙", 30L));

            verify(topicRevisionRepository, never()).save(any());
            verify(facadeRepository, never()).save(any());
        }

        @Test
        @DisplayName("name 필드 누락(설명만 변경)이면 이력이 생성되지 않는다")
        void description_only_no_revision() {
            service.updateTopic(descriptionOnlyCommand("자원 중심 URI"));

            verify(topicRevisionRepository, never()).save(any());
            verify(facadeRepository).save(facade);
        }
    }

    @Nested
    @DisplayName("이유 선택지 검증")
    class ReasonValidation {

        @Test
        @DisplayName("비활성 선택지 id를 전달하면 REVISION_REASON_NOT_FOUND 예외 + 이력 미저장")
        void inactive_reason_throws_and_no_save() {
            when(revisionReasonOptionRepository.findActiveById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.updateTopic(command("REST API 설계 가이드", 99L)))
                    .isInstanceOf(LearningFacadeDomainException.class);

            verify(topicRevisionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("isRefinementSuggested 플래그 응답 (Story-003-3)")
    class RefinementFlagInResponse {

        @Test
        @DisplayName("revisionCount=0 신규 주제는 응답에 isRefinementSuggested=false")
        void noChange_flag_false() {
            var response = service.updateTopic(command("REST API 설계 원칙", null));   // 동일 값 → 미증가
            assertThat(response.revisionCount()).isZero();
            assertThat(response.isRefinementSuggested()).isFalse();
        }

        @Test
        @DisplayName("이름 1회 변경 후 응답 revisionCount=1, isRefinementSuggested=false")
        void changed_once_flag_false() {
            var response = service.updateTopic(command("API 명세 작성", null));
            assertThat(response.revisionCount()).isEqualTo(1);
            assertThat(response.isRefinementSuggested()).isFalse();
        }

        @Test
        @DisplayName("이름 3회 누적 변경 후 응답 isRefinementSuggested=true (임계값 도달)")
        void changed_three_times_flag_true() {
            service.updateTopic(command("v1", null));
            service.updateTopic(command("v2", null));
            var response = service.updateTopic(command("v3", null));
            assertThat(response.revisionCount()).isEqualTo(3);
            assertThat(response.isRefinementSuggested()).isTrue();
        }
    }
}
