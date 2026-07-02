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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Roadmap 노드 Command Application Service (Story-LT-E3-S3-8).
 *
 * <p>모든 커맨드는 소유권 검증 (loadFacadeByUser → facade가 axis를 소유하는지 → axis가 node를 소유하는지).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AxisRoadmapNodeCommandService {

    private final LearningFacadeRepository facadeRepository;
    private final AxisRoadmapNodeRepository nodeRepository;

    public AxisRoadmapNodeResponse.NodeDetail add(AxisRoadmapNodeCommand.Add command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());

        AxisRoadmapNode node = axis.addRoadmapNode(command.title(), command.rationale(), command.body());
        facadeRepository.save(facade);

        if (node.getId() == null) {
            throw new IllegalStateException(
                    "AxisRoadmapNode id가 cascade save 후에도 null입니다. JPA 설정 회귀 가능성.");
        }
        return AxisRoadmapNodeResponse.NodeDetail.of(node);
    }

    public AxisRoadmapNodeResponse.NodeDetail update(AxisRoadmapNodeCommand.Update command) {
        LearningFacade facade = loadFacade(command.userId());
        AxisRoadmapNode node = loadNodeOwnedBy(facade, command.nodeId());

        if (command.titlePresent()) {
            node.updateTitle(command.title());
        }
        if (command.rationalePresent()) {
            node.updateRationale(command.rationale());
        }
        if (command.bodyPresent()) {
            node.updateBody(command.body());
        }
        // JPA dirty checking으로 flush 시점에 UPDATE.
        return AxisRoadmapNodeResponse.NodeDetail.of(node);
    }

    public void remove(AxisRoadmapNodeCommand.Remove command) {
        LearningFacade facade = loadFacade(command.userId());
        AxisRoadmapNode node = loadNodeOwnedBy(facade, command.nodeId());

        // 도메인 API 통과 (LearningAxis.removeRoadmapNode) — 자식 컬렉션에 대한 캡슐화 유지.
        LearningAxis axis = node.getAxis();
        axis.removeRoadmapNode(command.nodeId());
        facadeRepository.save(facade);
    }

    public AxisRoadmapNodeResponse.Reordered reorder(AxisRoadmapNodeCommand.Reorder command) {
        LearningFacade facade = loadFacade(command.userId());
        LearningAxis axis = findAxis(facade, command.axisId());

        axis.reorderRoadmapNodes(command.orderedNodeIds());
        facadeRepository.save(facade);
        return AxisRoadmapNodeResponse.Reordered.of(axis.getRoadmapNodes());
    }

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
     * 노드 로드 + 소유권 검증 통합. facade → axis → node 체인.
     */
    private AxisRoadmapNode loadNodeOwnedBy(LearningFacade facade, Long nodeId) {
        AxisRoadmapNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.ROADMAP_NODE_NOT_FOUND));

        // node.axis.facade == facade 여야 함 (userId 소유자 검증).
        LearningAxis axis = node.getAxis();
        if (axis == null || axis.getFacade() == null
                || !axis.getFacade().getId().equals(facade.getId())) {
            // 소유자 mismatch도 NOT_FOUND로 통일 (정보 노출 방지).
            throw LearningFacadeDomainException.of(ErrorCode.ROADMAP_NODE_NOT_FOUND);
        }
        return node;
    }
}
