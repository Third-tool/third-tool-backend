package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.CardRank;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.domain.repository.CardRankRepository;
import com.example.thirdtool.Card.presentation.dto.request.CardRankUpdateRequestDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CardRankService {

    private final CardRankRepository cardRankRepository;
    private final UserRepository userRepository;

    // ✅ 사용자의 랭크 기준을 수정하는 메서드
    @Transactional
    public void updateUserCardRank(Long userId, CardRankUpdateRequestDto dto) {
        CardRank rank = cardRankRepository.findByUserIdAndName(userId, dto.name())
                                          .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));
        rank.updateScoreRange(dto.minScore(), dto.maxScore());
    }


    // ✅ 새로운 사용자를 위한 기본 랭크를 자동 생성하는 메서드 (기존에 논의했던 로직)
    // 시작 로직입니다!!
    @Transactional
    public void createDefaultRanksIfAbsent(Long userId) {
        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 이미 랭크가 존재한다면 생성하지 않음
        boolean hasRanks = cardRankRepository.existsByUserId(userId);
        if (hasRanks) return;

        int silverMax = 99, goldMax = 150, diamondMax = 300;

        CardRank silver = CardRank.createRank(CardRankType.SILVER.name(), 0, silverMax, user);
        CardRank gold   = CardRank.createRank(CardRankType.GOLD.name(), silverMax + 1, goldMax, user);
        CardRank dia    = CardRank.createRank(CardRankType.DIAMOND.name(), goldMax + 1, diamondMax, user);

        cardRankRepository.saveAll(List.of(silver, gold, dia));

        log.info("[CardRankService] 기본 랭크 생성 완료 - userId={}", userId);
    }
}