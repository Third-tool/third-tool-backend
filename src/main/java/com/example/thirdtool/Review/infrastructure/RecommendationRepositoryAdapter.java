package com.example.thirdtool.Review.infrastructure;

import com.example.thirdtool.Review.domain.model.Recommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RecommendationRepositoryAdapter implements RecommendationRepository {

    private final RecommendationJpaRepository jpa;

    @Override
    public Recommendation save(Recommendation recommendation) {
        return jpa.save(recommendation);
    }

    @Override
    public Optional<Recommendation> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<Recommendation> findLatestByUserIdSince(Long userId, LocalDateTime since) {
        return jpa.findLatestSince(userId, since).stream().findFirst();
    }

    @Override
    public Optional<Recommendation> findLatestUnresolvedByUserId(Long userId) {
        return jpa.findUnresolvedByUserId(userId).stream().findFirst();
    }

    @Override
    public List<Recommendation> findAllByUserId(Long userId) {
        return jpa.findByUserIdOrderByTriggeredAtDesc(userId);
    }
}
