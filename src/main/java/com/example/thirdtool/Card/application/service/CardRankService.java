package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.CardRank;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.domain.repository.CardRankRepository;
import com.example.thirdtool.Card.presentation.dto.CardRankUpdateRequestDto;
import com.example.thirdtool.User.domain.model.User;
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
    public void updateUserCardRank(Long userId, CardRankUpdateRequestDto updateDto) {
        // 1. 해당 사용자의 랭크를 조회합니다.
        CardRank existingRank = cardRankRepository.findByUserIdAndName(userId, updateDto.name())
                                                  .orElseThrow(() -> new IllegalArgumentException("유저 랭크 값이 확인이 안됩니다!"));

        // 2. 새로운 점수 기준으로 업데이트합니다.
        existingRank.updateScoreRange(updateDto.minScore(), updateDto.maxScore());
    }

    // ✅ 새로운 사용자를 위한 기본 랭크를 자동 생성하는 메서드 (기존에 논의했던 로직)
    // 시작 로직입니다!!
    @Transactional
    public void createDefaultRanksForUser(Long userId) {
        // 1. userId로 User 객체를 조회합니다.
        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new IllegalArgumentException("없는 사용자입니당"));
        // 기본 랭크 설정값
        int silverMaxScore = 99;
        int goldMaxScore = 150;
        int diamondMaxScore = 300;

        // ✅ CardRank.createRank() 메서드 사용
        CardRank silver = CardRank.createRank(CardRankType.SILVER.name(), 0, silverMaxScore, user);
        CardRank gold = CardRank.createRank(CardRankType.GOLD.name(), silverMaxScore + 1, goldMaxScore, user);
        CardRank diamond = CardRank.createRank(CardRankType.DIAMOND.name(), goldMaxScore + 1, diamondMaxScore, user);


        cardRankRepository.saveAll(List.of(silver, gold, diamond));
    }
}