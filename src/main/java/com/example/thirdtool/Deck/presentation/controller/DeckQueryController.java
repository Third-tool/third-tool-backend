package com.example.thirdtool.Deck.presentation.controller;

import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.presentation.dto.DeckRecentResponseDto;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/decks")
public class DeckQueryController {

    private final DeckQueryService deckQueryService;

    // ✅ 상위 덱 조회
    @GetMapping
    public ResponseEntity<List<DeckResponseDto>> getTopLevelDecks(
            @AuthenticationPrincipal UserEntity user) {
        log.info("[DeckQueryController] 상위 덱 조회 요청 - userId={}", user.getId());
        return ResponseEntity.ok(deckQueryService.getTopLevelDecks(user.getId()));
    }

    // ✅ 하위 덱 조회
    @GetMapping("/{deckId}/sub-decks")
    public ResponseEntity<List<DeckResponseDto>> getSubDecks(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long deckId) {
        log.info("[DeckQueryController] 하위 덱 조회 요청 - userId={}, parentDeckId={}", user.getId(), deckId);
        return ResponseEntity.ok(deckQueryService.getSubDecks(user.getId(), deckId));
    }

    // ✅ 최근 덱 조회
    @GetMapping("/recent")
    public ResponseEntity<List<DeckResponseDto>> getRecentDecks(
            @AuthenticationPrincipal UserEntity user) {
        log.info("[DeckQueryController] 최근 덱 조회 요청 - userId={}", user.getId());
        return ResponseEntity.ok(deckQueryService.getRecentDecks(user.getId()));
    }

    // ✅ 가장 최근 덱 1개
    @GetMapping("/recent/top")
    public ResponseEntity<DeckRecentResponseDto> getRecentTop(@AuthenticationPrincipal UserEntity user) {
        return deckQueryService.getMostRecentDeck(user.getId())
                               .map(ResponseEntity::ok)
                               .orElseGet(() -> ResponseEntity.noContent().build());

    }
    // ✅ lastAccessed 즉시 갱신(Continue 진입 시 호출)
    @PostMapping("/{deckId}/touch")
    public ResponseEntity<Void> touch(@AuthenticationPrincipal UserEntity user,
                                      @PathVariable Long deckId) {
        deckQueryService.touchLastAccessed(user.getId(), deckId);
        return ResponseEntity.noContent().build();
    }

}