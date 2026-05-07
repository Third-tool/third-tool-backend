package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.TopicRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TopicRevisionRepositoryAdapter implements TopicRevisionRepository {

    private final TopicRevisionJpaRepository jpa;

    @Override
    public TopicRevision save(TopicRevision revision) {
        return jpa.save(revision);
    }

    @Override
    public List<TopicRevision> findByTopicIdOrderByRevisedAtAsc(Long topicId) {
        return jpa.findByTopic_IdOrderByRevisedAtAsc(topicId);
    }
}
