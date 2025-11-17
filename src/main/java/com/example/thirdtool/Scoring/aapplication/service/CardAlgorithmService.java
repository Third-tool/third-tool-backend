package com.example.thirdtool.Scoring.aapplication.service;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Scoring.domain.model.algorithm.ScoringAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class CardAlgorithmService {

    private final Map<String, ScoringAlgorithm> algorithms;

    // 명시적 매핑 — Deck의 scoringAlgorithmType 값과 연결
    private static final Map<String, String> NAME_MAP = Map.of(
            "SM2", "sm2Algorithm",
            "LEITNER", "leitnerAlgorithm"
                                                              );

    @PostConstruct
    public void init() {
        log.info("✅ 등록된 학습 알고리즘 빈 목록: {}", algorithms.keySet());
    }

    public ScoringAlgorithm getAlgorithm(String algorithmType) {
        if (algorithmType == null || algorithmType.isBlank()) {
            throw new BusinessException(ErrorCode.DECK_ALGORITHM_NOT_SET);
        }

        String beanName = NAME_MAP.get(algorithmType.toUpperCase());

        if (beanName == null) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_ALGORITHM);
        }

        ScoringAlgorithm algorithm = algorithms.get(beanName);
        if (algorithm == null) {
            throw new BusinessException(ErrorCode.ALGORITHM_BEAN_NOT_FOUND);
        }

        log.debug("✅ 선택된 학습 알고리즘: {}", beanName);
        return algorithm;
    }

    /**
     * 현재 애플리케이션에서 등록된 모든 알고리즘 이름 조회용
     */
    public List<String> getAvailableAlgorithmNames() {
        return algorithms.keySet().stream().sorted().toList();
    }
}
