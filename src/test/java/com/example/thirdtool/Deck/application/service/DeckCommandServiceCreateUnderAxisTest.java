package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * DeckCommandService.createUnderAxis 단위 테스트 (Fix-Story 2).
 *
 * Mockist 전략 — Repository Mock + 도메인 + 응답 DTO 매핑은 실제 객체로 검증.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeckCommandService — createUnderAxis (Fix-Story 2)")
class DeckCommandServiceCreateUnderAxisTest {

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
    @DisplayName("정상 생성 — axisId/axisName 응답 동봉, 중복 검사 1회 + save 1회")
    void 정상_생성() {
        given(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "백엔드 학습")).willReturn(false);
        given(deckRepository.save(any(Deck.class))).willAnswer(inv -> inv.getArgument(0));

        DeckResponse.Create response = sut.createUnderAxis(user, 100L, "백엔드 학습", "백엔드");

        assertThat(response.axisId()).isEqualTo(100L);
        assertThat(response.axisName()).isEqualTo("백엔드");
        assertThat(response.name()).isEqualTo("백엔드 학습");
        assertThat(response.parentDeckId()).isNull();
        assertThat(response.depth()).isZero();
        verify(deckRepository).save(any(Deck.class));
    }

    @Test
    @DisplayName("name trim 후 중복 검사 — 앞뒤 공백 입력이 trim된 값으로 검사됨")
    void name_trim_후_중복검사() {
        given(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "백엔드 학습")).willReturn(false);
        given(deckRepository.save(any(Deck.class))).willAnswer(inv -> inv.getArgument(0));

        DeckResponse.Create response = sut.createUnderAxis(user, 100L, "  백엔드 학습  ", "백엔드");

        assertThat(response.name()).isEqualTo("백엔드 학습");
        verify(deckRepository).existsByUserIdAndNameAndDeletedFalse(1L, "백엔드 학습");
    }

    @Test
    @DisplayName("동일 이름 중복 — DECK_NAME_DUPLICATE 예외, save 호출 안 함")
    void 동일이름_중복_예외() {
        given(deckRepository.existsByUserIdAndNameAndDeletedFalse(1L, "백엔드 학습")).willReturn(true);

        assertThatThrownBy(() -> sut.createUnderAxis(user, 100L, "백엔드 학습", "백엔드"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.DECK_NAME_DUPLICATE);

        verify(deckRepository, never()).save(any(Deck.class));
    }

    @Test
    @DisplayName("name blank — 중복 검사 우회, 도메인이 DECK_NAME_BLANK 예외")
    void name_blank_도메인예외() {
        assertThatThrownBy(() -> sut.createUnderAxis(user, 100L, "   ", "백엔드"))
                .isInstanceOf(BusinessException.class)
                .matches(e -> ((BusinessException) e).getErrorCode() == ErrorCode.DECK_NAME_BLANK);

        verify(deckRepository, never()).save(any(Deck.class));
    }
}
