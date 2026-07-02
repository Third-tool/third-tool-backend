package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Story-LT-E3-S3-9 · AxisSelection 컨테이너 · 자식 노드 도메인 단위 테스트.
 */
@DisplayName("AxisSelection · AxisSelectionNode (Story-LT-E3-S3-9)")
class AxisSelectionTest {

    private static final String SAMPLE_TITLE = "1. IoC & DI 핵심 원리";
    private static final String SAMPLE_BODY = "├── 1-1. 의존성 역전\n│       모듈 간 결합도 낮춤";

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        axis = facade.addAxis("하네스 엔지니어링");
    }

    // ─── 컨테이너 CRUD ─────────────────────────────

    @Test
    @DisplayName("[해피] addSelection: 컨테이너 저장 + name trim")
    void addSelection_savesWithTrimmedName() {
        AxisSelection selection = axis.addSelection("  능 아키텍처 selections v1  ");

        assertThat(selection.getName()).isEqualTo("능 아키텍처 selections v1");
        assertThat(axis.getSelections()).hasSize(1);
    }

    @Test
    @DisplayName("[예외] addSelection: name blank → AXIS_SELECTION_NAME_BLANK")
    void addSelection_blankName_throws() {
        assertThatThrownBy(() -> axis.addSelection("   "))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AXIS_SELECTION_NAME_BLANK);
    }

    @Test
    @DisplayName("[예외] addSelection: 동일 이름 중복 → AXIS_SELECTION_NAME_ALREADY_EXISTS (이슈 #11 계승)")
    void addSelection_duplicateName_throws() {
        axis.addSelection("selections v1");

        assertThatThrownBy(() -> axis.addSelection("selections v1"))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AXIS_SELECTION_NAME_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("[해피] renameSelection: in-place 이름 변경 (이슈 #11 계승)")
    void renameSelection_inPlace() {
        AxisSelection s = axis.addSelection("v1");
        ReflectionTestUtils.setField(s, "id", 100L);

        axis.renameSelection(100L, "v2");

        assertThat(s.getName()).isEqualTo("v2");
    }

    @Test
    @DisplayName("[예외] renameSelection: 다른 활성 컨테이너와 이름 중복 → 예외")
    void renameSelection_duplicate_throws() {
        AxisSelection s1 = axis.addSelection("v1");
        AxisSelection s2 = axis.addSelection("v2");
        ReflectionTestUtils.setField(s1, "id", 100L);
        ReflectionTestUtils.setField(s2, "id", 200L);

        assertThatThrownBy(() -> axis.renameSelection(200L, "v1"))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AXIS_SELECTION_NAME_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("[해피] removeSelection: 컨테이너 hard delete (컬렉션에서 제거)")
    void removeSelection_hardDelete() {
        AxisSelection s = axis.addSelection("v1");
        ReflectionTestUtils.setField(s, "id", 100L);

        axis.removeSelection(100L);

        assertThat(axis.getSelections()).isEmpty();
    }

    @Test
    @DisplayName("[예외] removeSelection: 없는 selectionId → AXIS_SELECTION_NOT_FOUND")
    void removeSelection_notFound_throws() {
        assertThatThrownBy(() -> axis.removeSelection(999L))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AXIS_SELECTION_NOT_FOUND);
    }

    // ─── 자식 노드 CRUD ─────────────────────────────

    @Test
    @DisplayName("[해피] addNode: 첫 노드 → displayOrder=1")
    void addNode_first_displayOrder1() {
        AxisSelection s = axis.addSelection("v1");

        AxisSelectionNode node = s.addNode(SAMPLE_TITLE, "근거", SAMPLE_BODY);

        assertThat(node.getDisplayOrder()).isEqualTo(1);
        assertThat(node.getTitle()).isEqualTo(SAMPLE_TITLE);
        assertThat(node.getRationale()).isEqualTo("근거");
        assertThat(node.getBody()).isEqualTo(SAMPLE_BODY);
    }

    @Test
    @DisplayName("[엣지] addNode: title trim + rationale blank → null")
    void addNode_normalize() {
        AxisSelection s = axis.addSelection("v1");

        AxisSelectionNode node = s.addNode("  제목  ", "   ", "  body  ");

        assertThat(node.getTitle()).isEqualTo("제목");
        assertThat(node.getRationale()).isNull();
        assertThat(node.getBody()).isEqualTo("body");
    }

    @Test
    @DisplayName("[예외] addNode: title blank → SELECTION_NODE_TITLE_BLANK")
    void addNode_blankTitle_throws() {
        AxisSelection s = axis.addSelection("v1");

        assertThatThrownBy(() -> s.addNode("   ", null, SAMPLE_BODY))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SELECTION_NODE_TITLE_BLANK);
    }

    @Test
    @DisplayName("[예외] addNode: body blank → SELECTION_NODE_BODY_BLANK")
    void addNode_blankBody_throws() {
        AxisSelection s = axis.addSelection("v1");

        assertThatThrownBy(() -> s.addNode(SAMPLE_TITLE, null, "  "))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SELECTION_NODE_BODY_BLANK);
    }

    @Test
    @DisplayName("[해피] reorderNodes: 노드 순서 재부여")
    void reorderNodes_reassignsOrder() {
        AxisSelection s = axis.addSelection("v1");
        AxisSelectionNode n1 = s.addNode("1. 첫", null, "b1");
        AxisSelectionNode n2 = s.addNode("2. 둘", null, "b2");
        AxisSelectionNode n3 = s.addNode("3. 셋", null, "b3");
        ReflectionTestUtils.setField(n1, "id", 100L);
        ReflectionTestUtils.setField(n2, "id", 200L);
        ReflectionTestUtils.setField(n3, "id", 300L);

        s.reorderNodes(List.of(300L, 100L, 200L));

        assertThat(n3.getDisplayOrder()).isEqualTo(1);
        assertThat(n1.getDisplayOrder()).isEqualTo(2);
        assertThat(n2.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("[예외] reorderNodes: id 집합 불일치 → SELECTION_NODE_ORDER_MISMATCH")
    void reorderNodes_idMismatch_throws() {
        AxisSelection s = axis.addSelection("v1");
        AxisSelectionNode n1 = s.addNode("1. 첫", null, "b1");
        ReflectionTestUtils.setField(n1, "id", 100L);

        assertThatThrownBy(() -> s.reorderNodes(List.of(100L, 999L)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SELECTION_NODE_ORDER_MISMATCH);
    }

    @Test
    @DisplayName("[해피] removeNode: 노드 hard delete (컨테이너 정책 계승)")
    void removeNode_hardDelete() {
        AxisSelection s = axis.addSelection("v1");
        AxisSelectionNode n1 = s.addNode("1. 첫", null, "b1");
        AxisSelectionNode n2 = s.addNode("2. 둘", null, "b2");
        ReflectionTestUtils.setField(n1, "id", 100L);
        ReflectionTestUtils.setField(n2, "id", 200L);

        s.removeNode(100L);

        assertThat(s.getNodes()).hasSize(1);
        assertThat(s.getNodes().get(0).getId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("[예외] removeNode: 없는 nodeId → SELECTION_NODE_NOT_FOUND")
    void removeNode_notFound_throws() {
        AxisSelection s = axis.addSelection("v1");

        assertThatThrownBy(() -> s.removeNode(999L))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SELECTION_NODE_NOT_FOUND);
    }

    @Test
    @DisplayName("[해피] getNodes: unmodifiable 반환")
    void getNodes_unmodifiable() {
        AxisSelection s = axis.addSelection("v1");
        s.addNode("1. 첫", null, "b1");

        List<AxisSelectionNode> nodes = s.getNodes();

        assertThatThrownBy(() -> nodes.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
