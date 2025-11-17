package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.application.resolver.PermanentThresholdResolver;
import com.example.thirdtool.Card.domain.model.CardRank;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.domain.repository.CardRankRepository;
import com.example.thirdtool.Card.presentation.dto.request.CardRankBoundaryUpdateRequestDto;
import com.example.thirdtool.Card.presentation.dto.request.CardRankUpdateRequestDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.example.thirdtool.User.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CardRankService {

    private final CardRankRepository cardRankRepository;
    private final UserRepository userRepository;


    @Transactional
    public void updateUserCardRankBoundaries(Long userId, CardRankBoundaryUpdateRequestDto dto) {
        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<CardRank> ranks = cardRankRepository.findAllByUserIdOrderByMinScoreAsc(userId);
        if (ranks.size() != 3)
            throw new BusinessException(ErrorCode.RANK_NOT_FOUND);

        CardRank silver = ranks.stream().filter(r -> r.getName().equals(CardRankType.SILVER.name())).findFirst()
                               .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));
        CardRank gold = ranks.stream().filter(r -> r.getName().equals(CardRankType.GOLD.name())).findFirst()
                             .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));
        CardRank dia = ranks.stream().filter(r -> r.getName().equals(CardRankType.DIAMOND.name())).findFirst()
                            .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));

        int silverMin = silver.getMinScore();
        int silverMax = require(dto.getSilverGoldBoundary(), "silverGoldBoundary");
        int goldMax = require(dto.getGoldDiamondBoundary(), "goldDiamondBoundary");
        int diamondMax = require(dto.getDiamondMax(), "diamondMax");

        // ✅ 검증: 연속적이고 증가하는 순서
        if (!(silverMin <= silverMax && silverMax < goldMax && goldMax < diamondMax)) {
            throw new BusinessException(ErrorCode.INVALID_RANK_RANGE);
        }

        // ✅ 각 구간 재계산
        silver.updateScoreRange(silverMin, silverMax);
        gold.updateScoreRange(silverMax + 1, goldMax);
        dia.updateScoreRange(goldMax + 1, diamondMax);

        log.info("[CardRankService] userId={} rank boundaries updated: S={}~{}, G={}~{}, D={}~{}",
                userId, silverMin, silverMax, silverMax + 1, goldMax, goldMax + 1, diamondMax);
    }

    private int require(Integer val, String field) {
        if (val == null || val < 0)
            throw new BusinessException(ErrorCode.INVALID_RANK_RANGE);
        return val;
    }

    // ✅ 새로운 사용자를 위한 기본 랭크를 자동 생성하는 메서드 (기존에 논의했던 로직)
    // 시작 로직입니다!!
    @Transactional
    public void createDefaultRanksIfAbsent(Long userId) {
        UserEntity user = userRepository.findById(userId)
                                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (cardRankRepository.existsByUserId(userId)) return;

        // ✅ 기본값(초기 디폴트) 300을 DIAMOND max로 사용
        int diamondMax = PermanentThresholdResolver.DEFAULT_THRESHOLD;
        int readyScore = PermanentThresholdResolver.DEFAULT_SILVER_MIN;
        // 예시: 0~99, 100~199, 200~diamondMax(=300)
        int silverMax = 99;
        int goldMax   = 199;

        CardRank silver = CardRank.createRank(CardRankType.SILVER.name(), readyScore, silverMax, user);
        CardRank gold   = CardRank.createRank(CardRankType.GOLD.name(), silverMax + 1, goldMax, user);
        CardRank dia    = CardRank.createRank(CardRankType.DIAMOND.name(), goldMax + 1, diamondMax, user);

        cardRankRepository.saveAll(List.of(silver, gold, dia));
        log.info("[CardRankService] 기본 랭크 생성 완료 - userId={}", userId);
    }
}