package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckCommandService;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicDeletionRecordRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LearningFacadeCommandService.createDeckUnderAxis 단위 테스트 (Fix-Story 2).
 *
 * <p>facade/axis 소유권 검증 흐름 + DeckCommandService 위임을 검증.
 * 도메인 객체(LearningFacade, LearningAxis)는 실제 인스턴스, BC 경계는 Mock.
 */
@DisplayName("LearningFacadeCommandService.createDeckUnderAxis (Fix-Story 2)")
class LearningFacadeCommandServiceCreateDeckUnderAxisTest {

    private LearningFacadeRepository facadeRepository;
    private DeckCommandService deckCommandService;
    private LearningFacadeCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        deckCommandService = mock(DeckCommandService.class);

        service = new LearningFacadeCommandService(
                facadeRepository,
                mock(TopicRevisionRepository.class),
                mock(RevisionReasonOptionRepository.class),
                mock(TopicDeletionRecordRepository.class),
                mock(LearningMaterialRepository.class),
                mock(TopicMaterialRepository.class),
                mock(ApplicationEventPublisher.class),
                deckCommandService);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        ReflectionTestUtils.setField(facade, "id", 50L);
        axis = facade.addAxis("시스템 설계");
        ReflectionTestUtils.setField(axis, "id", 10L);
    }

    @Test
    @DisplayName("정상 위임 — facade 로드 + axis 검증 후 DeckCommandService에 user/axisId/name/axisName 전달")
    void 정상_위임() {
        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.of(facade));
        DeckResponse.Create expected = new DeckResponse.Create(
                999L, "새 Deck", null, 0, false, null, LocalDateTime.now(), LocalDateTime.now(), 10L, "시스템 설계");
        when(deckCommandService.createUnderAxis(user, 10L, "새 Deck", "시스템 설계"))
                .thenReturn(expected);

        DeckResponse.Create result = service.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(1L, 10L, "새 Deck"));

        assertThat(result).isSameAs(expected);
        verify(deckCommandService).createUnderAxis(user, 10L, "새 Deck", "시스템 설계");
    }

    @Test
    @DisplayName("facade 미존재 — LEARNING_FACADE_NOT_FOUND, DeckCommandService 호출 안 함")
    void facade_미존재_예외() {
        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(1L, 10L, "새 Deck")))
                .isInstanceOf(LearningFacadeDomainException.class)
                .matches(e -> ((LearningFacadeDomainException) e).getErrorCode() == ErrorCode.LEARNING_FACADE_NOT_FOUND);

        verify(deckCommandService, never()).createUnderAxis(
                any(), any(), any(), any());
    }

    @Test
    @DisplayName("axis 미존재 (또는 타 유저 axisId) — LEARNING_AXIS_NOT_FOUND, DeckCommandService 호출 안 함")
    void axis_미존재_예외() {
        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.of(facade));

        assertThatThrownBy(() -> service.createDeckUnderAxis(
                new LearningFacadeCommand.CreateAxisDeck(1L, 99_999L, "새 Deck")))
                .isInstanceOf(LearningFacadeDomainException.class)
                .matches(e -> ((LearningFacadeDomainException) e).getErrorCode() == ErrorCode.LEARNING_AXIS_NOT_FOUND);

        verify(deckCommandService, never()).createUnderAxis(
                any(), any(), any(), any());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
