package com.example.thirdtool.Deck.application.event;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.LearningFacade.domain.event.LearningAxisCreatedEvent;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LearningAxisCreatedEventHandler — Axis 기반 Deck 자동 생성 (Fix-Story 1)")
class LearningAxisCreatedEventHandlerTest {

    private DeckRepository deckRepository;
    private UserRepository userRepository;
    private LearningAxisCreatedEventHandler handler;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        deckRepository = mock(DeckRepository.class);
        userRepository = mock(UserRepository.class);
        handler = new LearningAxisCreatedEventHandler(deckRepository, userRepository);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> {
            Deck d = inv.getArgument(0);
            if (d.getId() == null) ReflectionTestUtils.setField(d, "id", 999L);
            return d;
        });
    }

    private LearningAxisCreatedEvent event() {
        return new LearningAxisCreatedEvent(1L, 100L, "Java 심화");
    }

    // ─── 해피 ──────────────────────────────────────────────────

    @Test
    @DisplayName("handle_valid_덱_생성됨 — axisId 기준 Deck 1개 생성")
    void handle_valid_덱_생성됨() {
        when(deckRepository.existsByAxisIdAndDeletedFalse(100L)).thenReturn(false);

        handler.handle(event());

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        Deck saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Java 심화");
        assertThat(saved.getAxisId()).isEqualTo(100L);
        assertThat(saved.getLearningMaterialId()).isNull();
        assertThat(saved.getUser()).isEqualTo(user);
    }

    // ─── 엣지 ──────────────────────────────────────────────────

    @Test
    @DisplayName("handle_axisId_이미_Deck_존재_멱등_처리 — 재생성 없음")
    void handle_axisId_이미_Deck_존재_멱등_처리() {
        when(deckRepository.existsByAxisIdAndDeletedFalse(100L)).thenReturn(true);

        handler.handle(event());

        verify(deckRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    // ─── 예외 ──────────────────────────────────────────────────

    @Test
    @DisplayName("handle_userId_없음_예외 — USER_NOT_FOUND throw")
    void handle_userId_없음_예외() {
        when(deckRepository.existsByAxisIdAndDeletedFalse(100L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(event()))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.USER_NOT_FOUND);

        verify(deckRepository, never()).save(any());
    }
}
