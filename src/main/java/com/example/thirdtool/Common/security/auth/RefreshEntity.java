package com.example.thirdtool.Common.security.auth;

import com.example.thirdtool.Common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "jwt_refresh_entity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /**
     * Story 3-1: RT 원문이 로그·디버거 출력에 노출되지 않도록 방어. JwtService 회전 경로에서
     * RT 원문이 출력되어야 한다면 {@code SensitiveLogMasker.maskToken(...)}를 거친다.
     */
    @ToString.Exclude
    @Column(name = "refresh", nullable = false, length = 512)
    private String refresh;

    @Builder
    private RefreshEntity(Long id, String username, String refresh) {
        this.id = id;
        this.username = username;
        this.refresh = refresh;
    }

    /**
     * 정적 팩토리 — 신규 발급용 (id=null이라 JPA가 insert로 처리).
     */
    public static RefreshEntity ofNew(String username, String refresh) {
        return RefreshEntity.builder()
                            .username(username)
                            .refresh(refresh)
                            .build();
    }

    /**
     * 기존 엔티티에 새 RT 값을 적용한다 (dirty checking 통한 UPDATE).
     * username 변경 금지 — 발급된 RT 토큰의 sub claim과 일치해야 한다.
     */
    public void updateRefresh(String newRefreshToken) {
        if (newRefreshToken == null || newRefreshToken.isBlank()) {
            throw new IllegalArgumentException("새 Refresh Token 값은 비어 있을 수 없습니다.");
        }
        this.refresh = newRefreshToken;
    }
}
