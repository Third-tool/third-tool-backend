package com.example.thirdtool.Common.Exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-5-3: GlobalExceptionHandler.handleAccessDenied() 단위 검증.
 *
 * <p>Spring Security의 {@link AccessDeniedException}이 Controller에서 throw됐을 때
 * AUTH_FORBIDDEN(AUTH005, FORBIDDEN) ErrorCode 응답으로 변환되는지 확인. 기존
 * SocialLoginControllerExceptionSliceTest의 패턴(handler 직접 인스턴스화 +
 * MockHttpServletRequest)을 답습.
 */
class GlobalExceptionHandlerAccessDeniedTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAccessDenied_403_AUTH005_응답() {
        AccessDeniedException ex = new AccessDeniedException("본인 혹은 관리자만 삭제할 수 있습니다.");
        HttpServletRequest req = new MockHttpServletRequest("DELETE", "/user");

        ResponseEntity<?> response = handler.handleAccessDenied(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Object body = response.getBody();
        assertThat(body).isNotNull();
        assertThat((String) ReflectionTestUtils.invokeMethod(body, "getCode")).isEqualTo("AUTH005");
        assertThat((String) ReflectionTestUtils.invokeMethod(body, "getMessage"))
                .isEqualTo("접근 권한이 없습니다.");
        assertThat((String) ReflectionTestUtils.invokeMethod(body, "getPath")).isEqualTo("/user");
    }

    @Test
    void handleAccessDenied_응답메시지가_ErrorCode_enum값으로_통일() {
        // 보안 정책: Controller에서 던진 구체 메시지는 응답에 echo하지 않고 enum 메시지로 통일.
        AccessDeniedException ex = new AccessDeniedException("매우 구체적인 거부 사유 (서버 로그에만 기록)");
        HttpServletRequest req = new MockHttpServletRequest("PUT", "/user");

        ResponseEntity<?> response = handler.handleAccessDenied(ex, req);

        Object body = response.getBody();
        String message = (String) ReflectionTestUtils.invokeMethod(body, "getMessage");
        assertThat(message)
                .isEqualTo("접근 권한이 없습니다.")
                .doesNotContain("구체적인 거부 사유");
    }
}
