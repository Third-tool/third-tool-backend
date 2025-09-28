package com.example.thirdtool.Deck.presentation.controller;

import com.example.thirdtool.Deck.application.service.DeckService;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.presentation.dto.DeckCreateRequestDto;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import com.example.thirdtool.Deck.presentation.dto.DeckSearchDto;
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

    //덱 이름으로 덱 조회
    @GetMapping("/search-by-Deckname") //덱 이름으로 덱 조회하기
    public ResponseEntity<List<DeckResponseDto>> searchByName(Long userId,@RequestParam(required = false) String name) {
        //userId는 나중에 jwt로 받아올 예정 @AuthenticationPrincipal
        List<DeckResponseDto> decks = deckService.findDecksByName(userId, name);
        return ResponseEntity.ok(decks);
    }

    //태그 id로 덱 조회
    @GetMapping("/my-decks/search-by-tags") //태그 id로 덱 조회
    public ResponseEntity<List<DeckResponseDto>> getDecksByTag(@RequestParam(required = false) List<Long> tagIds) {
        Long userId = 1L; // TODO: 임시 사용자, 나중에 JWT/리졸버로 대체
        List<DeckResponseDto> decks = deckService.findDecksByTagId(userId, tagIds);
        return ResponseEntity.ok(decks);
    }

    //덱 이름 자동완성
    @GetMapping("/auto-complete")
    public ResponseEntity<List<DeckSearchDto>> autoCompleteDeckNames(@RequestParam String keyword, @RequestParam(defaultValue = "public") String scope){
        Long userId = 1L;
        List<DeckSearchDto> suggestions = deckService.getAutocompleteDeckNames(keyword, scope, userId);
        return ResponseEntity.ok(suggestions);
    }

    //덱 공유하기(공개하기)
    @PostMapping("/{deckId}/archive")
    public ResponseEntity<DeckResponseDto> archiveDeck(@PathVariable Long deckId) {
        Long userId = 1L; // TODO: 임시 사용자, 나중에 JWT/리졸버로 대체
        Deck archivedDeck = deckService.archiveDeck(userId, deckId);
        return ResponseEntity.ok(DeckResponseDto.from(archivedDeck));
    }

    //덱 비공개로 전환하기
    @PostMapping("/{deckId}/unarchive")
    public ResponseEntity<DeckResponseDto> unArchiveDeck(@PathVariable Long deckId) {
        Long userId = 1L; // TODO: 임시 사용자, 나중에 JWT/리졸버로 대체
        Deck unArchivedDeck = deckService.unArchiveDeck(userId, deckId);
        return ResponseEntity.ok(DeckResponseDto.from(unArchivedDeck));
    }

    //공개된 덱들 조회하기
    @GetMapping("/public")
    public ResponseEntity<List<DeckResponseDto>> getPublicDecks() {
        List<Deck> publicDecks = deckService.getAllArchivedDecks();
        List<DeckResponseDto> responseDtos = publicDecks.stream()
                .map(DeckResponseDto::from)
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    //덱 복사하기
    @PostMapping("/{deckId}/copy")
    public ResponseEntity<DeckResponseDto> copyDeck(@PathVariable Long deckId){
        Long userId = 1L; // TODO: 임시 사용자, 나중에 JWT/리졸버로 대체
        Deck copiedDeck = deckService.copyDeckToUser(userId, deckId);
        DeckResponseDto responseDtos = DeckResponseDto.from(copiedDeck);
        return ResponseEntity.ok(responseDtos);
    }

}
