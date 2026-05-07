package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.LearningFacade.domain.model.TopicRevision;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicRevisionRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.RevisionReasonOptionItem;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.TopicRevisionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TopicRevisionQueryService {

    private final TopicRevisionRepository topicRevisionRepository;
    private final RevisionReasonOptionRepository revisionReasonOptionRepository;

    /**
     * 주제별 이름 수정 이력을 revisedAt 오름차순으로 반환한다.
     * 이력이 없으면 빈 목록을 반환한다.
     */
    public LearningFacadeResponse.TopicRevisions getRevisions(Long topicId) {
        List<TopicRevision> revisions = topicRevisionRepository.findByTopicIdOrderByRevisedAtAsc(topicId);
        return new LearningFacadeResponse.TopicRevisions(
                topicId,
                revisions.stream().map(TopicRevisionItem::of).toList()
        );
    }

    /**
     * 활성 수정 이유 선택지 목록 (FE 라디오 버튼 등 노출용).
     */
    public List<RevisionReasonOptionItem> getActiveReasonOptions() {
        return revisionReasonOptionRepository.findActiveOrderByDisplayOrderAsc().stream()
                .map(RevisionReasonOptionItem::of)
                .toList();
    }
}
