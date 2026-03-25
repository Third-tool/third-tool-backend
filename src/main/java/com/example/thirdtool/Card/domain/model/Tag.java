package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_tag_value", columnNames = "value")
)
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "value", nullable = false, length = 50)
    private String value;

    /** JPA 전용 기본 생성자. 외부에서 직접 사용 금지. */
    protected Tag() {}

    private Tag(String value) {
        this.value = value;
    }

    /**
     * 태그를 생성한다.
     * value는 trim 처리 후 저장한다. - 전처리 과정
     * null 또는 blank이면 생성할 수 없다.
     */
    public static Tag of(String value) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isBlank()) {
            throw CardDomainException.of(ErrorCode.TAG_VALUE_BLANK);
        }
        return new Tag(trimmed);
    }

    private static String trim(String v) {
        return v == null ? null : v.trim();
    }
}