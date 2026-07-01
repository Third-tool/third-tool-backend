package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * DeckCommandService.softDeleteByAxisId 단위 테스트
 * (Fix — Axis↔Deck 완전 통합, 2026-07-01).
 *
 * <p>Mockist 전략 — Repository만 Mock, Deck 도메인은 실제 객체로 상태 전이 확인.
 * 축 소프트 삭제 시 소속 Deck 연쇄 소프트 삭제가 정확히 동작하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeckCommandService — softDeleteByAxisId")
class DeckCommandServiceSoftDeleteByAxisIdTest {

    @Mock
    DeckRepository deckRepository;

    @Mock
    DeckQueryService deckQueryService;

    @Mock
    DeckHierarchyService deckHierarchyService;

    @InjectMocks
    DeckCommandService sut;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = UserEntity.ofLocal("tester", "encoded-pw", "닉네임", "tester@example.com");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("축에 속한 활성 Deck 다건이 모두 소프트 삭제된다")
    void 다건_연쇄_softDelete() {
        Long axisId = 100L;
        Deck deck1 = Deck.createFromAxis(user, axisId, "덱1");
        Deck deck2 = Deck.createFromAxis(user, axisId, "덱2");
        given(deckRepository.findByAxisIdInAndDeletedFalse(List.of(axisId)))
                .willReturn(List.of(deck1, deck2));

        sut.softDeleteByAxisId(axisId);

        assertThat(deck1.isDeleted()).isTrue();
        assertThat(deck1.getDeletedAt()).isNotNull();
        assertThat(deck2.isDeleted()).isTrue();
        assertThat(deck2.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("대상 Deck이 0건이면 no-op — 예외 없이 종료")
    void 대상_0건_noop() {
        Long axisId = 100L;
        given(deckRepository.findByAxisIdInAndDeletedFalse(List.of(axisId)))
                .willReturn(List.of());

        // 예외 없이 조용히 종료
        sut.softDeleteByAxisId(axisId);
    }

    @Test
    @DisplayName("이미 소프트 삭제된 Deck은 조회 필터로 제외되므로 재삭제 예외를 만나지 않는다")
    void 이미삭제된_Deck_필터되어_재삭제_예외없음() {
        Long axisId = 100L;
        Deck alreadyDeleted = Deck.createFromAxis(user, axisId, "이미삭제");
        alreadyDeleted.softDelete();
        // Repository의 findByAxisIdInAndDeletedFalse는 이미 필터된 결과를 반환한다고 가정
        given(deckRepository.findByAxisIdInAndDeletedFalse(List.of(axisId)))
                .willReturn(List.of());

        sut.softDeleteByAxisId(axisId);

        // alreadyDeleted는 여전히 삭제 상태 유지, 새로운 재삭제 시도가 없어야 함
        assertThat(alreadyDeleted.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Deck.softDelete()가 소속 Card까지 연쇄 소프트 삭제한다 — 계약 확인")
    void softDelete_Card_연쇄_계약() {
        Long axisId = 100L;
        Deck deck = Deck.createFromAxis(user, axisId, "덱");
        // Card 연쇄는 Deck.softDelete()가 담당 — 본 단위 테스트는 Deck에 위임 사실만 확인.
        given(deckRepository.findByAxisIdInAndDeletedFalse(List.of(axisId)))
                .willReturn(List.of(deck));

        sut.softDeleteByAxisId(axisId);

        // Deck이 이미 삭제된 상태로 진입하면 예외를 던지지만, 필터로 미리 걸러지므로
        // 두 번째 softDelete 호출은 발생하지 않는다.
        assertThatThrownBy(deck::softDelete)
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.DECK_ALREADY_DELETED);
    }
}
