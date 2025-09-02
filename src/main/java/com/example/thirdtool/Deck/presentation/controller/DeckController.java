package com.example.thirdtool.Deck.presentation.controller;

import com.example.thirdtool.Deck.application.service.DeckService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.presentation.dto.DeckCreateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("api/decks")
public class DeckController {

    private final DeckService deckService;

    //카 생성 ㅇㅇ
    //확인 완료
    @PostMapping
    public ResponseEntity<Deck> createDeck(@RequestBody DeckCreateRequestDto deckRequestDto) {
        Deck newDeck = deckService.createDeck(deckRequestDto);
        return ResponseEntity.ok(newDeck);
    }

    // parentId가 null인 최상위 덱들을 조회하는 API
    //확인 완료
    @GetMapping
    public ResponseEntity<List<Deck>> getTopLevelDecks() {
        List<Deck> topLevelDecks = deckService.getTopLevelDecks();
        return ResponseEntity.ok(topLevelDecks);
    }
    //
    //확인 완료
    @GetMapping("/{deckId}/sub-decks")
    public ResponseEntity<List<Deck>> getSubDecks(@PathVariable Long deckId) {
        List<Deck> subDecks = deckService.getSubDecks(deckId);
        return ResponseEntity.ok(subDecks);
    }

    //최신덱 가져오기
    @GetMapping("/recent")
    public ResponseEntity<List<Deck>> getRecentDecks() {
        List<Deck> recentDecks = deckService.getRecentDecks();

        return ResponseEntity.ok(recentDecks);
    }


    //카드 삭제
    //확인 완료
    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@PathVariable Long deckId) {
        deckService.deleteDeck(deckId);
        return ResponseEntity.noContent().build();
    }

    //확인 완료
    @PutMapping("/{deckId}")
    public ResponseEntity<Deck> updateDeck(@PathVariable Long deckId, @RequestBody DeckCreateRequestDto deckRequestDto) {
        Deck updatedDeck = deckService.updateDeck(deckId, deckRequestDto);
        return ResponseEntity.ok(updatedDeck);
    }


}
