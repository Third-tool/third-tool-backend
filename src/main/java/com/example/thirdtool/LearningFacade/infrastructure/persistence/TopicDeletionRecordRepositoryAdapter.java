package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.TopicDeletionRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TopicDeletionRecordRepositoryAdapter implements TopicDeletionRecordRepository {

    private final TopicDeletionRecordJpaRepository jpa;

    @Override
    public TopicDeletionRecord save(TopicDeletionRecord record) {
        return jpa.save(record);
    }

    @Override
    public List<TopicDeletionRecord> findByLearningAxisIdOrderByDeletedAtDesc(Long learningAxisId) {
        return jpa.findByLearningAxisIdOrderByDeletedAtDesc(learningAxisId);
    }
}
