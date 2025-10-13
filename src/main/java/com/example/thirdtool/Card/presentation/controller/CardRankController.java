package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardRankService;
import com.example.thirdtool.Card.presentation.dto.request.CardRankUpdateRequestDto;
import com.example.thirdtool.Card.presentation.dto.request.UserCreateRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/card-ranks")
public class CardRankController {

    private final CardRankService cardRankService;


    // ✅ 사용자의 랭크 기준을 수정하는 API
    // PUT /api/card-ranks/users/{userId}
    @PutMapping("/users/{userId}")
    public ResponseEntity<Void> updateUserCardRank(@PathVariable Long userId,
                                                   @RequestBody CardRankUpdateRequestDto updateDto) {
        cardRankService.updateUserCardRank(userId, updateDto);
        return ResponseEntity.ok().build();
    }

}