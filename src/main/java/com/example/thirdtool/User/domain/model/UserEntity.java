package com.example.thirdtool.User.domain.model;

import com.example.thirdtool.LegacyCard.Card.domain.model.CardRank;
import com.example.thirdtool.Common.BaseEntity;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.User.infrastructure.Naver.NaverMember;
import com.example.thirdtool.User.infrastructure.kakao.KakaoMember;
import com.example.thirdtool.User.dto.UserUpdateRequestDTO;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "user_entity")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, updatable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "is_lock", nullable = false)
    private Boolean isLock;

    @Column(name = "is_social", nullable = false)
    private Boolean isSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider_type")
    private SocialProviderType socialProviderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private UserRoleType roleType;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "email")
    private String email;

    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;

    // ✅ 연관관계 (소셜 로그인 전용)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KakaoMember> kakaoMembers = new ArrayList<>();

    // ✅ 연관관계 (소셜 로그인 전용)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NaverMember> naverMembers = new ArrayList<>();

    // ✅ CardRankService의 createDefaultRanksForUser() 메서드에서 사용하기 위해
    //    양방향 연관 관계를 설정합니다.
    // 당연히 리스트라고 매핑이 바로되는거 아님
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardRank> cardRanks = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<Deck> decks = new ArrayList<>();

    public void updateUser(UserUpdateRequestDTO dto) {
        this.email = dto.getEmail();
        this.nickname = dto.getNickname();
    }

    //internalBuilder + private 생성자
    @Builder(builderMethodName = "internalBuilder")
    private UserEntity(String username,
                       String password,
                       Boolean isLock,
                       Boolean isSocial,
                       SocialProviderType socialProviderType,
                       UserRoleType roleType,
                       String nickname,
                       String email) {
        this.username = username;
        this.password = password;
        this.isLock = isLock;
        this.isSocial = isSocial;
        this.socialProviderType = socialProviderType;
        this.roleType = roleType;
        this.nickname = nickname;
        this.email = email;
    }

    // 🔑 정적 팩토리 메서드 추가- 전체에 대한 기본 생성자
    public static UserEntity of(String username,
                                String password,
                                Boolean isLock,
                                Boolean isSocial,
                                SocialProviderType socialProviderType,
                                UserRoleType roleType,
                                String nickname,
                                String email) {
        return internalBuilder()
                .username(username)
                .password(password)
                .isLock(isLock)
                .isSocial(isSocial)
                .socialProviderType(socialProviderType)
                .roleType(roleType)
                .nickname(nickname)
                .email(email)
                .build();
    }

    // 2) 자체 로그인 회원가입 (일반 가입용)
    public static UserEntity ofLocal(String username,
                                     String encodedPassword,
                                     String nickname,
                                     String email) {
        return internalBuilder()
                .username(username)
                .password(encodedPassword)
                .isLock(false)              // 신규 계정은 기본적으로 잠금 해제
                .isSocial(false)            // 자체 로그인 계정
                .socialProviderType(null)   // 자체 로그인은 소셜 제공자 없음
                .roleType(UserRoleType.USER)
                .nickname(nickname)
                .email(email)
                .build();
    }

    // ✅ 정적 팩토리 메서드 (소셜 로그인 가입용)
    public static UserEntity ofSocial(String username,
                                      SocialProviderType socialProviderType,
                                      String nickname,
                                      String email) {
        return internalBuilder()
                .username(username)
                .password("SOCIAL_USER")  // 더미 패스워드
                .isLock(false)
                .isSocial(true)
                .socialProviderType(socialProviderType)
                .roleType(UserRoleType.USER)
                .nickname(nickname)
                .email(email)
                .build();
    }


}