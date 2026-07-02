package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.AxisRoadmapNodeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.AxisRoadmapNodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Roadmap 노드 Query Application Service (Story-LT-E3-S3-8).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AxisRoadmapNodeQueryService {

    private final LearningFacadeRepository facadeRepository;
    private final AxisRoadmapNodeRepository nodeRepository;

    /**
     * axisId 하위 활성 노드 리스트 (display_order ASC).
     * 소유권 검증: axis가 user의 facade에 속하는지.
     */
    public AxisRoadmapNodeResponse.NodeList list(Long userId, Long axisId) {
        LearningFacade facade = facadeRepository.findByUserId(userId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));
        LearningAxis axis = facade.getAxes().stream()
                .filter(a -> a.getId() != null && a.getId().equals(axisId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_NOT_FOUND));

        return AxisRoadmapNodeResponse.NodeList.of(
                nodeRepository.findByAxisIdOrderByDisplayOrderAsc(axis.getId()));
    }
}
