package com.example.thirdtool.Card.presentation.controller;


import com.example.thirdtool.Card.Document.CardDocument;
import com.example.thirdtool.Card.application.service.CardSearchService;
import com.example.thirdtool.Card.presentation.dto.CardSearchResponseDto;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/search/cards")
@RequiredArgsConstructor
public class CardSearchController {

    private final CardSearchService cardSearchService;

    /**
     * ✅ 단순 키워드 검색 (contains 기반)
     * 예: GET /api/search/cards/simple?keyword=자바
     */
    @GetMapping("/simple")
    public ResponseEntity<List<CardDocument>> simpleSearch(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam String keyword
                                                          ) {
        log.info("[CardSearchController] Simple search requested - userId={}, keyword={}", user.getId(), keyword);

        Long userId = user.getId();
        List<CardDocument> results = cardSearchService.simpleSearch(userId, keyword);
        return ResponseEntity.ok(results);
    }


    @GetMapping("/search")
    public ResponseEntity<Slice<CardSearchResponseDto>> searchCards(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
                                                                   ) {
        Long userId = user.getId();
        log.info("[CardSearchController] 검색 요청 - userId={}, keyword={}", user.getId(), keyword);
        Slice<CardSearchResponseDto> result = cardSearchService.searchCards(userId,keyword, page, size);
        return ResponseEntity.ok(result);
    }
}