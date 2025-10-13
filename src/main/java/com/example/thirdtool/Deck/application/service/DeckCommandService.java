package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Deck.presentation.dto.DeckCreateRequestDto;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
@Transactional
public class DeckCommandService {

    private final DeckRepository deckRepository;
    private final UserRepository userRepository;


    /**
     * ✅ 덱 생성 (루트 or 하위 덱 자동 분기)
     */
    public DeckResponseDto createDeck(Long userId, DeckCreateRequestDto dto) {
        log.debug("[DeckService] 덱 생성 시작 - userId={}, dto={}", userId, dto);

        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (dto.parentDeckId() == null) {
            return createRootDeck(dto, user);
        } else {
            return createChildDeck(dto, user);
        }
    }

    /**
     * ✅ 루트 덱 생성
     * - 알고리즘 필수
     * - 부모 없음
     */
    private DeckResponseDto createRootDeck(DeckCreateRequestDto dto, UserEntity user) {
        if (dto.scoringAlgorithmType() == null || dto.scoringAlgorithmType().isBlank()) {
            throw new BusinessException(ErrorCode.DECK_ALGORITHM_REQUIRED);
        }

        Deck rootDeck = Deck.of(dto.name(), null, dto.scoringAlgorithmType(), user);
        deckRepository.save(rootDeck);

        log.info("[DeckService] 루트 덱 생성 완료 - id={}, name={}", rootDeck.getId(), rootDeck.getName());
        return DeckResponseDto.from(rootDeck);
    }

    /**
     * ✅ 하위 덱 생성
     * - 부모 덱 필수
     * - 알고리즘 직접 지정 불가 (부모 덱 상속)
     */
    private DeckResponseDto createChildDeck(DeckCreateRequestDto dto, UserEntity user) {
        Deck parentDeck = deckRepository.findById(dto.parentDeckId())
                                        .orElseThrow(() -> new BusinessException(ErrorCode.DECK_PARENT_NOT_FOUND));

        if (dto.scoringAlgorithmType() != null && !dto.scoringAlgorithmType().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String inheritedAlgorithm = parentDeck.getScoringAlgorithmType();
        Deck childDeck = Deck.of(dto.name(), parentDeck, inheritedAlgorithm, user);
        deckRepository.save(childDeck);

        log.info("[DeckService] 하위 덱 생성 완료 - parentId={}, name={}", parentDeck.getId(), childDeck.getName());
        return DeckResponseDto.from(childDeck);
    }

    // ✅ 여러 개 덱 생성 (batch)
    public List<DeckResponseDto> createDecks(Long userId, List<DeckCreateRequestDto> dtos) {
        return dtos.stream()
                   .map(dto -> createDeck(userId, dto))
                   .toList();
    }

    // ✅ 덱 수정
    @Transactional
    public DeckResponseDto updateDeck(Long deckId, DeckCreateRequestDto dto) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        deck.updateName(dto.name());

        return DeckResponseDto.from(deck);// 트랜잭션 종료 시 자동 반영
    }

    // ✅ 덱 삭제
    @Transactional
    public void deleteDeck(Long deckId) {
        if (!deckRepository.existsById(deckId)) {
            throw new BusinessException(ErrorCode.DECK_NOT_FOUND);
        }
        deckRepository.deleteById(deckId);
    }
}
