package com.example.thirdtool.Library.application.service;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Library.domain.LibraryEntry;
import com.example.thirdtool.Library.domain.LibraryEntryRepository;
import com.example.thirdtool.Library.domain.dto.LibraryNameOnlyResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// library/application/LibraryNameOnlyService.java
@Service
@RequiredArgsConstructor
public class LibraryNameOnlyService {

    private final DeckRepository deckRepository;
    private final LibraryEntryRepository libraryEntryRepository;

    /** 덱 이름만 라이브러리에 등록 */
    @Transactional
    public Long publish(Long userId, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));
        if (!deck.getUser().getId().equals(userId)) throw new BusinessException(ErrorCode.ACCESS_DENIED);

        libraryEntryRepository.findByDeckId(deckId).ifPresent(e -> {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS);
        });

        LibraryEntry entry = LibraryEntry.of(deck);
        return libraryEntryRepository.save(entry).getId();
    }

    /** 등록 취소(라이브러리에서 제거) */
    @Transactional
    public void unpublish(Long userId, Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));
        if (!deck.getUser().getId().equals(userId)) throw new BusinessException(ErrorCode.ACCESS_DENIED);

        LibraryEntry entry = libraryEntryRepository.findByDeckId(deckId)
                                                   .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        libraryEntryRepository.delete(entry);
    }

    /** 공개 피드(이름만) */
    @Transactional(readOnly = true)
    public Page<LibraryNameOnlyResponse> feed(Pageable pageable) {
        return libraryEntryRepository
                .findByPublicVisibleTrueOrderByPublishedAtDesc(pageable)
                .map(e -> new LibraryNameOnlyResponse(
                        e.getId(),
                        e.getDeck().getId(),
                        e.getDeckNameSnapshot(),
                        display(e.getOwner()),
                        e.getPublishedAt()
                ));
    }

    private String display(UserEntity u) {
        return (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname() : u.getUsername();
    }
}

