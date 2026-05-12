package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.TopicDeletionRecord;

import java.util.List;

public interface TopicDeletionRecordRepository {

    TopicDeletionRecord save(TopicDeletionRecord record);

    /** 특정 축의 주제 삭제 이력을 deletedAt 내림차순(최신순)으로 반환. */
    List<TopicDeletionRecord> findByLearningAxisIdOrderByDeletedAtDesc(Long learningAxisId);
}
