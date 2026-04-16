package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ActionRevision")
class ActionRevisionTest {

    // ─── 테스트 픽스처 헬퍼 ───────────────────────────────────────────────

    private AxisAction action;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.create(1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드 개발자");
        LearningAxis axis = LearningAxis.create(facade, "API 설계", 1);
        action = AxisAction.create(axis, "설계하다");
    }

    @Test
    @DisplayName("수정 이유 레이블이 있을 때 정상적으로 이력이 생성된다")
    void create_valid_reason있음() {
        //when
        ActionRevision revision = ActionRevision.create(
                action, "설계하다", "검증하다", "더 정확한 표현을 찾았다");

        //then
        assertThat(revision.getPreviousDescription()).isEqualTo("설계하다");
        assertThat(revision.getNewDescription()).isEqualTo("검증하다");
        assertThat(revision.getRevisionReasonLabel()).isEqualTo("더 정확한 표현을 찾았다");
    }

    @Test
    @DisplayName("revisionReasonLabel이 null이어도 이력이 정상 생성된다 — 이유는 선택 항목")
    void create_valid_reason_null허용() {
        //when
        ActionRevision revision = ActionRevision.create(
                action, "설계하다", "검증하다", null);

        //then
        assertThat(revision.getRevisionReasonLabel()).isNull();
    }

    @Test
    @DisplayName("생성 시 revisedAt이 자동으로 설정된다")
    void create_revisedAt_자동설정() {
        //when
        ActionRevision revision = ActionRevision.create(
                action, "설계하다", "검증하다", null);

        //then
        assertThat(revision.getRevisedAt()).isNotNull();
    }

    @Test
    @DisplayName("action이 null이면 예외가 발생한다")
    void create_action_null_예외() {
        //when & then
        assertThatThrownBy(() ->
                ActionRevision.create(null, "설계하다", "검증하다", null))
                .isInstanceOf(LearningFacadeDomainException.class);
    }

    @Test
    @DisplayName("previousDescription이 null이면 예외가 발생한다")
    void create_previousDescription_null_예외() {
        //when & then
        assertThatThrownBy(() ->
                ActionRevision.create(action, null, "검증하다", null))
                .isInstanceOf(LearningFacadeDomainException.class);
    }

    @Test
    @DisplayName("newDescription이 null이면 예외가 발생한다")
    void create_newDescription_null_예외() {
        //when & then
        assertThatThrownBy(() ->
                ActionRevision.create(action, "설계하다", null, null))
                .isInstanceOf(LearningFacadeDomainException.class);
    }
}
