package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.TopicDeletionRecord;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicDeletionRecordRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("LearningFacadeCommandService.removeTopic — TopicDeletionRecord 스냅샷 저장 (Story-003-4)")
class LearningFacadeCommandServiceRemoveTopicTest {

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
        topic = axis.addTopic("REST API 설계 원칙", "자원 중심 URI");
        ReflectionTestUtils.setField(axis, "id", 10L);
        ReflectionTestUtils.setField(topic, "id", 100L);

        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(facade));
    }

    @Test
    @DisplayName("주제 삭제 시 TopicDeletionRecord가 저장되고 facade가 save된다")
    void removeTopic_saves_archive_then_removes() {
        topic.updateName("REST 설계 가이드"); // revisionCount=1

        service.removeTopic(user, 10L, 100L);

        ArgumentCaptor<TopicDeletionRecord> captor = ArgumentCaptor.forClass(TopicDeletionRecord.class);
        verify(topicDeletionRecordRepository).save(captor.capture());
        TopicDeletionRecord snap = captor.getValue();

        assertThat(snap.getOriginalTopicId()).isEqualTo(100L);
        assertThat(snap.getLearningAxisId()).isEqualTo(10L);
        assertThat(snap.getName()).isEqualTo("REST 설계 가이드");
        assertThat(snap.getDescription()).isEqualTo("자원 중심 URI");
        assertThat(snap.getRevisionCount()).isEqualTo(1);

        assertThat(axis.getTopics()).extracting(AxisTopic::getId).doesNotContain(100L);
        verify(facadeRepository).save(facade);
    }

    @Test
    @DisplayName("미존재 topicId면 archive 미저장 + 도메인 예외 (LEARNING_AXIS_TOPIC_NOT_FOUND)")
    void removeTopic_unknown_id_no_archive() {
        assertThatThrownBy(() -> service.removeTopic(user, 10L, 9999L))
                .isInstanceOf(LearningFacadeDomainException.class);

        verify(topicDeletionRecordRepository, never()).save(any());
        verify(facadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("revisionCount가 0인 주제도 archive에 0 그대로 저장된다")
    void removeTopic_revisionCount_zero_preserved() {
        // setup의 topic은 변경 없는 상태(revisionCount=0)
        service.removeTopic(user, 10L, 100L);

        ArgumentCaptor<TopicDeletionRecord> captor = ArgumentCaptor.forClass(TopicDeletionRecord.class);
        verify(topicDeletionRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getRevisionCount()).isZero();
    }
}
