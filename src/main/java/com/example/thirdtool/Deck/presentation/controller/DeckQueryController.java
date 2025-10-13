package com.example.thirdtool.Deck.presentation.controller;

import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}