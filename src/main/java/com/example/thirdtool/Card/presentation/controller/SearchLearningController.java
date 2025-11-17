package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.SearchLearningService;
import com.example.thirdtool.Card.presentation.dto.response.CardMainResponseDto;
import com.example.thirdtool.Card.presentation.dto.response.CardSearchMainResponseDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/learning/search")
public class SearchLearningController {

    private final SearchLearningService searchLearningService;

    /** 🔎 GET /api/learning/search/{cardId} → CardSearchMainResponseDto 단일 객체 */
    @GetMapping("/{cardId}")
    public ResponseEntity<CardSearchMainResponseDto> getSearchLearning(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long cardId
                                                                      ) {
        if (user == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);

        log.info("[SearchLearningController] 🔍 검색 기반 학습 - userId={}, cardId={}", user.getId(), cardId);
        CardSearchMainResponseDto main = searchLearningService.getSearchMainCard(user.getId(), cardId);
        return ResponseEntity.ok(main);
    }
}

