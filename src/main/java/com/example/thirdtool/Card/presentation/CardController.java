package com.example.thirdtool.Card.presentation;


import com.example.thirdtool.Card.application.service.CardCommandService;
import com.example.thirdtool.Card.application.service.CardQueryService;
import com.example.thirdtool.Card.presentation.dto.CardRequest;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardCommandService cardCommandService;
    private final CardQueryService   cardQueryService;

    // ─── 1. 카드 생성 ─────────────────────────────────────
    @PostMapping("/api/v1/decks/{deckId}/cards")
    public ResponseEntity<CardResponse.Create> create(
            @PathVariable Long deckId,
            @Valid @RequestBody CardRequest.Create request
                                                     ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cardCommandService.create(deckId, request));
    }

    // ─── 2. 카드 단건 조회 ────────────────────────────────
    @GetMapping("/api/v1/cards/{cardId}")
    public ResponseEntity<CardResponse.Detail> findById(
            @PathVariable Long cardId
                                                       ) {
        return ResponseEntity.ok(cardQueryService.findById(cardId));
    }

    // ─── 3. 덱 내 카드 목록 조회 ─────────────────────────
    @GetMapping("/api/v1/decks/{deckId}/cards")
    public ResponseEntity<List<CardResponse.Summary>> findAllByDeckId(
            @PathVariable Long deckId
                                                                     ) {
        return ResponseEntity.ok(cardQueryService.findAllByDeckId(deckId));
    }

    // ─── 4. MainNote 수정 ─────────────────────────────────
    @PatchMapping("/api/v1/cards/{cardId}/main-note")
    public ResponseEntity<CardResponse.UpdateMainNote> updateMainNote(
            @PathVariable Long cardId,
            @RequestBody CardRequest.UpdateMainNote request
                                                                     ) {
        return ResponseEntity.ok(cardCommandService.updateMainNote(cardId, request));
    }

    // ─── 5. Summary 수정 ──────────────────────────────────
    @PatchMapping("/api/v1/cards/{cardId}/summary")
    public ResponseEntity<CardResponse.UpdateSummary> updateSummary(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequest.UpdateSummary request
                                                                   ) {
        return ResponseEntity.ok(cardCommandService.updateSummary(cardId, request));
    }

    // ─── 6. Keyword 전체 교체 ─────────────────────────────
    @PutMapping("/api/v1/cards/{cardId}/keywords")
    public ResponseEntity<CardResponse.Keywords> replaceKeywords(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequest.ReplaceKeywords request
                                                                ) {
        return ResponseEntity.ok(cardCommandService.replaceKeywords(cardId, request));
    }

    // ─── 7. Keyword 단건 추가 ─────────────────────────────
    @PostMapping("/api/v1/cards/{cardId}/keywords")
    public ResponseEntity<CardResponse.Keywords> addKeyword(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequest.AddKeyword request
                                                           ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cardCommandService.addKeyword(cardId, request));
    }

    // ─── 8. Keyword 단건 제거 ─────────────────────────────
    @DeleteMapping("/api/v1/cards/{cardId}/keywords/{keywordCueId}")
    public ResponseEntity<CardResponse.Keywords> removeKeyword(
            @PathVariable Long cardId,
            @PathVariable Long keywordCueId
                                                              ) {
        return ResponseEntity.ok(cardCommandService.removeKeyword(cardId, keywordCueId));
    }

    // ─── 9. 태그 단건 추가 ────────────────────────────────
    @PostMapping("/api/v1/cards/{cardId}/tags")
    public ResponseEntity<CardResponse.Tags> addTag(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequest.AddTag request
                                                   ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cardCommandService.addTag(cardId, request));
    }

    // ─── 10. 태그 단건 제거 ───────────────────────────────
    @DeleteMapping("/api/v1/cards/{cardId}/tags/{tagId}")
    public ResponseEntity<CardResponse.Tags> removeTag(
            @PathVariable Long cardId,
            @PathVariable Long tagId
                                                      ) {
        return ResponseEntity.ok(cardCommandService.removeTag(cardId, tagId));
    }

    // ─── 11. 태그 전체 교체 ───────────────────────────────
    @PutMapping("/api/v1/cards/{cardId}/tags")
    public ResponseEntity<CardResponse.Tags> replaceTags(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequest.ReplaceTags request
                                                        ) {
        return ResponseEntity.ok(cardCommandService.replaceTags(cardId, request));
    }

    // ─── 12. 관련 카드 후보 조회 ──────────────────────────
    @GetMapping("/api/v1/cards/{cardId}/related")
    public ResponseEntity<List<CardResponse.RelatedCard>> findRelated(
            @PathVariable Long cardId
                                                                     ) {
        return ResponseEntity.ok(cardQueryService.findRelated(cardId));
    }

    // ─── 13. 카드 삭제 (Soft Delete) ──────────────────────
    @DeleteMapping("/api/v1/cards/{cardId}")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long cardId
                                          ) {
        cardCommandService.softDelete(cardId);
        return ResponseEntity.noContent().build();
    }

    // ─── 14. 카드 ARCHIVE 전환 ────────────────────────────
    // product-card.md Epic 1 Story 1-1 — 사용자 명시 보관. 멱등.
    @PostMapping("/api/v1/cards/{cardId}/archive")
    public ResponseEntity<CardResponse.Detail> archive(
            @PathVariable Long cardId,
            @Valid @RequestBody CardRequest.Archive request
                                                      ) {
        return ResponseEntity.ok(cardCommandService.archive(cardId, request.reason()));
    }

    // ─── 15. 카드 ON_FIELD 복귀 ───────────────────────────
    // product-card.md Epic 7 — 새 사이클 시작. 멱등.
    @PostMapping("/api/v1/cards/{cardId}/return-to-field")
    public ResponseEntity<CardResponse.Detail> returnToField(
            @PathVariable Long cardId
                                                            ) {
        return ResponseEntity.ok(cardCommandService.returnToField(cardId));
    }

    // ─── 16. Tag 클릭 탐색 (Story 5-2) ────────────────────
    // 특정 Tag가 부착된 본인 카드를 ON_FIELD/ARCHIVE 모두 포함해 반환.
    // FE가 응답 DTO의 status 필드로 ON_FIELD / ARCHIVE 섹션 분리.
    @GetMapping("/api/v1/tags/{tagId}/cards")
    public ResponseEntity<List<CardResponse.Summary>> findCardsByTag(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long tagId
                                                                    ) {
        return ResponseEntity.ok(cardQueryService.findByTag(tagId, user.getId()));
    }
}