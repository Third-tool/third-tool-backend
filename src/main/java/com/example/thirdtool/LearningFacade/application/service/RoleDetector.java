package com.example.thirdtool.LearningFacade.application.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@code concepts[]}로부터 사용자 role hint를 자동 감지하는 유틸 서비스 (Story-AS-E3-S1).
 *
 * <p>4개 role(backend-developer / planner / designer / problem-solver) + generic fallback.
 * 매칭 규칙은 하드코드 사전 (keyword substring match, 대소문자 무시).
 *
 * <p>다중 role 매칭 시 첫 매칭(LinkedHashMap 순서)이 우선. 매칭 실패 시 "generic".
 *
 * <p>v1 — 정규식/임베딩 대신 하드코드 사전 채택 (Epic 3 기술 결정).
 */
@Component
public class RoleDetector {

    public static final String ROLE_GENERIC = "generic";

    /** 매칭 순서 결정을 위해 {@link LinkedHashMap} 사용 (첫 매칭 우선). */
    private static final Map<String, Set<String>> ROLE_KEYWORDS;

    static {
        ROLE_KEYWORDS = new LinkedHashMap<>();
        ROLE_KEYWORDS.put("backend-developer",
                Set.of("백엔드", "backend", "java", "spring", "시스템", "system", "서버", "api", "jpa", "database", "db"));
        ROLE_KEYWORDS.put("planner",
                Set.of("기획", "planner", "프로덕트", "product", "pm", "요구사항", "product manager", "기획자"));
        ROLE_KEYWORDS.put("designer",
                Set.of("디자인", "design", "ui", "ux", "와이어프레임", "wireframe", "figma", "디자이너"));
        ROLE_KEYWORDS.put("problem-solver",
                Set.of("문제해결", "problem", "알고리즘", "algorithm", "트러블슈팅", "troubleshoot", "코딩테스트", "ps"));
    }

    /**
     * concepts에서 첫 매칭 role을 반환한다. 매칭 실패 시 {@link #ROLE_GENERIC}.
     *
     * <p>입력 정규화: 각 concept은 {@code lowerCase().trim()} 후 substring 매칭.
     * null concept은 무시. 빈 리스트도 무시하고 fallback 반환.
     */
    public String detect(List<String> concepts) {
        if (concepts == null || concepts.isEmpty()) {
            return ROLE_GENERIC;
        }
        for (Map.Entry<String, Set<String>> entry : ROLE_KEYWORDS.entrySet()) {
            for (String concept : concepts) {
                if (concept == null || concept.isBlank()) {
                    continue;
                }
                String normalized = concept.toLowerCase().trim();
                for (String keyword : entry.getValue()) {
                    if (normalized.contains(keyword.toLowerCase())) {
                        return entry.getKey();
                    }
                }
            }
        }
        return ROLE_GENERIC;
    }
}
