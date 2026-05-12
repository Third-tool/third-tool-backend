package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisTopic;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.FacadeDetail;
import com.example.thirdtool.LearningFacade.presentation.dto.MaterialBreakdown;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningFacadeQueryService {

    private final LearningFacadeRepository facadeRepository;
    private final TopicMaterialRepository topicMaterialRepository;

    @Transactional(readOnly = true)
    public FacadeDetail getFacade(LearningFacadeQuery.GetFacade query) {
        LearningFacade facade = facadeRepository.findByUserId(query.userId())
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));

        List<Long> topicIds = facade.getAxes().stream()
                .flatMap(axis -> axis.getTopics().stream())
                .map(AxisTopic::getId)
                .collect(Collectors.toList());

        Map<Long, MaterialBreakdown> breakdownByTopic = computeBreakdown(topicIds);
        return FacadeDetail.of(facade, breakdownByTopic);
    }

    // table-spec §3-1 (4): 저장하지 않고 인메모리 그룹핑. 주제당 자료 수가 수십 건 수준이라 한 번의 IN 쿼리로 충분.
    private Map<Long, MaterialBreakdown> computeBreakdown(List<Long> topicIds) {
        Map<Long, MaterialBreakdown> result = new HashMap<>();
        if (topicIds.isEmpty()) {
            return result;
        }

        List<TopicMaterial> mappings = topicMaterialRepository.findByTopicIdIn(topicIds);

        Map<Long, List<MaterialType>> typesByTopic = mappings.stream()
                .collect(Collectors.groupingBy(
                        tm -> tm.getTopic().getId(),
                        Collectors.mapping(
                                tm -> tm.getMaterial().getMaterialType(),
                                Collectors.toList())));

        for (Long topicId : topicIds) {
            List<MaterialType> types = typesByTopic.getOrDefault(topicId, List.of());
            result.put(topicId, MaterialBreakdown.from(types));
        }
        return result;
    }
}
