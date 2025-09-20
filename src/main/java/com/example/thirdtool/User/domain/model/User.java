package com.example.thirdtool.User.domain.model;

import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "user_id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // ✅ username을 email로 변경

    @Column(nullable = false)
    private String password;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Deck> decks = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tag> tags = new ArrayList<>();

    @Builder(builderMethodName = "internalBuilder")
    private User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public User(long l, String mail, String number) {
    }

    // ✅ 회원가입에 사용할 정적 팩토리 메서드
    public static User of(String email, String password) {
        // 실제 서비스에서는 비밀번호 해싱 로직이 이곳에 위치해야 합니다.
        // String encodedPassword = PasswordUtil.hashPassword(password);
        // return internalBuilder().email(email).password(encodedPassword).build();
        return internalBuilder().email(email).password(password).build();
    }
}