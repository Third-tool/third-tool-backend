package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.application.dto.AxisRoadmapNodeCommand;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisRoadmapNode;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.AxisRoadmapNodeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.AxisRoadmapNodeResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Story-LT-E3-S3-8 · AxisRoadmapNodeCommandService 단위 테스트.
 * Repository는 Mock, 도메인은 실제 객체 (Classist).
 */
@DisplayName("AxisRoadmapNodeCommandService (Story-LT-E3-S3-8)")
class AxisRoadmapNodeCommandServiceTest {

    private LearningFacadeRepository facadeRepository;
    private AxisRoadmapNodeRepository nodeRepository;
    private AxisRoadmapNodeCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        nodeRepository = mock(AxisRoadmapNodeRepository.class);
        service = new AxisRoadmapNodeCommandService(facadeRepository, nodeRepository);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);

        facade = LearningFacade.create(user, "백엔드");
        ReflectionTestUtils.setField(facade, "id", 50L);
        axis = facade.addAxis("하네스 엔지니어링");
        ReflectionTestUtils.setField(axis, "id", 10L);

        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.of(facade));
        when(facadeRepository.save(facade)).thenAnswer(inv -> {
            LearningFacade saved = inv.getArgument(0);
            var nodes = saved.getAxes().get(0).getRoadmapNodes();
            for (int i = 0; i < nodes.size(); i++) {
                AxisRoadmapNode n = nodes.get(i);
                if (n.getId() == null) ReflectionTestUtils.setField(n, "id", 500L + i);
            }
            return saved;
        });
    }

    @Test
    @DisplayName("[해피] add: 노드 저장 + Response 반환")
    void add_savesAndReturns() {
        AxisRoadmapNodeResponse.NodeDetail detail = service.add(
                new AxisRoadmapNodeCommand.Add(1L, 10L, "1. 기초", "근거", "├── 1-1. ..."));

        assertThat(detail.title()).isEqualTo("1. 기초");
        assertThat(detail.rationale()).isEqualTo("근거");
        assertThat(detail.displayOrder()).isEqualTo(1);
        assertThat(detail.axisId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("[예외] add: 존재하지 않는 axisId → LEARNING_AXIS_NOT_FOUND")
    void add_axisNotFound_throws() {
        assertThatThrownBy(() -> service.add(
                new AxisRoadmapNodeCommand.Add(1L, 999L, "제목", null, "body")))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_AXIS_NOT_FOUND);
    }

    @Test
    @DisplayName("[해피] update: title/rationale/body 부분 업데이트")
    void update_partialFields() {
        AxisRoadmapNode node = axis.addRoadmapNode("1. 기초", null, "b1");
        ReflectionTestUtils.setField(node, "id", 500L);
        ReflectionTestUtils.setField(node, "axis", axis);
        when(nodeRepository.findById(500L)).thenReturn(Optional.of(node));

        AxisRoadmapNodeResponse.NodeDetail updated = service.update(
                new AxisRoadmapNodeCommand.Update(1L, 500L, "1. 새 제목", null, null,
                        true, false, false));

        assertThat(updated.title()).isEqualTo("1. 새 제목");
        assertThat(updated.body()).isEqualTo("b1");
    }

    @Test
    @DisplayName("[예외] update: 다른 유저의 노드는 ROADMAP_NODE_NOT_FOUND (정보 노출 방지)")
    void update_otherUserNode_notFound() {
        // 다른 facade의 axis에 붙은 노드
        UserEntity otherUser = UserEntity.ofLocal("other", "pw", "닉2", "o@t.com");
        ReflectionTestUtils.setField(otherUser, "id", 2L);
        LearningFacade otherFacade = LearningFacade.create(otherUser, "다른컨셉");
        ReflectionTestUtils.setField(otherFacade, "id", 99L);
        LearningAxis otherAxis = otherFacade.addAxis("다른 축");
        ReflectionTestUtils.setField(otherAxis, "id", 20L);
        AxisRoadmapNode otherNode = otherAxis.addRoadmapNode("1. 다른", null, "b");
        ReflectionTestUtils.setField(otherNode, "id", 600L);
        ReflectionTestUtils.setField(otherNode, "axis", otherAxis);
        when(nodeRepository.findById(600L)).thenReturn(Optional.of(otherNode));

        assertThatThrownBy(() -> service.update(
                new AxisRoadmapNodeCommand.Update(1L, 600L, "새 제목", null, null,
                        true, false, false)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROADMAP_NODE_NOT_FOUND);
    }

    @Test
    @DisplayName("[해피] remove: 노드 소프트 삭제")
    void remove_softDeletes() {
        AxisRoadmapNode node = axis.addRoadmapNode("1. 기초", null, "b1");
        ReflectionTestUtils.setField(node, "id", 500L);
        ReflectionTestUtils.setField(node, "axis", axis);
        when(nodeRepository.findById(500L)).thenReturn(Optional.of(node));

        service.remove(new AxisRoadmapNodeCommand.Remove(1L, 500L));

        assertThat(node.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("[해피] reorder: 노드 순서 재부여")
    void reorder_reassignsOrder() {
        AxisRoadmapNode n1 = axis.addRoadmapNode("1. 첫", null, "b1");
        AxisRoadmapNode n2 = axis.addRoadmapNode("2. 둘", null, "b2");
        AxisRoadmapNode n3 = axis.addRoadmapNode("3. 셋", null, "b3");
        ReflectionTestUtils.setField(n1, "id", 500L);
        ReflectionTestUtils.setField(n2, "id", 501L);
        ReflectionTestUtils.setField(n3, "id", 502L);

        AxisRoadmapNodeResponse.Reordered result = service.reorder(
                new AxisRoadmapNodeCommand.Reorder(1L, 10L, List.of(502L, 500L, 501L)));

        assertThat(n3.getDisplayOrder()).isEqualTo(1);
        assertThat(n1.getDisplayOrder()).isEqualTo(2);
        assertThat(n2.getDisplayOrder()).isEqualTo(3);
        assertThat(result.nodes()).hasSize(3);
    }

    @Test
    @DisplayName("[예외] reorder: id 집합 불일치 → ROADMAP_NODE_ORDER_MISMATCH")
    void reorder_idMismatch_throws() {
        AxisRoadmapNode n1 = axis.addRoadmapNode("1. 첫", null, "b1");
        ReflectionTestUtils.setField(n1, "id", 500L);

        assertThatThrownBy(() -> service.reorder(
                new AxisRoadmapNodeCommand.Reorder(1L, 10L, List.of(999L))))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROADMAP_NODE_ORDER_MISMATCH);
    }

    @Test
    @DisplayName("[예외] update: 없는 nodeId → ROADMAP_NODE_NOT_FOUND")
    void update_nodeNotFound_throws() {
        when(nodeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                new AxisRoadmapNodeCommand.Update(1L, 999L, "새 제목", null, null,
                        true, false, false)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROADMAP_NODE_NOT_FOUND);
    }
}
