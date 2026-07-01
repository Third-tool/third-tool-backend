package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Card.application.service.CardQueryService;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("LearningFacadeQueryService.findAxisCards — 축 스코프 카드 조회 (fix-deck-axis-visibility Fix-Story 4)")
class LearningFacadeQueryServiceFindAxisCardsTest {

    private LearningFacadeRepository facadeRepository;
    private CardQueryService cardQueryService;
    private LearningFacadeQueryService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis ownedAxis;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        TopicMaterialRepository topicMaterialRepository = mock(TopicMaterialRepository.class);
        DeckQueryService deckQueryService = mock(DeckQueryService.class);
        cardQueryService = mock(CardQueryService.class);
        service = new LearningFacadeQueryService(facadeRepository, topicMaterialRepository, deckQueryService, cardQueryService);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드 개발자");
        ownedAxis = facade.addAxis("API 설계");
        ReflectionTestUtils.setField(facade, "id", 1L);
        ReflectionTestUtils.setField(ownedAxis, "id", 10L);

        when(facadeRepository.findByUserId(user.getId())).thenReturn(Optional.of(facade));
    }

    @Test
    @DisplayName("findAxisCards_해피_소유축_status명시_CardQueryService위임")
    void findAxisCards_happy_delegates_to_card_query_service() {
        List<CardResponse.Summary> expected = List.of();
        when(cardQueryService.findByAxisIds(eq(user.getId()), eq(List.of(10L)), eq(CardStatus.ON_FIELD)))
                .thenReturn(expected);

        List<CardResponse.Summary> result = service.findAxisCards(
                new LearningFacadeQuery.FindAxisCards(user.getId(), 10L, CardStatus.ON_FIELD));

        assertThat(result).isSameAs(expected);
        verify(cardQueryService).findByAxisIds(user.getId(), List.of(10L), CardStatus.ON_FIELD);
    }

    @Test
    @DisplayName("findAxisCards_해피_status미지정_null이면_ON_FIELD기본값")
    void findAxisCards_default_status_is_on_field_when_null() {
        when(cardQueryService.findByAxisIds(eq(user.getId()), eq(List.of(10L)), eq(CardStatus.ON_FIELD)))
                .thenReturn(List.of());

        service.findAxisCards(
                new LearningFacadeQuery.FindAxisCards(user.getId(), 10L, null));

        verify(cardQueryService).findByAxisIds(user.getId(), List.of(10L), CardStatus.ON_FIELD);
    }

    @Test
    @DisplayName("findAxisCards_해피_ARCHIVE지정_status그대로전달")
    void findAxisCards_archive_status_passthrough() {
        when(cardQueryService.findByAxisIds(eq(user.getId()), eq(List.of(10L)), eq(CardStatus.ARCHIVE)))
                .thenReturn(List.of());

        service.findAxisCards(
                new LearningFacadeQuery.FindAxisCards(user.getId(), 10L, CardStatus.ARCHIVE));

        verify(cardQueryService).findByAxisIds(user.getId(), List.of(10L), CardStatus.ARCHIVE);
    }

    @Test
    @DisplayName("findAxisCards_예외_LearningFacade미존재_LEARNING_FACADE_NOT_FOUND")
    void findAxisCards_facade_not_found_throws() {
        when(facadeRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAxisCards(
                new LearningFacadeQuery.FindAxisCards(99L, 10L, CardStatus.ON_FIELD)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_NOT_FOUND);

        verifyNoInteractions(cardQueryService);
    }

    @Test
    @DisplayName("findAxisCards_예외_다른유저의axisId_LEARNING_FACADE_FORBIDDEN")
    void findAxisCards_axis_not_owned_throws() {
        long notOwnedAxisId = 999L;

        assertThatThrownBy(() -> service.findAxisCards(
                new LearningFacadeQuery.FindAxisCards(user.getId(), notOwnedAxisId, CardStatus.ON_FIELD)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_FACADE_FORBIDDEN);

        verifyNoInteractions(cardQueryService);
    }
}
