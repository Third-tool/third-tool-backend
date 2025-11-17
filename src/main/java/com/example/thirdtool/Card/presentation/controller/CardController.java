package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardImageService;
import com.example.thirdtool.Card.application.service.CardService;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.presentation.dto.CardRankInfoDto;
import com.example.thirdtool.Card.presentation.dto.WriteCardDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards/")
public class CardController {


    private final CardService cardService;
    private final CardImageService cardImageService;


    //카드 단건 조회 api
    @GetMapping("/{cardId}")
    public ResponseEntity<Card> getCardById(@PathVariable("cardId") Long cardId) {
        Card card = cardService.getCardById(cardId);
        return ResponseEntity.ok().body(card);
    }

    // 덱 ID와 학습 모드에 따라 카드 목록을 조회하는 API (무한 스크롤 대응)
    @GetMapping("/decks/{deckId}")
    public ResponseEntity<Slice<Card>> getCardsByDeckAndMode(
            @PathVariable Long deckId,
            @RequestParam(name = "mode") String modeParam,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
                                                            ) {
        DeckMode mode;
        try {
            mode = DeckMode.valueOf(modeParam.toUpperCase());
        } catch (BusinessException e) {
            log.warn("⚠️ 잘못된 mode 요청: {}", modeParam);
            return ResponseEntity.badRequest().build();
        }
        Slice<Card> cards = cardService.getCardsByDeckIdAndMode(deckId, mode, page, size);
        return ResponseEntity.ok(cards);
    }

    // ✅ 덱 ID를 기반으로 특정 랭크의 카드 목록을 조회하는 API
    // GET /api/cards/by-rank?deckId=1&rankName=SILVER
    // ✅ GET /api/cards/by-rank?deckId=1&rankName=SILVER&mode=THREE_DAY
    @GetMapping("/by-rank")
    public ResponseEntity<Slice<CardRankInfoDto>> getCardsByRank(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam Long deckId,
            @RequestParam DeckMode mode,
            @RequestParam CardRankType rankName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
                                                                ) {
        Long userId = user.getId();
        Slice<CardRankInfoDto> cards = cardService.getCardsByRank(userId, deckId, mode, rankName, page, size);

        if (cards.isEmpty()) {
            // ✅ 카드가 없을 때 204 No Content로 응답
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(cards);
    }

    // 무한 스크롤 기반 카드 조회 API
    @GetMapping("/decks/{deckId}/all")
    public ResponseEntity<Slice<Card>> getCardsById(
            @PathVariable Long deckId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
                                                   ) {
        Slice<Card> cards = cardService.getCardsByDeckId(deckId, page, size);
        return ResponseEntity.ok(cards);
    }

    // 카드 생성 API
    // POST /api/cards/decks/{deckId}
    @PostMapping("/decks/{deckId}")
    public ResponseEntity<Void> createCard(@PathVariable Long deckId,
                                           @ModelAttribute WriteCardDto writeCardDto) {
        cardService.createCard(deckId, writeCardDto);
        return ResponseEntity.ok().build();
    }

    // ✅ 여러 카드 생성
    @PostMapping("/decks/{deckId}/batch")
    public ResponseEntity<Void> createCards(@PathVariable Long deckId,
                                            @ModelAttribute List<WriteCardDto> writeCardDtos) {
        cardService.createCards(deckId, writeCardDtos);
        return ResponseEntity.ok().build();
    }

    // ✅ 카드 수정 API
    // PUT /api/cards/{cardId}
    @PutMapping("/{cardId}")
    public ResponseEntity<Void> updateCard(@PathVariable Long cardId,
                                           @ModelAttribute WriteCardDto writeCardDto) {
        cardService.updateCard(cardId, writeCardDto);
        return ResponseEntity.ok().build();
    }

    // ✅ 카드 삭제 API
    // DELETE /api/cards/{cardId}
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.ok().build();
    }

}


