package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * LearningFacade 소유의 컨셉 값. `learning_facade_concept` 테이블(V16)에 매핑.
 *
 * <p>생성은 정적 팩토리 {@link #of(LearningFacade, String, int)}만 사용 — new 금지.
 * displayOrder는 1-based (테이블 CHECK는 {@code >= 0} 안전망).
 * 값은 항상 trim된 상태로 저장되며 blank/길이 초과는 팩토리에서 거부한다.
 *
 * <p>Story-LT-E1-S2.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "learning_facade_concept")
public class LearningFacadeConcept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_facade_concept_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_facade_id", nullable = false, updatable = false)
    private LearningFacade facade;

    /** 컨셉 값. V16 컬럼 {@code concept_value}. MySQL 예약어 회피 목적. */
    @Column(name = "concept_value", nullable = false, length = LearningFacade.MAX_CONCEPT_VALUE_LENGTH)
    private String value;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LearningFacadeConcept(LearningFacade facade, String value, int displayOrder) {
        this.facade       = facade;
        this.value        = value;
        this.displayOrder = displayOrder;
    }

    /**
     * 정적 팩토리. Aggregate Root({@link LearningFacade})의 행위를 통해서만 호출된다.
     * value는 trim + blank/길이 검증 후 저장. displayOrder는 1-based.
     */
    static LearningFacadeConcept of(LearningFacade facade, String value, int displayOrder) {
        if (facade == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "facade은(는) null일 수 없습니다."
            );
        }
        String normalized = normalizeValue(value);
        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder
            );
        }
        return new LearningFacadeConcept(facade, normalized, displayOrder);
    }

    void updateDisplayOrder(int newOrder) {
        if (newOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. newOrder=" + newOrder
            );
        }
        this.displayOrder = newOrder;
    }

    /**
     * 입력 값 정규화 — trim 후 blank/길이 검증. 실패 시 도메인 예외.
     * package-private: {@link LearningFacade}가 컬렉션 재정렬/치환 시에도 재사용한다.
     */
    static String normalizeValue(String value) {
        if (value == null || value.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_CONCEPT_BLANK);
        }
        String trimmed = value.trim();
        if (trimmed.length() > LearningFacade.MAX_CONCEPT_VALUE_LENGTH) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_FACADE_CONCEPT_TOO_LONG,
                    "value.length=" + trimmed.length()
            );
        }
        return trimmed;
    }
}
