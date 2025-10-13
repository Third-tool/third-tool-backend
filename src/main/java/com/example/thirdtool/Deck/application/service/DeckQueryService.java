package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    // ✅ 유저별 최근 접근 덱 5개
    public List<DeckResponseDto> getRecentDecks(Long userId) {
        List<Deck> decks = deckRepository.findTop5ByUserIdOrderByLastAccessedDesc(userId);
        log.info("[DeckQueryService] 최근 덱 조회 - userId={}, count={}", userId, decks.size());

        return decks.stream()
                    .map(DeckResponseDto::from)
                    .toList();
    }
}

