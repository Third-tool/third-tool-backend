package com.example.thirdtool.User.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 자체 로그인 사용자 정보 수정 요청 (Story-5-4 정리).
 *
 * <p>수정 대상자는 요청 바디가 아닌 {@code @AuthenticationPrincipal UserEntity currentUser}로 결정한다.
 * 따라서 {@code username}/{@code password} 필드는 본 DTO에 두지 않는다 — 타인 username을 바디로
 * 넣어 타 계정 수정 시도 표면을 원천 차단. 비밀번호 변경은 후속 별도 endpoint 도입 시점에 신설.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDTO {

    private String nickname;

    @Email
    private String email;
}
