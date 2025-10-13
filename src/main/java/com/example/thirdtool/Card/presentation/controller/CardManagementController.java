package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardImageService;
import com.example.thirdtool.Card.application.service.CardManagementService;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.presentation.dto.request.MoveCardsRequest;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardManagementController {

    private final CardManagementService cardMgmtService;
    private final CardImageService cardImageService; // 순서 변경 재사용 시

    // 단건 이동
    @PostMapping("/{cardId}:move")
    public ResponseEntity<Void> moveCard(@PathVariable Long cardId,
                                         @RequestParam Long toDeckId) {
        cardMgmtService.moveCard(cardId, toDeckId);
        return ResponseEntity.noContent().build();
    }

    // 단건 복제
    @PostMapping("/{cardId}:copy")
    public ResponseEntity<Map<String, Long>> copyCard(@PathVariable Long cardId,
                                                      @RequestParam Long toDeckId) {
        Long newId = cardMgmtService.copyCard(cardId, toDeckId);
        return ResponseEntity.status(201).body(Map.of("cardId", newId));
    }

    // 일괄 이동
    @PostMapping(":move")
    public ResponseEntity<Map<String, Integer>> moveCards(@RequestBody MoveCardsRequest req) {
        int updated = cardMgmtService.moveCards(req.cardIds(), req.toDeckId());
        return ResponseEntity.ok(Map.of("moved", updated));
    }

    // 검색/필터
    @GetMapping("/search")
    public ResponseEntity<Page<Card>> search(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) Long deckId,
            @RequestParam(required = false) DeckMode mode,
            @RequestParam(required = false) String rankName,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Card> result = cardMgmtService.search(
                user.getId(), deckId, mode, rankName, q, PageRequest.of(page, size, Sort.by("id").ascending()));
        return ResponseEntity.ok(result);
    }
}