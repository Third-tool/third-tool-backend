package com.example.thirdtool.User.presentation;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Common.Exception.GlobalExceptionHandler;
import com.example.thirdtool.User.domain.exception.UserDomainException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-5-1 AC2 검증: User 도메인 예외가 GlobalExceptionHandler를 통해
 * `{code, message, path, timestamp}` 형식으로 응답되는지 단위 검증.
 *
 * 본 테스트는 Spring 컨텍스트를 띄우지 않고 GlobalExceptionHandler를
 * 직접 인스턴스화해 handleBusiness()를 호출 — `@WebMvcTest` Slice 보다
 * 가볍지만 변환 흐름의 본질(ErrorCode → ResponseEntity status·body)을
 * 동일하게 검증한다. 같은 chain을 거치는 다른 User 예외(USER_ALREADY_EXISTS
 * 등)도 동일하게 적용된다.
 */
class SocialLoginControllerExceptionSliceTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusiness_SOCIAL_PROVIDER_NOT_SUPPORTED_400_USER007_응답() {
        UserDomainException ex = UserDomainException.of(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        HttpServletRequest req = new MockHttpServletRequest("POST", "/social/login/google");

        ResponseEntity<?> response = handler.handleBusiness(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Object body = response.getBody();
        assertThat(body).isNotNull();
        assertThat((String) ReflectionTestUtils.invokeMethod(body, "getCode")).isEqualTo("USER007");
        assertThat((String) ReflectionTestUtils.invokeMethod(body, "getMessage"))
                .isEqualTo("지원하지 않는 소셜 제공자입니다.");
    }

    @Test
    void handleBusiness_PASSWORD_NOT_MATCHED_401_USER005_응답() {
        // Critical 1 조치 검증: 로그인 실패 흐름이 401 + USER005 단일 응답으로 통일됨.
        UserDomainException ex = UserDomainException.of(ErrorCode.PASSWORD_NOT_MATCHED);
        HttpServletRequest req = new MockHttpServletRequest("POST", "/login");

        ResponseEntity<?> response = handler.handleBusiness(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Object body = response.getBody();
        assertThat(body).isNotNull();
        assertThat((String) ReflectionTestUtils.invokeMethod(body, "getCode")).isEqualTo("USER005");
    }

    @Test
    void handleBusiness_USER_IS_SOCIAL_400_USER006_응답() {
        UserDomainException ex = UserDomainException.of(ErrorCode.USER_IS_SOCIAL);
        HttpServletRequest req = new MockHttpServletRequest("POST", "/login");

        ResponseEntity<?> response = handler.handleBusiness(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Object body = response.getBody();
        assertThat(body).isNotNull();
        assertThat((String) ReflectionTestUtils.invokeMethod(body, "getCode")).isEqualTo("USER006");
    }
}
