package com.example.thirdtool.User.domain.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@NoArgsConstructor
@DiscriminatorColumn(name = "provider_type")
public abstract class SocialMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    protected UserEntity user;

    @Column(nullable = false, unique = true)
    protected String socialId;

    public abstract SocialProviderType getProviderType();

    @Column(name = "refresh_token")
    protected String refreshToken;

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    protected SocialMember(UserEntity user, String socialId, String refreshToken) {
        this.user = user;
        this.socialId = socialId;
        this.refreshToken = refreshToken;
    }
}