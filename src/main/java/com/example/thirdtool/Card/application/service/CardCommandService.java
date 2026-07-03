package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Card.domain.model.ArchiveReason;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.CardStatusHistoryAppender;
import com.example.thirdtool.Card.domain.model.MainNote;
import com.example.thirdtool.Card.domain.model.Summary;
import com.example.thirdtool.Card.domain.model.Tag;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Card.infrastructure.persistence.TagRepository;
import com.example.thirdtool.Card.presentation.dto.CardRequest;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.infrastructure.repository.DeckRepository;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CardCommandService {

    private final CardRepository cardRepository;
    private final TagRepository tagRepository;
    private final DeckRepository deckRepository;
    private final CardStatusHistoryAppender cardStatusHistoryAppender;

    // Story-CARD-E3-S3-4 — Card.create 팩토리 확장(createdMode 스냅샷)을 위해 UserSchedule BC 의존 추가.
    private final UserScheduleQueryService userScheduleQueryService;

    // ─── 카드 생성 ─────────────────────────────────────

    public CardResponse.Create create(Long deckId, CardRequest.Create request) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> CardDomainException.of(
                                          ErrorCode.DECK_NOT_FOUND, "deckId=" + deckId));

        MainNote mainNote = MainNote.of(
                request.mainNote().textContent(),
                request.mainNote().imageUrl()
                                       );
        Summary summary = Summary.of(request.summary());

        List<Tag> tags = resolveTags(request.tags());

        // Story-CARD-E3-S3-4 — 카드 생성 시점 사용자 mode를 스냅샷으로 저장.
        // 미보유 유저는 currentMode 내부에서 default MODE_14D로 lazy 초기화.
        LearningMode createdMode = userScheduleQueryService.currentMode(deck.getUser().getId());
        LocalDate today = LocalDate.now();

        Card card = Card.create(deck, mainNote, summary, request.keywords(), tags, createdMode, today);
        cardRepository.save(card);

        // Story-005-2: 첫 Card 추가 시 Deck progressStatus를 NOT_STARTED → IN_PROGRESS로 자동 전환.
        // markInProgress는 멱등 — 이미 IN_PROGRESS / COMPLETED면 무시. JPA dirty checking으로 자동 save.
        deck.markInProgress();

        return CardResponse.Create.of(card);
    }

    // ─── MainNote 수정 ─────────────────────────────────

    public CardResponse.UpdateMainNote updateMainNote(Long cardId, CardRequest.UpdateMainNote request) {
        Card card = findActiveCard(cardId);
        card.changeMainNote(request.textContent(), request.imageUrl());
        return CardResponse.UpdateMainNote.of(card);
    }

    // ─── Summary 수정 ──────────────────────────────────

    public CardResponse.UpdateSummary updateSummary(Long cardId, CardRequest.UpdateSummary request) {
        Card card = findActiveCard(cardId);
        card.changeSummary(request.summary());
        return CardResponse.UpdateSummary.of(card);
    }

    // ─── Keyword 전체 교체 ─────────────────────────────

    public CardResponse.Keywords replaceKeywords(Long cardId, CardRequest.ReplaceKeywords request) {
        Card card = findActiveCard(cardId);
        card.replaceKeywords(request.keywords());
        return CardResponse.Keywords.of(card);
    }

    // ───  Keyword 단건 추가 ─────────────────────────────

    public CardResponse.Keywords addKeyword(Long cardId, CardRequest.AddKeyword request) {
        Card card = findActiveCard(cardId);
        card.addKeyword(request.value());
        return CardResponse.Keywords.of(card);
    }

    // ─── Keyword 단건 제거 ─────────────────────────────

    public CardResponse.Keywords removeKeyword(Long cardId, Long keywordCueId) {
        Card card = findActiveCard(cardId);
        card.removeKeyword(keywordCueId);
        return CardResponse.Keywords.of(card);
    }

    // ─── 태그 단건 추가 ────────────────────────────────

    public CardResponse.Tags addTag(Long cardId, CardRequest.AddTag request) {
        Card card = findActiveCard(cardId);
        Tag  tag  = findOrCreateTag(request.value());
        card.addTag(tag);
        return CardResponse.Tags.of(card);
    }

    // ─── 태그 단건 제거 ───────────────────────────────

    public CardResponse.Tags removeTag(Long cardId, Long tagId) {
        Card card = findActiveCard(cardId);
        card.removeTag(tagId);
        return CardResponse.Tags.of(card);
    }

    // ─── 태그 전체 교체 ───────────────────────────────

    public CardResponse.Tags replaceTags(Long cardId, CardRequest.ReplaceTags request) {
        Card       card    = findActiveCard(cardId);
        List<Tag>  newTags = resolveTags(request.tags());
        card.replaceTags(newTags);
        return CardResponse.Tags.of(card);
    }

    // ─── 카드 ARCHIVE 전환 ────────────────────────────────
    // product-card.md Epic 1 Story 1-1 — 사용자가 카드를 "보관"하는 운영 위치 전환.
    // 멱등: 이미 ARCHIVE면 도메인 no-op + 이력 미생성 + Deck 재계산 미호출.

    public CardResponse.Detail archive(Long cardId, ArchiveReason reason) {
        if (reason == null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT, "archive: reason은 null일 수 없습니다.");
        }
        Card       card       = findActiveCard(cardId);
        CardStatus fromStatus = card.getStatus();

        card.archive();

        if (fromStatus != card.getStatus()) {
            cardStatusHistoryAppender.append(card, fromStatus, card.getStatus(), reason);
            card.getDeck().recalculateProgressStatus();
        }
        return CardResponse.Detail.of(card);
    }

    // ─── 카드 ON_FIELD 복귀 ───────────────────────────────
    // Story-CARD-E3-S3-3 — fresh 재시작: createdMode를 사용자 현재 모드로 재기록, enteredFieldAt=today.
    // 멱등: 이미 ON_FIELD면 도메인 no-op + 이력 미생성 + Deck 재계산 미호출.

    public CardResponse.Detail returnToField(Long cardId) {
        Card       card       = findActiveCard(cardId);
        CardStatus fromStatus = card.getStatus();

        LearningMode userCurrentMode = userScheduleQueryService.currentMode(card.getDeck().getUser().getId());
        card.returnToField(userCurrentMode, LocalDate.now());

        if (fromStatus != card.getStatus()) {
            cardStatusHistoryAppender.append(card, fromStatus, card.getStatus(), null);
            card.getDeck().recalculateProgressStatus();
        }
        return CardResponse.Detail.of(card);
    }

    // ─── 13. 카드 삭제 (Soft Delete) ──────────────────────

    public void softDelete(Long cardId) {
        Card card = findActiveCard(cardId);
        card.softDelete();

        // Story-005-2: 카드 soft delete 후 Deck progressStatus 자동 재계산.
        // 모든 활성 Card가 ARCHIVE면 COMPLETED, 0개면 NOT_STARTED로 회귀.
        card.getDeck().recalculateProgressStatus();
    }

    // ─── 내부 유틸 ────────────────────────────────────────

    private Card findActiveCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> CardDomainException.of(
                                          ErrorCode.CARD_NOT_FOUND, "cardId=" + cardId));
        if (card.isDeleted()) {
            throw CardDomainException.of(ErrorCode.CARD_NOT_FOUND, "cardId=" + cardId);
        }
        return card;
    }

    private List<Tag> resolveTags(List<String> tagValues) {
        if (tagValues == null || tagValues.isEmpty()) return Collections.emptyList();
        return tagValues.stream()
                        .map(this::findOrCreateTag)
                        .toList();
    }

    private Tag findOrCreateTag(String value) {
        return tagRepository.findByValue(value.trim())
                            .orElseGet(() -> tagRepository.save(Tag.of(value)));
    }

}