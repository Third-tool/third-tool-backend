package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "learning_facade")
public class LearningFacade {

    // ─── 식별자 ───────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_facade_id")
    private Long id;

    // ─── 소유 사용자 ──────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity user;

    // ─── 직업적 컨셉 ──────────────────────────────────────
    @Column(name = "concept", nullable = false, length = 100)
    private String concept;

    // ─── 시각 ─────────────────────────────────────────────
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private LearningFacade(UserEntity user, String concept) {
        this.user    = user;
        this.concept = concept;
    }

    // ─── 생성 ─────────────────────────────────────────────

    public static LearningFacade create(UserEntity user, String concept) {
        requireNonNull(user, "user");
        validateConcept(concept);

        return new LearningFacade(user, concept.trim());
    }

    // ─── 행위 ─────────────────────────────────────────────

    public ConceptChangeRecord updateConcept(String newConcept) {
        validateConcept(newConcept);

        String trimmed = newConcept.trim();

        if (this.concept.equals(trimmed)) {
            return ConceptChangeRecord.unchanged(this.concept); // 동일 값 — 변경 없음
        }

        String previous = this.concept;
        this.concept    = trimmed;
        return ConceptChangeRecord.changed(previous, this.concept);
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    // ─── 내부 검증 ────────────────────────────────────────

    private static void validateConcept(String concept) {
        if (concept == null || concept.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_CONCEPT_BLANK);
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은(는) null일 수 없습니다."
                                                  );
        }
    }
}