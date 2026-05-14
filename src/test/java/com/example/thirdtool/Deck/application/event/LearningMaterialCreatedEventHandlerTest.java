package com.example.thirdtool.Deck.application.event;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.LearningFacade.domain.event.LearningMaterialCreatedEvent;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("LearningMaterialCreatedEventHandler — 동명 검사 + 자료 기반 Deck 자동 생성 (Story-005-1)")
class LearningMaterialCreatedEventHandlerTest {

    private DeckRepository deckRepository;
    private UserRepository userRepository;
    private LearningMaterialCreatedEventHandler handler;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        deckRepository = mock(DeckRepository.class);
        userRepository = mock(UserRepository.class);
        handler = new LearningMaterialCreatedEventHandler(deckRepository, userRepository);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // save 시 id를 주입하고 동일 객체를 반환 (실제 JPA save 동작 모사)
        when(deckRepository.save(any(Deck.class))).thenAnswer(inv -> {
            Deck d = inv.getArgument(0);
            if (d.getId() == null) ReflectionTestUtils.setField(d, "id", 999L);
            return d;
        });
    }

    private LearningMaterialCreatedEvent event(String requestedDeckName, boolean force) {
        return new LearningMaterialCreatedEvent(
                1L, 200L, "도메인 주도 설계", 100L, requestedDeckName, force);
    }

    // ─── 해피 ──────────────────────────────────────

    @Test
    @DisplayName("handle_동명없음 — materialName으로 Deck 생성·저장")
    void handle_동명없음_정상_생성() {
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "도메인 주도 설계")).thenReturn(false);

        handler.handle(event(null, false));

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        Deck saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("도메인 주도 설계");
        assertThat(saved.getAxisId()).isEqualTo(100L);
        assertThat(saved.getLearningMaterialId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("handle_requestedDeckName_우선 — materialName 무시")
    void handle_requestedDeckName_우선() {
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "내 Deck")).thenReturn(false);

        handler.handle(event("내 Deck", false));

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("내 Deck");
    }

    @Test
    @DisplayName("handle_requestedDeckName_blank — materialName 폴백")
    void handle_requestedDeckName_blank_폴백() {
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "도메인 주도 설계")).thenReturn(false);

        handler.handle(event("   ", false));

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("도메인 주도 설계");
    }

    // ─── 예외 ──────────────────────────────────────

    @Test
    @DisplayName("handle_동명존재_force_false — DECK_NAME_DUPLICATE throw + 저장 안 함")
    void handle_동명존재_force_false_예외() {
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "도메인 주도 설계")).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(event(null, false)))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.DECK_NAME_DUPLICATE);

        verify(deckRepository, never()).save(any());
    }

    @Test
    @DisplayName("handle_user_미존재 — USER_NOT_FOUND throw")
    void handle_user_미존재_예외() {
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "도메인 주도 설계")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.handle(event(null, false)))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.USER_NOT_FOUND);

        verify(deckRepository, never()).save(any());
    }

    // ─── force=true suffix ──────────────────────────────────────

    @Test
    @DisplayName("handle_동명존재_force_true — suffix (2) 자동 부여")
    void handle_동명존재_force_true_suffix2() {
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "도메인 주도 설계")).thenReturn(true);
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "도메인 주도 설계 (2)")).thenReturn(false);

        handler.handle(event(null, true));

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("도메인 주도 설계 (2)");
    }

    @Test
    @DisplayName("handle_동명존재_force_true_2까지_차있음 — suffix (3) 부여")
    void handle_동명존재_force_true_suffix3() {
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "도메인 주도 설계")).thenReturn(true);
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "도메인 주도 설계 (2)")).thenReturn(true);
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(eq(1L), eq("도메인 주도 설계 (3)"))).thenReturn(false);

        handler.handle(event(null, true));

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("도메인 주도 설계 (3)");
    }

    // ─── axisId null ──────────────────────────────────────

    @Test
    @DisplayName("handle_axisId_null — linkedTopicIds 없는 자료도 Deck 생성")
    void handle_axisId_null_정상() {
        when(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "리소스 미연결")).thenReturn(false);

        LearningMaterialCreatedEvent event = new LearningMaterialCreatedEvent(
                1L, 200L, "리소스 미연결", null, null, false);
        handler.handle(event);

        ArgumentCaptor<Deck> captor = ArgumentCaptor.forClass(Deck.class);
        verify(deckRepository).save(captor.capture());
        Deck saved = captor.getValue();
        assertThat(saved.getAxisId()).isNull();
        assertThat(saved.getLearningMaterialId()).isEqualTo(200L);
    }
}
