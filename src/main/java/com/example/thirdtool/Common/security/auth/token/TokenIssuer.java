package com.example.thirdtool.Common.security.auth.token;

import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 토큰 발급 단일 진입점.
 *
 * 모든 로그인 경로(자체/소셜)와 토큰 재발급 경로가 본 인터페이스를 통해
 * AT(HttpOnly Cookie) + RT(응답 바디) 한 쌍을 일관되게 발급한다.
 *
 * Story 1-2 진입과 함께 JWTUtil 직접 호출은 본 구현체 한 곳으로 수렴한다.
 */
public interface TokenIssuer {

    /**
     * 새 AT/RT 한 쌍을 발급하고, AT는 응답의 Set-Cookie 헤더에, RT는 반환값으로 전달한다.
     *
     * @param user     발급 대상 사용자
     * @param response AT Cookie 발급용 HTTP 응답
     * @return 응답 바디로 반환할 Refresh Token 문자열
     */
    String issue(UserEntity user, HttpServletResponse response);

    /**
     * 토큰 재발급 경로용. RT claim에서 추출한 username/role로 새 AT/RT 한 쌍을 발급한다.
     * UserEntity 재조회를 피해 refresh path의 추가 DB hit를 차단한다.
     *
     * @param username RT의 sub claim
     * @param role     RT의 role claim
     * @param response AT Cookie 발급용 HTTP 응답
     * @return 응답 바디로 반환할 Refresh Token 문자열
     */
    String reissue(String username, String role, HttpServletResponse response);
}
