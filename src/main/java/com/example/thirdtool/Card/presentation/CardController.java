package com.example.thirdtool.Card.presentation;


import com.example.thirdtool.Card.application.service.CardCommandService;
import com.example.thirdtool.Card.application.service.CardQueryService;
import com.example.thirdtool.Card.presentation.dto.CardRequest;
import com.example.thirdtool.Card.presentation.dto.CardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardCommandService cardCommandService;
    private final CardQueryService cardQueryService;

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

    // ─── 9. 카드 삭제 (Soft Delete) ───────────────────────
    @DeleteMapping("/api/v1/cards/{cardId}")
    public ResponseEntity<Void> softDelete(
            @PathVariable Long cardId
                                          ) {
        cardCommandService.softDelete(cardId);
        return ResponseEntity.noContent().build();
    }
}