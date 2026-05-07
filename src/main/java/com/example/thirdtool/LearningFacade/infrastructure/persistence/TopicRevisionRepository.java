package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.TopicRevision;

import java.util.List;

public interface TopicRevisionRepository {

    TopicRevision save(TopicRevision revision);

    List<TopicRevision> findByTopicIdOrderByRevisedAtAsc(Long topicId);
}
