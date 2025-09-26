package com.example.thirdtool.Deck.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;

import com.example.thirdtool.Deck.presentation.dto.DeckCreateRequestDto;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import com.example.thirdtool.Deck.presentation.dto.DeckSearchDto;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.domain.repository.TagRepository;
import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.domain.repository.UserRepository;

import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final UserRepository userRepository; // ✅ UserRepository 주입
    private final TagRepository tagRepository;

    //최상위 덱 가져오기(parentId 값이 null인)
    //최상위 덱 가져올 때는 업데이트 안하고 -> 그 밑에 있는 카드를 누를 때
    @Transactional(readOnly = true)
    public List<Deck> getTopLevelDecks() {
        return deckRepository.findByParentDeckIsNull();
    }

    //서브 덱들 가져오기
    @Transactional(readOnly = true)
    public List<Deck> getSubDecks(Long parentDeckId) {
        List<Deck> decks = deckRepository.findByParentDeckId(parentDeckId);
        decks.forEach(Deck::updateLastAccessed); // ✅
        return decks;
    }

    // ✅ 덱 생성 로직에 알고리즘 선택 기능 추가
    @Transactional
    public Deck createDeck(DeckCreateRequestDto deckRequestDto) {
        Long userId = 1L; // ✅ 임시로 userId를 1로 가정합니다. 실제 구현 시 수정 필요- 추후 무조건 수정

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("우리 서비스 유저가 아닙니다!"));

        Deck deck;


        // 부모 덱이 없는 경우 (최상위 덱)
        if (deckRequestDto.parentDeckId() == null) {
            deck = Deck.of(deckRequestDto.name(), deckRequestDto.scoringAlgorithmType(), user); // ✅ user 전달
        } else {
            Deck parentDeck = deckRepository.findById(deckRequestDto.parentDeckId())
                .orElseThrow(() -> new IllegalArgumentException("부모 덱이 존재하지 않습니다."));

            // ✅ 부모 덱의 알고리즘 타입을 상속받아 새 덱을 생성
            String inheritedAlgorithmType = parentDeck.getScoringAlgorithmType();
            deck = Deck.of(deckRequestDto.name(), parentDeck, inheritedAlgorithmType, user); // ✅ user 전달

        }

        if (deckRequestDto.tagIds() != null && !deckRequestDto.tagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(deckRequestDto.tagIds());
            deck.setTags(new ArrayList<>(tags));
        }

        return deckRepository.save(deck);
    }

    //최신 덱 가져오기
    @Transactional(readOnly = true)
    public List<Deck> getRecentDecks() {
        // 최근 학습한 덱 로직 (예: lastAccessed가 가장 최근인 덱 5개)
        // Deck 엔티티에 lastAccessed 필드가 필요합니다.
        return deckRepository.findTop5ByOrderByLastAccessedDesc();
    }


    //덱 삭제하기
    @Transactional
    public void deleteDeck(Long deckId) {
        // 특정 덱 ID를 가진 덱을 삭제합니다.
        deckRepository.deleteById(deckId);
    }

    // ✅ 덱을 수정하는 메서드
    @Transactional
    public Deck updateDeck(Long deckId, DeckCreateRequestDto deckRequestDto) {
        // 1. deckId로 기존 덱을 찾습니다.
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        // 2. 덱의 이름을 업데이트합니다.
        deck.updateName(deckRequestDto.name());

        List<Tag> tags = tagRepository.findAllById(deckRequestDto.tagIds()); //태그 업데이트
        deck.setTags(tags);

        // 3. 트랜잭션이 완료되면 변경사항이 자동으로 반영됩니다.
        return deck;
    }

    //덱 이름으로 덱 찾기
    @Transactional(readOnly = true)
    public List<DeckResponseDto> findDecksByName(Long userId, String deckName) {
        List<Deck> decks;
        if (deckName == null || deckName.isEmpty()) {
            decks = deckRepository.findAll();
        } else {
            decks = deckRepository.findAllByUserIdAndDeckName(userId, deckName);
        }

        return decks.stream()
            .map(DeckResponseDto::from)
            .toList();
    }

    //태그로 덱 찾기
    @Transactional(readOnly = true)
    public List<DeckResponseDto> findDecksByTagId(Long userId, List<Long> tagIds) {
        List<Deck> decks;
        if (tagIds == null || tagIds.isEmpty()) {
            decks = deckRepository.findAll();
        } else {
            decks = deckRepository.findAllByUserIdAndTagId(userId,tagIds);
        }

        return decks.stream()
            .map(DeckResponseDto::from)
            .toList();
    }

    //덱 이름 자동완성
    @Transactional(readOnly = true)
    public List<DeckSearchDto> getAutocompleteDeckNames(String keyword,String scope, Long userId){
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        Pageable limit = (Pageable) PageRequest.of(0, 10); // 최대 10개 결과 제한

        if ("public".equalsIgnoreCase(scope)){
            return deckRepository.findPublicDecksByNameStartingWith(keyword, limit);
        }
        else if ("private".equalsIgnoreCase(scope)){
            if (userId == null){
                throw new IllegalArgumentException("User ID가 필요합니다");
            }
            return deckRepository.findUserDecksByNameStartingWith(userId, keyword, limit);
        }
        return List.of();
    }

    //덱 아카이브하기
    @Transactional
    public Deck archiveDeck(Long userId, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 덱입니다"));
        if( !deck.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("본인의 덱만 공유할 수 있습니다");
        }
        deck.setShared();
        return deck;
    }

    //덱 비공개 상태로 전환하기
    @Transactional
    public Deck unArchiveDeck(Long userId, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 덱입니다"));
        if( !deck.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("본인의 덱만 공유할 수 있습니다");
        }
        deck.setUnshared();
        return deck;
    }

    //덱 복사하기
    @Transactional
    public Deck copyDeckToUser(Long deckId,Long newOwnerId) {
        Deck originalDeck = deckRepository.findById(deckId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 덱입니다"));
        User newOwner = userRepository.findById(newOwnerId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다"));

        Deck copiedDeck = Deck.of(originalDeck.getName(), originalDeck.getScoringAlgorithmType(), newOwner);
        copiedDeck.setTags(new ArrayList<>(originalDeck.getTags()));

        List<Card> copiedCards = new ArrayList<>();
        for (Card originalCard : originalDeck.getCards()) {
            Card copiedCard = Card.of(originalCard.getQuestion(), originalCard.getAnswer(), copiedDeck);
            copiedCards.add(copiedCard);
        }
        copiedDeck.setCards(copiedCards);
        return deckRepository.save(copiedDeck);
    }

    //아카이브된 덱들 조회하기
    @Transactional(readOnly = true)
    public List<Deck> getAllArchivedDecks() {
        return deckRepository.findByIsSharedTrue();
    }


}

