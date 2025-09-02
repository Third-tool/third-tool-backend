package com.example.thirdtool.Scoring.aapplication.service;

import com.example.thirdtool.Scoring.domain.model.ScoringAlgorithm;
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

    private static final String DEFAULT_ALGORITHM = "sm2Algorithm"; // ✅ 기본 알고리즘 설정

    private final Map<String, ScoringAlgorithm> algorithms;

    // ✅ 애플리케이션 시작 시 사용 가능한 알고리즘 목록을 로그로 출력
    @PostConstruct
    public void init() {
        log.info("사용 가능한 학습 알고리즘: {}", algorithms.keySet());
    }

    public ScoringAlgorithm getAlgorithm(String algorithmName) {
        if (algorithmName == null || algorithmName.isBlank()) {
            log.warn("알고리즘 이름이 null 또는 비어있습니다. 기본 알고리즘을 사용합니다: {}", DEFAULT_ALGORITHM);
            return algorithms.get(DEFAULT_ALGORITHM);
        }

        String beanName = algorithmName.substring(0, 1).toLowerCase() + algorithmName.substring(1);
        ScoringAlgorithm algorithm = algorithms.get(beanName);

        if (algorithm == null) {
            log.error("지원하지 않는 알고리즘입니다: {}. 기본 알고리즘을 사용합니다.", algorithmName);
            return algorithms.get(DEFAULT_ALGORITHM);
        }

        return algorithm;
    }

    // ✅ 사용 가능한 모든 알고리즘 이름 목록을 반환
    public List<String> getAvailableAlgorithmNames() {
        return algorithms.keySet().stream()
                         .map(key -> key.substring(0, 1).toUpperCase() + key.substring(1))
                         .collect(Collectors.toList());
    }

}
