package com.example.thirdtool.Deck.presentation.controller;

import com.example.thirdtool.Deck.application.service.DeckCommandService;
import com.example.thirdtool.Deck.application.service.DeckHierarchyService;
import com.example.thirdtool.Deck.presentation.dto.DeckCreateRequestDto;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/decks")
public class DeckCommandController {

    private final DeckCommandService deckCommandService;
    private final DeckHierarchyService deckHierarchyService;

    // ✅ 단일 덱 생성
    @PostMapping
    public ResponseEntity<DeckResponseDto> createDeck(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody DeckCreateRequestDto dto) {

        log.info("[DeckCommandController] 새 덱 생성 요청 - userId={}, name={}", user.getId(), dto.name());
        DeckResponseDto response = deckCommandService.createDeck(user.getId(), dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ 여러 덱 일괄 생성
    @PostMapping("/batch")
    public ResponseEntity<List<DeckResponseDto>> createDecks(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody List<DeckCreateRequestDto> dtos) {

        log.info("[DeckCommandController] 덱 배치 생성 요청 - userId={}, count={}", user.getId(), dtos.size());
        List<DeckResponseDto> responses = deckCommandService.createDecks(user.getId(), dtos);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    // ✅ 덱 수정
    @PutMapping("/{deckId}")
    public ResponseEntity<DeckResponseDto> updateDeck(
            @PathVariable Long deckId,
            @Valid @RequestBody DeckCreateRequestDto dto) {

        log.info("[DeckCommandController] 덱 수정 요청 - deckId={}, name={}", deckId, dto.name());
        return ResponseEntity.ok(deckCommandService.updateDeck(deckId, dto));
    }

    // ✅ 덱 삭제
    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@PathVariable Long deckId) {
        log.info("[DeckCommandController] 덱 삭제 요청 - deckId={}", deckId);
        deckCommandService.deleteDeck(deckId);
        return ResponseEntity.noContent().build();
    }

    // ✅ 덱 이동 (부모 변경)
    @PatchMapping("/{deckId}/parent")
    public ResponseEntity<Void> changeParent(
            @PathVariable Long deckId,
            @RequestParam(required = false) Long newParentId) {

        log.info("[DeckCommandController] 덱 부모 변경 요청 - deckId={}, newParentId={}", deckId, newParentId);
        deckHierarchyService.changeParent(deckId, newParentId);
        return ResponseEntity.noContent().build();
    }
}
