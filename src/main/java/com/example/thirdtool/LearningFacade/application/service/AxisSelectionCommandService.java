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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Selection 컨테이너 + 자식 노드 Command Application Service (Story-LT-E3-S3-11).
 *
 * <p>소유권 검증: facade → axis → selection → node 체인.
 * 컨테이너 정책은 이슈 #11 계승 (name UNIQUE, created_at DESC, hard delete).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AxisSelectionCommandService {

    private final LearningFacadeRepository facadeRepository;
    private final AxisSelectionRepository selectionRepository;

    // ─── 컨테이너 CRUD ─────────────────────────────

    public AxisSelectionResponse.SelectionDetail addSelection(AxisSelectionCommand.AddSelection command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());

        AxisSelection selection = axis.addSelection(command.name());
        facadeRepository.save(facade);

        if (selection.getId() == null) {
            throw new IllegalStateException(
                    "AxisSelection id가 cascade save 후에도 null입니다. JPA 설정 회귀 가능성.");
        }
        return AxisSelectionResponse.SelectionDetail.of(selection);
    }

    public AxisSelectionResponse.SelectionDetail renameSelection(AxisSelectionCommand.RenameSelection command) {
        LearningFacade facade = loadFacade(command.userId());
        AxisSelection selection = loadSelectionOwnedBy(facade, command.selectionId());

        selection.getAxis().renameSelection(command.selectionId(), command.name());
        facadeRepository.save(facade);
        return AxisSelectionResponse.SelectionDetail.of(selection);
    }

    public void removeSelection(AxisSelectionCommand.RemoveSelection command) {
        LearningFacade facade = loadFacade(command.userId());
        AxisSelection selection = loadSelectionOwnedBy(facade, command.selectionId());

        selection.getAxis().removeSelection(command.selectionId());
        facadeRepository.save(facade);
    }

    // ─── 자식 노드 CRUD ────────────────────────────

    public AxisSelectionResponse.NodeDetail addNode(AxisSelectionCommand.AddNode command) {
        LearningFacade facade = loadFacade(command.userId());
        AxisSelection selection = loadSelectionOwnedBy(facade, command.selectionId());

        AxisSelectionNode node = selection.addNode(command.title(), command.rationale(), command.body());
        facadeRepository.save(facade);

        if (node.getId() == null) {
            throw new IllegalStateException(
                    "AxisSelectionNode id가 cascade save 후에도 null입니다.");
        }
        return AxisSelectionResponse.NodeDetail.of(node);
    }

    public AxisSelectionResponse.NodeDetail updateNode(AxisSelectionCommand.UpdateNode command) {
        LearningFacade facade = loadFacade(command.userId());
        AxisSelectionNode node = loadNodeOwnedBy(facade, command.nodeId());

        if (command.titlePresent()) {
            node.updateTitle(command.title());
        }
        if (command.rationalePresent()) {
            node.updateRationale(command.rationale());
        }
        if (command.bodyPresent()) {
            node.updateBody(command.body());
        }
        return AxisSelectionResponse.NodeDetail.of(node);
    }

    public void removeNode(AxisSelectionCommand.RemoveNode command) {
        LearningFacade facade = loadFacade(command.userId());
        AxisSelectionNode node = loadNodeOwnedBy(facade, command.nodeId());

        node.getSelection().removeNode(command.nodeId());
        facadeRepository.save(facade);
    }

    public AxisSelectionResponse.Reordered reorderNodes(AxisSelectionCommand.ReorderNodes command) {
        LearningFacade facade = loadFacade(command.userId());
        AxisSelection selection = loadSelectionOwnedBy(facade, command.selectionId());

        selection.reorderNodes(command.orderedNodeIds());
        facadeRepository.save(facade);
        return AxisSelectionResponse.Reordered.of(selection.getNodes());
    }

    // ─── helpers ─────────────────────────────────

    private LearningFacade loadFacade(Long userId) {
        return facadeRepository.findByUserId(userId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));
    }

    private LearningAxis findAxis(LearningFacade facade, Long axisId) {
        return facade.getAxes().stream()
                .filter(a -> a.getId() != null && a.getId().equals(axisId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_NOT_FOUND));
    }

    /**
     * Selection 컨테이너 로드 + 소유권 검증. 다른 유저 소유 시 NOT_FOUND 통일.
     */
    private AxisSelection loadSelectionOwnedBy(LearningFacade facade, Long selectionId) {
        AxisSelection selection = selectionRepository.findById(selectionId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.AXIS_SELECTION_NOT_FOUND));

        LearningAxis axis = selection.getAxis();
        if (axis == null || axis.getFacade() == null
                || !axis.getFacade().getId().equals(facade.getId())) {
            throw LearningFacadeDomainException.of(ErrorCode.AXIS_SELECTION_NOT_FOUND);
        }
        return selection;
    }

    private AxisSelectionNode loadNodeOwnedBy(LearningFacade facade, Long nodeId) {
        // 노드는 Selection 컨테이너를 통해서만 로드. Repository 별도 조회 대신 Selection에서 find.
        // 다만 nodeId만으로 접근하는 API가 존재하므로 별도 조회 필요.
        // 여기서는 nodeId → selection → axis → facade 체인 검증 목적.
        for (LearningAxis axis : facade.getAxes()) {
            for (AxisSelection s : axis.getSelections()) {
                for (AxisSelectionNode n : s.getNodes()) {
                    if (n.getId() != null && n.getId().equals(nodeId)) {
                        return n;
                    }
                }
            }
        }
        throw LearningFacadeDomainException.of(ErrorCode.SELECTION_NODE_NOT_FOUND);
    }
}
