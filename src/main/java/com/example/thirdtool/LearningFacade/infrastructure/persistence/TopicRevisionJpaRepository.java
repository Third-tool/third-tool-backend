package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.TopicRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRevisionJpaRepository extends JpaRepository<TopicRevision, Long> {

    List<TopicRevision> findByTopic_IdOrderByRevisedAtAsc(Long topicId);
}
