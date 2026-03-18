package com.example.thirdtool.Deck.application.service;


import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
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



    @Transactional // ❗️ 여기는 쓰기 작업이므로 @Transactional(readOnly=true)가 아님
    public void touchLastAccessed(Long userId, Long deckId) {
        int updated = deckRepository.touchLastAccessed(userId, deckId, LocalDateTime.now());
        if (updated == 0) {
            // 덱이 없거나, 유저의 덱이 아닌 경우
            throw new IllegalArgumentException("덱이 존재하지 않거나 접근 권한이 없습니다. deckId=" + deckId);
        }
    }
}

