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
 * Story-LT-E3-S3-6 · LearningAxis.roadmapNodes 컬렉션 확장 검증.
 * (id 부여가 필요한 reorder/find/remove 케이스는 ReflectionTestUtils로 id 주입 후 검증)
 */
@DisplayName("LearningAxis · roadmapNodes 컬렉션 (Story-LT-E3-S3-6)")
class LearningAxisRoadmapNodesTest {

    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        UserEntity user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        LearningFacade facade = LearningFacade.create(user, "백엔드");
        axis = facade.addAxis("하네스 엔지니어링");
    }

    @Test
    @DisplayName("[해피] findRoadmapNode: id로 노드 조회")
    void findRoadmapNode_by_id() {
        AxisRoadmapNode n1 = axis.addRoadmapNode("1. 첫 챕터", null, "body 1");
        ReflectionTestUtils.setField(n1, "id", 100L);

        AxisRoadmapNode found = axis.findRoadmapNode(100L);

        assertThat(found).isSameAs(n1);
    }

    @Test
    @DisplayName("[예외] findRoadmapNode: 없는 id → ROADMAP_NODE_NOT_FOUND")
    void findRoadmapNode_notFound_throws() {
        assertThatThrownBy(() -> axis.findRoadmapNode(999L))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROADMAP_NODE_NOT_FOUND);
    }

    @Test
    @DisplayName("[해피] reorderRoadmapNodes: id 순서대로 displayOrder 재부여")
    void reorderRoadmapNodes_reassignsDisplayOrder() {
        AxisRoadmapNode n1 = axis.addRoadmapNode("1. 첫", null, "b1");
        AxisRoadmapNode n2 = axis.addRoadmapNode("2. 둘", null, "b2");
        AxisRoadmapNode n3 = axis.addRoadmapNode("3. 셋", null, "b3");
        ReflectionTestUtils.setField(n1, "id", 100L);
        ReflectionTestUtils.setField(n2, "id", 200L);
        ReflectionTestUtils.setField(n3, "id", 300L);

        axis.reorderRoadmapNodes(List.of(300L, 100L, 200L));

        assertThat(n3.getDisplayOrder()).isEqualTo(1);
        assertThat(n1.getDisplayOrder()).isEqualTo(2);
        assertThat(n2.getDisplayOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("[예외] reorderRoadmapNodes: id 집합 불일치 → ROADMAP_NODE_ORDER_MISMATCH")
    void reorderRoadmapNodes_idMismatch_throws() {
        AxisRoadmapNode n1 = axis.addRoadmapNode("1. 첫", null, "b1");
        AxisRoadmapNode n2 = axis.addRoadmapNode("2. 둘", null, "b2");
        ReflectionTestUtils.setField(n1, "id", 100L);
        ReflectionTestUtils.setField(n2, "id", 200L);

        // 유령 id 999 포함
        assertThatThrownBy(() -> axis.reorderRoadmapNodes(List.of(100L, 999L)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROADMAP_NODE_ORDER_MISMATCH);
    }

    @Test
    @DisplayName("[예외] reorderRoadmapNodes: 사이즈 다름 → ROADMAP_NODE_ORDER_MISMATCH")
    void reorderRoadmapNodes_sizeMismatch_throws() {
        AxisRoadmapNode n1 = axis.addRoadmapNode("1. 첫", null, "b1");
        AxisRoadmapNode n2 = axis.addRoadmapNode("2. 둘", null, "b2");
        ReflectionTestUtils.setField(n1, "id", 100L);
        ReflectionTestUtils.setField(n2, "id", 200L);

        assertThatThrownBy(() -> axis.reorderRoadmapNodes(List.of(100L)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROADMAP_NODE_ORDER_MISMATCH);
    }

    @Test
    @DisplayName("[예외] reorderRoadmapNodes: null 전달 → ROADMAP_NODE_ORDER_MISMATCH")
    void reorderRoadmapNodes_null_throws() {
        assertThatThrownBy(() -> axis.reorderRoadmapNodes(null))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROADMAP_NODE_ORDER_MISMATCH);
    }

    @Test
    @DisplayName("[해피] removeRoadmapNode: 노드 소프트 삭제")
    void removeRoadmapNode_softDeletes() {
        AxisRoadmapNode n1 = axis.addRoadmapNode("1. 첫", null, "b1");
        ReflectionTestUtils.setField(n1, "id", 100L);

        axis.removeRoadmapNode(100L);

        assertThat(n1.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("[해피] getRoadmapNodes: unmodifiable 반환")
    void getRoadmapNodes_isUnmodifiable() {
        axis.addRoadmapNode("1. 첫", null, "b1");

        List<AxisRoadmapNode> nodes = axis.getRoadmapNodes();

        assertThatThrownBy(() -> nodes.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("[엣지] reorderRoadmapNodes: 빈 리스트 + 빈 컬렉션 → 정상")
    void reorderRoadmapNodes_emptyList_noop() {
        axis.reorderRoadmapNodes(List.of());
        // 예외 발생 없으면 통과
        assertThat(axis.getRoadmapNodes()).isEmpty();
    }
}
