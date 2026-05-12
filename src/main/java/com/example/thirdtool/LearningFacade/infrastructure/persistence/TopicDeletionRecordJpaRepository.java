package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.TopicDeletionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicDeletionRecordJpaRepository extends JpaRepository<TopicDeletionRecord, Long> {

    List<TopicDeletionRecord> findByLearningAxisIdOrderByDeletedAtDesc(Long learningAxisId);
}
