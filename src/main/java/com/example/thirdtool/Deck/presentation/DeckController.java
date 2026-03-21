package com.example.thirdtool.Deck.presentation;

import com.example.thirdtool.Deck.application.service.DeckCommandService;
import com.example.thirdtool.Deck.application.service.DeckQueryService;
import com.example.thirdtool.Deck.presentation.dto.DeckRequest;
import com.example.thirdtool.Deck.presentation.dto.DeckResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/decks")
@RequiredArgsConstructor
public class DeckController {

    private final DeckCommandService deckCommandService;
    private final DeckQueryService deckQueryService;

    // ─── 1. 덱 생성 ──────────────────────────────────────
    @PostMapping
    public ResponseEntity<DeckResponse.Create> create(
            @Valid @RequestBody DeckRequest.Create request,
            @AuthenticationPrincipal UserEntity user
                                                     ) {
        DeckResponse.Create response = deckCommandService.create(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── 2. 덱 단건 조회 ─────────────────────────────────
    @GetMapping("/{deckId}")
    public ResponseEntity<DeckResponse.Detail> findById(
            @PathVariable Long deckId
                                                       ) {
        return ResponseEntity.ok(deckQueryService.findById(deckId));
    }

    // ─── 3. 내 루트 덱 목록 조회 ─────────────────────────
    @GetMapping
    public ResponseEntity<DeckResponse.Page> findRootDecks(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
                                                          ) {
        return ResponseEntity.ok(
                deckQueryService.findRootDecks(user.getId(), PageRequest.of(page, size))
                                );
    }

    // ─── 4. 하위 덱 목록 조회 ────────────────────────────
    @GetMapping("/{deckId}/sub-decks")
    public ResponseEntity<DeckResponse.SubDeckList> findSubDecks(
            @PathVariable Long deckId
                                                                ) {
        return ResponseEntity.ok(deckQueryService.findSubDecks(deckId));
    }

    // ─── 5. 덱 이름 수정 ─────────────────────────────────
    @PatchMapping("/{deckId}/name")
    public ResponseEntity<DeckResponse.UpdateName> updateName(
            @PathVariable Long deckId,
            @Valid @RequestBody DeckRequest.UpdateName request
                                                             ) {
        return ResponseEntity.ok(deckCommandService.updateName(deckId, request));
    }

    // ─── 6. 부모 덱 변경 ─────────────────────────────────
    @PatchMapping("/{deckId}/parent")
    public ResponseEntity<DeckResponse.ChangeParent> changeParent(
            @PathVariable Long deckId,
            @RequestBody DeckRequest.ChangeParent request
                                                                 ) {
        return ResponseEntity.ok(deckCommandService.changeParent(deckId, request));
    }

    // ─── 7. 덱 삭제 (Soft Delete) ────────────────────────
    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long deckId
                                      ) {
        deckCommandService.delete(deckId);
        return ResponseEntity.noContent().build();
    }

    // ─── 8. 최근 접근 시각 갱신 ──────────────────────────
    @PatchMapping("/{deckId}/last-accessed")
    public ResponseEntity<DeckResponse.LastAccessed> updateLastAccessed(
            @PathVariable Long deckId
                                                                       ) {
        return ResponseEntity.ok(deckCommandService.updateLastAccessed(deckId));
    }
}