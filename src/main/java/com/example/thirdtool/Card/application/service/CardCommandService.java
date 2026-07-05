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
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
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
    private final CardStatusHistoryAppender cardStatusHistoryAppender;

    // LT-E5-S5-3 (M5) — DeckRepository/DeckQueryService 폐기 · LearningFacadeRepository β 확장 사용
    private final LearningFacadeRepository learningFacadeRepository;

    // Story-CARD-E3-S3-4 — Card.create 팩토리 확장(createdMode 스냅샷)을 위해 UserSchedule BC 의존 추가.
    private final UserScheduleQueryService userScheduleQueryService;

    // ─── 카드 생성 ─────────────────────────────────────

    /**
     * LT-E5-S5-3 (M5) — Deck 폐기 · axisId 직접 참조.
     * request.deckId()는 호환용 파라미터명 · 실제 값은 axisId. Controller 재배선 시 파라미터명 이관 예정.
     */
    public CardResponse.Create create(Long axisId, CardRequest.Create request) {
        LearningAxis axis = learningFacadeRepository.findAxisById(axisId)
                .orElseThrow(() -> CardDomainException.of(
                        ErrorCode.LEARNING_AXIS_NOT_FOUND, "axisId=" + axisId));

        Long userId = learningFacadeRepository.findUserIdByAxisId(axisId)
                .orElseThrow(() -> CardDomainException.of(
                        ErrorCode.LEARNING_AXIS_NOT_FOUND, "axisId=" + axisId));

        MainNote mainNote = MainNote.of(
                request.mainNote().textContent(),
                request.mainNote().imageUrl()
                                       );
        Summary summary = Summary.of(request.summary());

        List<Tag> tags = resolveTags(request.tags());

        // Story-CARD-E3-S3-4 — 카드 생성 시점 사용자 mode를 스냅샷으로 저장.
        LearningMode createdMode = userScheduleQueryService.currentMode(userId);
        LocalDate today = LocalDate.now();

        Card card = Card.create(axisId, mainNote, summary, request.keywords(), tags, createdMode, today);
        cardRepository.save(card);

        // LT-E5-S5-3 (M5) — Deck.markInProgress() → Axis.markInProgress() 이관.
        // markInProgress는 멱등 — 이미 IN_PROGRESS / COMPLETED면 무시. JPA dirty checking으로 자동 save.
        axis.markInProgress();

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
            recalculateAxisProgress(card.getAxisId());
        }
        return CardResponse.Detail.of(card);
    }

    // ─── 카드 ON_FIELD 복귀 ───────────────────────────────
    // Story-CARD-E3-S3-3 — fresh 재시작: createdMode를 사용자 현재 모드로 재기록, enteredFieldAt=today.
    // 멱등: 이미 ON_FIELD면 도메인 no-op + 이력 미생성 + Axis 재계산 미호출.

    public CardResponse.Detail returnToField(Long cardId) {
        Card       card       = findActiveCard(cardId);
        CardStatus fromStatus = card.getStatus();

        // LT-E5-S5-3 (M5) — Deck 폐기 · axis→facade→user 경로.
        Long ownerId = learningFacadeRepository.findUserIdByAxisId(card.getAxisId())
                .orElseThrow(() -> CardDomainException.of(
                        ErrorCode.LEARNING_AXIS_NOT_FOUND, "axisId=" + card.getAxisId()));
        LearningMode userCurrentMode = userScheduleQueryService.currentMode(ownerId);
        card.returnToField(userCurrentMode, LocalDate.now());

        if (fromStatus != card.getStatus()) {
            cardStatusHistoryAppender.append(card, fromStatus, card.getStatus(), null);
            recalculateAxisProgress(card.getAxisId());
        }
        return CardResponse.Detail.of(card);
    }

    // ─── REV E1 · Story 1-4 · 다건 archive orchestration ─
    /**
     * REV E1 · M5 · Story 1-4 — DailyLearningBatch generation 시 exhausted 카드 일괄 archive.
     * Aggregate가 다른 BC를 호출하지 않는 원칙 준수 · Application Service 조율.
     */
    public void archiveMany(List<Long> cardIds, ArchiveReason reason) {
        if (reason == null) {
            throw CardDomainException.of(ErrorCode.INVALID_INPUT, "archiveMany: reason은 null일 수 없습니다.");
        }
        if (cardIds == null || cardIds.isEmpty()) return;

        for (Long cardId : cardIds) {
            Card card = cardRepository.findById(cardId).orElse(null);
            if (card == null || card.isDeleted()) continue;
            CardStatus fromStatus = card.getStatus();
            card.archive();
            if (fromStatus != card.getStatus()) {
                cardStatusHistoryAppender.append(card, fromStatus, card.getStatus(), reason);
                recalculateAxisProgress(card.getAxisId());
            }
        }
    }

    // ─── 13. 카드 삭제 (Soft Delete) ──────────────────────

    public void softDelete(Long cardId) {
        Card card = findActiveCard(cardId);
        Long axisId = card.getAxisId();
        card.softDelete();

        // LT-E5-S5-3 (M5) — Deck 폐기 · Axis progressStatus 재계산.
        recalculateAxisProgress(axisId);
    }

    /**
     * LT-E5-S5-3 (M5) — 축 스코프 progressStatus 재계산 헬퍼.
     * <p>Application이 Card 카운트를 계산하여 axis.recalculateProgressStatus를 호출한다.
     * (Deck 폐기와 함께 Deck.recalculateProgressStatus() 부수효과 이관)
     */
    private void recalculateAxisProgress(Long axisId) {
        LearningAxis axis = learningFacadeRepository.findAxisById(axisId).orElse(null);
        if (axis == null) return;

        long total = cardRepository.countByAxisIdAndDeletedFalse(axisId);
        long archived = cardRepository.countByAxisIdAndStatusAndDeletedFalse(axisId, CardStatus.ARCHIVE);
        int activeCount = (int) (total - archived);
        int archivedCount = (int) archived;
        axis.recalculateProgressStatus(activeCount, archivedCount);
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