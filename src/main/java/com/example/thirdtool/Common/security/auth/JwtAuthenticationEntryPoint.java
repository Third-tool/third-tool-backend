package com.example.thirdtool.Common.security.auth;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 인증 실패 시 응답을 GlobalExceptionHandler와 동일한 {code, message, path, timestamp} 형식으로 통일한다 (Story 3-1).
 *
 * JWTFilter는 인증 실패 사유를 request attribute "auth.error" 에 ErrorCode로 저장하고
 * 본 EntryPoint가 그 ErrorCode를 꺼내 응답을 작성한다. attribute가 없으면 기본 UNAUTHORIZED.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    public static final String ERROR_CODE_ATTRIBUTE = "auth.error";

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorCode errorCode = resolveErrorCode(request, authException);

        log.debug("[AUTH-ENTRY] {} → {} {}", request.getRequestURI(), errorCode.getCode(), errorCode.getMessage());

        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", errorCode.getCode());
        body.put("message", errorCode.getMessage());
        body.put("path", request.getRequestURI());
        body.put("timestamp", OffsetDateTime.now().toString());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request, AuthenticationException ex) {
        Object attr = request.getAttribute(ERROR_CODE_ATTRIBUTE);
        if (attr instanceof ErrorCode ec) {
            return ec;
        }
        if (ex != null && ex.getCause() instanceof BusinessException be) {
            return be.getErrorCode();
        }
        return ErrorCode.UNAUTHORIZED;
    }
}
