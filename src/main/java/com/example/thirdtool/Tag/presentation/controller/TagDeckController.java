package com.example.thirdtool.Tag.presentation.controller;

import com.example.thirdtool.Deck.application.service.DeckService;
import com.example.thirdtool.Deck.presentation.dto.DeckResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/tags")
@RestController
@RequiredArgsConstructor
public class TagDeckController {
    private final DeckService deckService;

    @GetMapping("/{tagId}/decks")
    public ResponseEntity<List<DeckResponseDto>> getDecksByTag() {
        Long userId = 1L; // TODO: 임시 사용자, 나중에 JWT/리졸버로 대체
        Long tagId = 1L; // TODO: 임시 태그, 나중에 PathVariable로 대체
        ResponseEntity<List<DeckResponseDto>> decks = (ResponseEntity<List<DeckResponseDto>>) deckService.findDecksByTagId(userId, tagId);
        return ResponseEntity.ok(decks.getBody());
    }


}
