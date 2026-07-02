package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Story-LT-E3-S3-6 · AxisRoadmapNode 도메인 단위 테스트.
 */
@DisplayName("AxisRoadmapNode 도메인 단위 테스트 (Story-LT-E3-S3-6)")
class AxisRoadmapNodeTest {

    private static final String SAMPLE_TITLE = "1. 하네스 엔지니어링 기초";
    private static final String SAMPLE_RATIONALE = "AI 에이전트 기본 프레임";
    private static final String SAMPLE_BODY = "├── 1-1. 정의와 본질\n│       모델 + 하네스 — ...";

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        axis = facade.addAxis("하네스 엔지니어링");
    }

    @Test
    @DisplayName("[해피] axis에 첫 노드 추가 시 displayOrder=1")
    void addRoadmapNode_first_displayOrder1() {
        AxisRoadmapNode node = axis.addRoadmapNode(SAMPLE_TITLE, SAMPLE_RATIONALE, SAMPLE_BODY);

        assertThat(node.getDisplayOrder()).isEqualTo(1);
        assertThat(node.getTitle()).isEqualTo(SAMPLE_TITLE);
        assertThat(node.getRationale()).isEqualTo(SAMPLE_RATIONALE);
        assertThat(node.getBody()).isEqualTo(SAMPLE_BODY);
        assertThat(axis.getRoadmapNodes()).hasSize(1);
    }

    @Test
    @DisplayName("[해피] 3번째 노드 추가 시 displayOrder=3")
    void addRoadmapNode_third_displayOrder3() {
        axis.addRoadmapNode("1. 첫 챕터", null, "body 1");
        axis.addRoadmapNode("2. 둘째 챕터", null, "body 2");

        AxisRoadmapNode third = axis.addRoadmapNode("3. 셋째 챕터", null, "body 3");

        assertThat(third.getDisplayOrder()).isEqualTo(3);
        assertThat(axis.getRoadmapNodes()).hasSize(3);
    }

    @Test
    @DisplayName("[엣지] title trim + rationale blank → null 정규화")
    void addRoadmapNode_normalize_trim_blankRationaleToNull() {
        AxisRoadmapNode node = axis.addRoadmapNode("  1. 챕터  ", "   ", "  body  ");

        assertThat(node.getTitle()).isEqualTo("1. 챕터");
        assertThat(node.getRationale()).isNull();
        assertThat(node.getBody()).isEqualTo("body");
    }

    @Test
    @DisplayName("[예외] title blank이면 ROADMAP_NODE_TITLE_BLANK")
    void addRoadmapNode_blankTitle_throws() {
        assertThatThrownBy(() -> axis.addRoadmapNode("   ", null, SAMPLE_BODY))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROADMAP_NODE_TITLE_BLANK);
    }

    @Test
    @DisplayName("[예외] body blank이면 ROADMAP_NODE_BODY_BLANK")
    void addRoadmapNode_blankBody_throws() {
        assertThatThrownBy(() -> axis.addRoadmapNode(SAMPLE_TITLE, null, "  "))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROADMAP_NODE_BODY_BLANK);
    }

    @Test
    @DisplayName("[해피] updateTitle / updateRationale / updateBody 반영")
    void updateFields() {
        AxisRoadmapNode node = axis.addRoadmapNode(SAMPLE_TITLE, SAMPLE_RATIONALE, SAMPLE_BODY);

        node.updateTitle("1. 새 챕터");
        node.updateRationale("새 근거");
        node.updateBody("새 body");

        assertThat(node.getTitle()).isEqualTo("1. 새 챕터");
        assertThat(node.getRationale()).isEqualTo("새 근거");
        assertThat(node.getBody()).isEqualTo("새 body");
    }

    @Test
    @DisplayName("[엣지] updateRationale에 blank 전달 시 null로 정규화")
    void updateRationale_blank_normalizesToNull() {
        AxisRoadmapNode node = axis.addRoadmapNode(SAMPLE_TITLE, SAMPLE_RATIONALE, SAMPLE_BODY);

        node.updateRationale("   ");

        assertThat(node.getRationale()).isNull();
    }

    @Test
    @DisplayName("[엣지] softDelete 재호출은 no-op (멱등)")
    void softDelete_idempotent() {
        AxisRoadmapNode node = axis.addRoadmapNode(SAMPLE_TITLE, null, SAMPLE_BODY);

        node.softDelete();
        var firstDeletedAt = node.getDeletedAt();
        node.softDelete();

        assertThat(node.isDeleted()).isTrue();
        assertThat(node.getDeletedAt()).isEqualTo(firstDeletedAt);
    }

    @Test
    @DisplayName("[예외] title 200자 초과 시 INVALID_INPUT")
    void addRoadmapNode_titleTooLong_throws() {
        String tooLong = "x".repeat(AxisRoadmapNode.TITLE_MAX_LENGTH + 1);

        assertThatThrownBy(() -> axis.addRoadmapNode(tooLong, null, SAMPLE_BODY))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
