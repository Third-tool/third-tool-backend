package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.application.dto.AxisSelectionCommand;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisSelection;
import com.example.thirdtool.LearningFacade.domain.model.AxisSelectionNode;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.AxisSelectionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.AxisSelectionResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Story-LT-E3-S3-11 · AxisSelectionCommandService 단위 테스트.
 */
@DisplayName("AxisSelectionCommandService (Story-LT-E3-S3-11)")
class AxisSelectionCommandServiceTest {

    private LearningFacadeRepository facadeRepository;
    private AxisSelectionRepository selectionRepository;
    private AxisSelectionCommandService service;

    private UserEntity user;
    private LearningFacade facade;
    private LearningAxis axis;

    @BeforeEach
    void setUp() {
        facadeRepository = mock(LearningFacadeRepository.class);
        selectionRepository = mock(AxisSelectionRepository.class);
        service = new AxisSelectionCommandService(facadeRepository, selectionRepository);

        user = UserEntity.ofLocal("tester", "encoded-pw", "닉", "t@t.com");
        ReflectionTestUtils.setField(user, "id", 1L);
        facade = LearningFacade.create(user, "백엔드");
        ReflectionTestUtils.setField(facade, "id", 50L);
        axis = facade.addAxis("하네스 엔지니어링");
        ReflectionTestUtils.setField(axis, "id", 10L);

        when(facadeRepository.findByUserId(1L)).thenReturn(Optional.of(facade));
        when(facadeRepository.save(facade)).thenAnswer(inv -> {
            LearningFacade saved = inv.getArgument(0);
            long selBase = 500L;
            long nodeBase = 700L;
            for (LearningAxis a : saved.getAxes()) {
                for (AxisSelection s : a.getSelections()) {
                    if (s.getId() == null) ReflectionTestUtils.setField(s, "id", selBase++);
                    for (AxisSelectionNode n : s.getNodes()) {
                        if (n.getId() == null) ReflectionTestUtils.setField(n, "id", nodeBase++);
                    }
                }
            }
            return saved;
        });
    }

    @Test
    @DisplayName("[해피] addSelection: 컨테이너 저장")
    void addSelection_saves() {
        AxisSelectionResponse.SelectionDetail detail = service.addSelection(
                new AxisSelectionCommand.AddSelection(1L, 10L, "v1"));

        assertThat(detail.name()).isEqualTo("v1");
        assertThat(detail.axisId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("[예외] addSelection: 존재하지 않는 axisId → LEARNING_AXIS_NOT_FOUND")
    void addSelection_axisNotFound() {
        assertThatThrownBy(() -> service.addSelection(
                new AxisSelectionCommand.AddSelection(1L, 999L, "v1")))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LEARNING_AXIS_NOT_FOUND);
    }

    @Test
    @DisplayName("[해피] addNode: Selection 컨테이너에 노드 추가")
    void addNode_saves() {
        AxisSelection s = axis.addSelection("v1");
        ReflectionTestUtils.setField(s, "id", 500L);
        when(selectionRepository.findById(500L)).thenReturn(Optional.of(s));

        AxisSelectionResponse.NodeDetail detail = service.addNode(
                new AxisSelectionCommand.AddNode(1L, 500L, "1. 첫", null, "├── ..."));

        assertThat(detail.title()).isEqualTo("1. 첫");
        assertThat(detail.displayOrder()).isEqualTo(1);
        assertThat(detail.selectionId()).isEqualTo(500L);
    }

    @Test
    @DisplayName("[예외] addNode: 다른 유저 소유 Selection → AXIS_SELECTION_NOT_FOUND")
    void addNode_otherUserSelection_notFound() {
        UserEntity other = UserEntity.ofLocal("other", "pw", "닉2", "o@t.com");
        ReflectionTestUtils.setField(other, "id", 2L);
        LearningFacade otherFacade = LearningFacade.create(other, "다른");
        ReflectionTestUtils.setField(otherFacade, "id", 99L);
        LearningAxis otherAxis = otherFacade.addAxis("다른 축");
        ReflectionTestUtils.setField(otherAxis, "id", 20L);
        AxisSelection otherSelection = otherAxis.addSelection("v1");
        ReflectionTestUtils.setField(otherSelection, "id", 500L);
        when(selectionRepository.findById(500L)).thenReturn(Optional.of(otherSelection));

        assertThatThrownBy(() -> service.addNode(
                new AxisSelectionCommand.AddNode(1L, 500L, "제목", null, "body")))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AXIS_SELECTION_NOT_FOUND);
    }

    @Test
    @DisplayName("[해피] removeSelection: 컨테이너 hard delete (자식 CASCADE는 orphanRemoval)")
    void removeSelection_hardDelete() {
        AxisSelection s = axis.addSelection("v1");
        ReflectionTestUtils.setField(s, "id", 500L);
        when(selectionRepository.findById(500L)).thenReturn(Optional.of(s));

        service.removeSelection(new AxisSelectionCommand.RemoveSelection(1L, 500L));

        assertThat(axis.getSelections()).isEmpty();
    }

    @Test
    @DisplayName("[해피] renameSelection: in-place 이름 변경 (이슈 #11)")
    void renameSelection_inPlace() {
        AxisSelection s = axis.addSelection("v1");
        ReflectionTestUtils.setField(s, "id", 500L);
        when(selectionRepository.findById(500L)).thenReturn(Optional.of(s));

        AxisSelectionResponse.SelectionDetail result = service.renameSelection(
                new AxisSelectionCommand.RenameSelection(1L, 500L, "v2"));

        assertThat(result.name()).isEqualTo("v2");
    }

    @Test
    @DisplayName("[해피] removeNode: 노드 hard delete")
    void removeNode_hardDelete() {
        AxisSelection s = axis.addSelection("v1");
        ReflectionTestUtils.setField(s, "id", 500L);
        AxisSelectionNode n1 = s.addNode("1. 첫", null, "b1");
        ReflectionTestUtils.setField(n1, "id", 700L);

        service.removeNode(new AxisSelectionCommand.RemoveNode(1L, 700L));

        assertThat(s.getNodes()).isEmpty();
    }

    @Test
    @DisplayName("[예외] removeNode: 없는 nodeId → SELECTION_NODE_NOT_FOUND")
    void removeNode_notFound() {
        assertThatThrownBy(() -> service.removeNode(
                new AxisSelectionCommand.RemoveNode(1L, 999L)))
                .isInstanceOf(LearningFacadeDomainException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SELECTION_NODE_NOT_FOUND);
    }
}
