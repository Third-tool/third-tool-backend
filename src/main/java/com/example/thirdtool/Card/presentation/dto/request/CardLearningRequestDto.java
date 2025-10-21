package com.example.thirdtool.Card.presentation.dto.request;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.Optional;

@Getter
@Builder(access = AccessLevel.PRIVATE) // 외부에서 직접 빌더 호출 방지
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardLearningRequestDto {

    private final Long userId;
    private final Long deckId;
    private final DeckMode mode;
    private final Optional<String> rankName; // ✅ Optional로 변경
    private final Optional<Long> cardId;     // ✅ null 안전하게 처리

    /** ✅ cardId 없는 기본 요청용 (예: 덱 단위 요청) */
    public static CardLearningRequestDto of(Long userId, Long deckId, DeckMode mode, String rankName) {
        return CardLearningRequestDto.builder()
                                     .userId(userId)
                                     .deckId(deckId)
                                     .mode(mode)
                                     .rankName(Optional.ofNullable(rankName)) // ✅ null 안전 처리
                                     .cardId(Optional.empty())
                                     .build();
    }

    /** ✅ cardId 포함한 요청용 (예: 카드 학습 페이지 진입) */
    public static CardLearningRequestDto of(Long userId, Long deckId, Long cardId, DeckMode mode, String rankName) {
        return CardLearningRequestDto.builder()
                                     .userId(userId)
                                     .deckId(deckId)
                                     .mode(mode)
                                     .rankName(Optional.ofNullable(rankName)) // ✅ null 안전 처리
                                     .cardId(Optional.ofNullable(cardId))
                                     .build();
    }
}