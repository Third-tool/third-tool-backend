package com.example.thirdtool.Deck.application.event;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.LearningFacade.domain.event.LearningMaterialDeletedEvent;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LearningMaterialDeletedEventHandler — 자료 삭제 시 연결 Deck의 자료 참조 끊기 (Story-005-2)")
class LearningMaterialDeletedEventHandlerTest {

    private DeckRepository deckRepository;
    private LearningMaterialDeletedEventHandler handler;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        deckRepository = mock(DeckRepository.class);
        handler = new LearningMaterialDeletedEventHandler(deckRepository);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("handle_연결_Deck_여러건 — 모두 markMaterialDeleted 적용 (learningMaterialId=null)")
    void handle_multiple_affected_decks() {
        Deck deck1 = Deck.createFromLearningMaterial(user, 10L, 200L, "DDD");
        Deck deck2 = Deck.createFromLearningMaterial(user, 10L, 200L, "DDD (2)");
        ReflectionTestUtils.setField(deck1, "id", 500L);
        ReflectionTestUtils.setField(deck2, "id", 501L);
        when(deckRepository.findByLearningMaterialIdAndDeletedFalse(200L))
                .thenReturn(List.of(deck1, deck2));

        handler.handle(new LearningMaterialDeletedEvent(1L, 200L));

        verify(deckRepository).findByLearningMaterialIdAndDeletedFalse(200L);
        assertThat(deck1.getLearningMaterialId()).isNull();
        assertThat(deck2.getLearningMaterialId()).isNull();
        // axisId는 보존 (로드맵 추적성)
        assertThat(deck1.getAxisId()).isEqualTo(10L);
        assertThat(deck2.getAxisId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("handle_연결_Deck_없음 — 부작용 없음 (lookup만 수행)")
    void handle_no_affected_deck_noop() {
        when(deckRepository.findByLearningMaterialIdAndDeletedFalse(999L))
                .thenReturn(List.of());

        handler.handle(new LearningMaterialDeletedEvent(1L, 999L));

        verify(deckRepository).findByLearningMaterialIdAndDeletedFalse(999L);
        verify(deckRepository, never()).save(any(Deck.class));
    }

    @Test
    @DisplayName("handle_이미_자료미연결_Deck — markMaterialDeleted 멱등 (예외 X)")
    void handle_already_unlinked_idempotent() {
        Deck orphan = Deck.createFromLearningMaterial(user, 10L, 200L, "고아");
        orphan.markMaterialDeleted();
        ReflectionTestUtils.setField(orphan, "id", 700L);
        when(deckRepository.findByLearningMaterialIdAndDeletedFalse(200L))
                .thenReturn(List.of(orphan));

        handler.handle(new LearningMaterialDeletedEvent(1L, 200L));

        assertThat(orphan.getLearningMaterialId()).isNull();
    }

    private static <T> T any(Class<T> type) {
        return org.mockito.ArgumentMatchers.any(type);
    }
}
