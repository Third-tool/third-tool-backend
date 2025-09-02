package com.example.thirdtool.User.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "users")
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

    @Builder(builderMethodName = "internalBuilder")
    private User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // ✅ 회원가입에 사용할 정적 팩토리 메서드
    public static User of(String email, String password) {
        // 실제 서비스에서는 비밀번호 해싱 로직이 이곳에 위치해야 합니다.
        // String encodedPassword = PasswordUtil.hashPassword(password);
        // return internalBuilder().email(email).password(encodedPassword).build();
        return internalBuilder().email(email).password(password).build();
    }
}