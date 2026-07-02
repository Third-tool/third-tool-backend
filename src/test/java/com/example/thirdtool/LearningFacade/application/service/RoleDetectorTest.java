package com.example.thirdtool.LearningFacade.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-AS-E3-S1 · RoleDetector 하드코드 사전 매칭 검증.
 */
@DisplayName("RoleDetector — 4 role + generic fallback")
class RoleDetectorTest {

    private final RoleDetector detector = new RoleDetector();

    @Test
    @DisplayName("detect_백엔드_시스템설계_backend_developer_반환")
    void detect_백엔드_시스템설계_backend_developer_반환() {
        String role = detector.detect(List.of("백엔드", "시스템설계"));
        assertThat(role).isEqualTo("backend-developer");
    }

    @Test
    @DisplayName("detect_기획_프로덕트_planner_반환")
    void detect_기획_프로덕트_planner_반환() {
        String role = detector.detect(List.of("기획", "프로덕트"));
        assertThat(role).isEqualTo("planner");
    }

    @Test
    @DisplayName("detect_UI_와이어프레임_designer_반환")
    void detect_UI_와이어프레임_designer_반환() {
        String role = detector.detect(List.of("UI", "와이어프레임"));
        assertThat(role).isEqualTo("designer");
    }

    @Test
    @DisplayName("detect_알고리즘_트러블슈팅_problem_solver_반환")
    void detect_알고리즘_트러블슈팅_problem_solver_반환() {
        String role = detector.detect(List.of("알고리즘", "트러블슈팅"));
        assertThat(role).isEqualTo("problem-solver");
    }

    @Test
    @DisplayName("detect_대소문자_무시_backend")
    void detect_대소문자_무시_backend() {
        String role = detector.detect(List.of("SPRING", "JAVA"));
        assertThat(role).isEqualTo("backend-developer");
    }

    @Test
    @DisplayName("detect_공백_포함_trim_적용")
    void detect_공백_포함_trim_적용() {
        String role = detector.detect(List.of("  백엔드  "));
        assertThat(role).isEqualTo("backend-developer");
    }

    @Test
    @DisplayName("detect_매칭_실패_generic_반환")
    void detect_매칭_실패_generic_반환() {
        String role = detector.detect(List.of("random-word"));
        assertThat(role).isEqualTo(RoleDetector.ROLE_GENERIC);
    }

    @Test
    @DisplayName("detect_빈_리스트_generic_반환")
    void detect_빈_리스트_generic_반환() {
        String role = detector.detect(List.of());
        assertThat(role).isEqualTo(RoleDetector.ROLE_GENERIC);
    }

    @Test
    @DisplayName("detect_null_리스트_generic_반환")
    void detect_null_리스트_generic_반환() {
        String role = detector.detect(null);
        assertThat(role).isEqualTo(RoleDetector.ROLE_GENERIC);
    }

    @Test
    @DisplayName("detect_null_원소_무시_다음_원소_매칭")
    void detect_null_원소_무시_다음_원소_매칭() {
        String role = detector.detect(java.util.Arrays.asList(null, "  ", "백엔드"));
        assertThat(role).isEqualTo("backend-developer");
    }

    @Test
    @DisplayName("detect_다중_role_매칭시_첫_매칭_반환_backend_먼저")
    void detect_다중_role_매칭시_첫_매칭_반환_backend_먼저() {
        // LinkedHashMap 순서: backend-developer > planner > designer > problem-solver
        // "백엔드"(backend) + "디자인"(designer) 조합 → backend 우선
        String role = detector.detect(List.of("디자인", "백엔드"));
        assertThat(role).isEqualTo("backend-developer");
    }
}
