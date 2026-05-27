package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.infrastructure.persistence.TagRepository;
import com.example.thirdtool.Card.presentation.dto.CardRequest;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckProgressStatus;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CardCommandService의 Deck progressStatus 자동 갱신 트리거 검증 (Story-005-2).
 *
 * 도메인 상태 변화 자체를 관찰 (spy verify 대신) — 실제 Deck 인스턴스의 progressStatus 추적.
 */
@DisplayName("CardCommandService — Deck progressStatus 트리거 (Story-005-2)")
class CardCommandServiceDeckProgressTriggerTest {

    private CardRepository cardRepository;
    private TagRepository tagRepository;
    private DeckRepository deckRepository;
    private CardCommandService service;

    private UserEntity user;
    private Deck deck;

    @BeforeEach
    void setUp() {
        cardRepository = mock(CardRepository.class);
        tagRepository = mock(TagRepository.class);
        deckRepository = mock(DeckRepository.class);
        service = new CardCommandService(cardRepository, tagRepository, deckRepository);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        deck = Deck.createFromLearningMaterial(user, 10L, 200L, "DDD");
        ReflectionTestUtils.setField(deck, "id", 500L);

        when(deckRepository.findById(500L)).thenReturn(Optional.of(deck));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            if (c.getId() == null) ReflectionTestUtils.setField(c, "id", 1000L);
            return c;
        });
    }

    @Test
    @DisplayName("create — NOT_STARTED Deck에 첫 Card 추가 시 progressStatus가 IN_PROGRESS로 전환")
    void create_first_card_transitions_to_in_progress() {
        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);

        CardRequest.Create request = new CardRequest.Create(
                new CardRequest.MainNoteDto("본문 내용", null),
                List.of("키워드1"),
                "한 문장 요약입니다.",
                List.of()
        );

        service.create(500L, request);

        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("softDelete — Card 삭제 후 deck.recalculateProgressStatus 호출 (IN_PROGRESS → NOT_STARTED)")
    void softDelete_triggers_recalculate() {
        // given — deck은 IN_PROGRESS 상태. deck.cards 컬렉션은 단위 테스트에서 빈 상태 (JPA cascade 미적용)
        ReflectionTestUtils.setField(deck, "progressStatus", DeckProgressStatus.IN_PROGRESS);

        // 삭제 대상 Card: deck 소속, 활성. cardRepository.findById가 돌려준다.
        Card targetCard = Card.create(
                deck,
                com.example.thirdtool.Card.domain.model.MainNote.of("본문 내용", null),
                com.example.thirdtool.Card.domain.model.Summary.of("한 문장 요약입니다."),
                List.of("키워드1"),
                List.of()
        );
        ReflectionTestUtils.setField(targetCard, "id", 1000L);
        when(cardRepository.findById(1000L)).thenReturn(Optional.of(targetCard));

        // when
        service.softDelete(1000L);

        // then — recalculate가 호출되었으면 빈 cards 컬렉션 기준으로 NOT_STARTED로 회귀
        // (호출 안 됐다면 IN_PROGRESS 그대로 남는다)
        assertThat(deck.getProgressStatus()).isEqualTo(DeckProgressStatus.NOT_STARTED);
        assertThat(targetCard.isDeleted()).isTrue();
    }
}
