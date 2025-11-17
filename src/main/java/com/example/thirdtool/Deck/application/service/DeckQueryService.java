package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Card.domain.repository.CardImageRepository;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckRecentResponseDto;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional(readOnly = true)
public class DeckQueryService {

    private final DeckRepository deckRepository;
    private final CardImageRepository cardImageRepository;

    // ✅ 유저별 최상위 덱 가져오기
    public List<DeckResponseDto> getTopLevelDecks(Long userId) {
        List<Deck> decks = deckRepository.findByUserIdAndParentDeckIsNull(userId);
        return decks.stream()
                    .map(DeckResponseDto::from)
                    .toList();
    }

    // ✅ 유저별 서브덱 가져오기
    public List<DeckResponseDto> getSubDecks(Long  userId, Long parentDeckId) {
        List<Deck> subDecks = deckRepository.findByUserIdAndParentDeckId(userId, parentDeckId);
        subDecks.forEach(Deck::updateLastAccessed);

        log.info("[DeckQueryService] 하위 덱 조회 - userId={}, parentDeckId={}, count={}",
                userId, parentDeckId, subDecks.size());

        return subDecks.stream()
                       .map(DeckResponseDto::from)
                       .toList();
    }


    public List<DeckResponseDto> getRecentDecks(Long userId) {
        List<Deck> decks = deckRepository.findTop5ByUserIdOrderByLastAccessedDesc(userId);
        log.info("[DeckQueryService] 최근 덱 조회 - userId={}, count={}", userId, decks.size());

        return decks.stream()
                    .map(DeckResponseDto::from)
                    .toList();
    }


    public Optional<DeckRecentResponseDto> getMostRecentDeck(Long userId) {
        Optional<Deck> recentDeckOpt = deckRepository.findFirstByUserIdOrderByLastAccessedDesc(userId);

        if (recentDeckOpt.isEmpty()) {
            return Optional.empty(); // 덱이 없으면 빈 Optional 반환
        }

        Deck recentDeck = recentDeckOpt.get();

        // 2. ✅ 해당 덱의 썸네일 조회 (정책: 덱의 카드 이미지 중 첫 번째 1개)
        String thumbnailUrl = cardImageRepository.findFirstByCardDeckId(recentDeck.getId())
                                                 .map(cardImage -> cardImage.getImageUrl()) // CardImage 객체에서 URL 추출
                                                 .orElse(null); // 이미지가 없으면 null

        // 3. ✅ 덱 정보와 썸네일 URL을 'DeckRecentResponseDto'에 담아 반환
        return Optional.of(DeckRecentResponseDto.from(recentDeck, thumbnailUrl));
    }

    @Transactional // ❗️ 여기는 쓰기 작업이므로 @Transactional(readOnly=true)가 아님
    public void touchLastAccessed(Long userId, Long deckId) {
        int updated = deckRepository.touchLastAccessed(userId, deckId, LocalDateTime.now());
        if (updated == 0) {
            // 덱이 없거나, 유저의 덱이 아닌 경우
            throw new IllegalArgumentException("덱이 존재하지 않거나 접근 권한이 없습니다. deckId=" + deckId);
        }
    }
}

