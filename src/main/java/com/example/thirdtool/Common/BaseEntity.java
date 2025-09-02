package com.example.thirdtool.Common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // 여러 엔티티에서 공통적으로 사용하는 필드를 정의할 때 사용
@EntityListeners(AuditingEntityListener.class) // ✅ Auditing 기능을 활성화
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false) // 생성 시간은 수정되지 않도록 설정
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;
}