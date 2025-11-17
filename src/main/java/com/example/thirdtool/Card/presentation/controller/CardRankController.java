package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardRankService;
import com.example.thirdtool.Card.presentation.dto.request.CardRankBoundaryUpdateRequestDto;
import com.example.thirdtool.Card.presentation.dto.request.CardRankUpdateRequestDto;
import com.example.thirdtool.Card.presentation.dto.request.UserCreateRequestDto;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/card-ranks")
public class CardRankController {

    private final CardRankService cardRankService;


    // ✅ 사용자의 랭크 기준을 수정하는 API
    // PUT /api/card-ranks/users/{userId}
    @PutMapping("/users/me/boundaries")
    public ResponseEntity<Void> updateUserCardRankBoundaries(@AuthenticationPrincipal UserEntity user,
                                                             @RequestBody CardRankBoundaryUpdateRequestDto dto) {
        Long userId = user.getId();

        cardRankService.updateUserCardRankBoundaries(userId, dto);
        return ResponseEntity.ok().build();
    }

}