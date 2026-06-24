package com.example.thirdtool.Common.Exception;


import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;

/**
 * 전역 예외 핸들러 — 예외 유형별 로그 레벨을 명시 분리한다 (Story 3-1, ADR011 참조).
 *
 * <p>매핑 표:
 * <ul>
 *   <li>{@link BusinessException} → WARN, stack trace 미포함 (예상 가능한 비정상)</li>
 *   <li>{@link MethodArgumentNotValidException}, {@link ConstraintViolationException} → INFO (4xx 검증)</li>
 *   <li>{@link AuthenticationException} → INFO (Controller 내부 throw 케이스. 정상 경로는 JwtAuthenticationEntryPoint)</li>
 *   <li>{@link AccessDeniedException} → WARN (보안 추적 가치 — Story 5-3 결정 유지)</li>
 *   <li>{@link MaxUploadSizeExceededException} → INFO (4xx 검증의 일종)</li>
 *   <li>{@link Exception} fallback → ERROR + stack trace (예상치 못한 5xx)</li>
 * </ul>
 *
 * <p>핸들러 진입 직후 {@code MDC.put("errorCode", ...)}로 로그 수집기의 집계 키를 주입하고,
 * {@code try-finally}에서 {@link MDC#remove(String)}로 정리한다. {@code MdcLoggingFilter}의
 * {@code MDC.clear()}가 단일 책임이지만, 비-HTTP 경로(@Scheduled·@Async·테스트)에서도
 * 누수가 없도록 핸들러 자체에서 즉시 정리.
 *
 * <p>응답 {@code message}는 {@code ErrorCode} enum 정적 메시지만 노출한다 — detail은 로그에만.
 * 사용자 입력값이 detail로 흘러들어와 응답 body로 reflected되는 정보 누출을 차단.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String MDC_ERROR_CODE = "errorCode";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                        HttpServletRequest req) {
        ErrorCode ec = ex.getErrorCode();
        try {
            MDC.put(MDC_ERROR_CODE, ec.getCode());
            log.warn("business.exception code={} message={}", ec.getCode(), ex.getMessage());
            return errorResponse(ec, req);
        } finally {
            MDC.remove(MDC_ERROR_CODE);
        }
    }

    //파일 관련 예외 처리
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(MaxUploadSizeExceededException ex,
                                                                HttpServletRequest req) {
        try {
            MDC.put(MDC_ERROR_CODE, ErrorCode.PAYLOAD_TOO_LARGE.getCode());
            log.info("upload.size.exceeded maxSize={}", ex.getMaxUploadSize());
            return errorResponse(ErrorCode.PAYLOAD_TOO_LARGE, req);
        } finally {
            MDC.remove(MDC_ERROR_CODE);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        try {
            MDC.put(MDC_ERROR_CODE, ErrorCode.INVALID_INPUT.getCode());
            log.info("validation.failed errors={}", ex.getBindingResult().getAllErrors().size());
            return errorResponse(ErrorCode.INVALID_INPUT, req);
        } finally {
            MDC.remove(MDC_ERROR_CODE);
        }
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest req) {
        try {
            MDC.put(MDC_ERROR_CODE, ErrorCode.INVALID_INPUT.getCode());
            log.info("validation.constraint.violation violations={}", ex.getConstraintViolations().size());
            return errorResponse(ErrorCode.INVALID_INPUT, req);
        } finally {
            MDC.remove(MDC_ERROR_CODE);
        }
    }

    /**
     * Controller 내부에서 직접 throw된 {@link AuthenticationException} 처리용.
     * Spring Security 필터 단의 인증 실패는 {@code JwtAuthenticationEntryPoint}가 처리하므로
     * 본 핸들러는 보안망(fallback) 역할. 두 경로 모두 INFO + MDC errorCode 정책.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.UNAUTHORIZED;
        try {
            MDC.put(MDC_ERROR_CODE, ec.getCode());
            log.info("authentication.failed type={} message={}", ex.getClass().getSimpleName(), ex.getMessage());
            return errorResponse(ec, req);
        } finally {
            MDC.remove(MDC_ERROR_CODE);
        }
    }

    /**
     * Story-5-3: Controller에서 throw한 {@link AccessDeniedException}을 ErrorCode 기반 JSON 응답으로 변환.
     *
     * <p><strong>주의 — 처리 범위 분리</strong>:
     * <ul>
     *   <li>Controller throw → 본 핸들러가 {code:"AUTH005", message, path, timestamp} JSON 응답</li>
     *   <li>Spring Security 필터 체인 throw (예: SecurityConfig의 `hasRole(...)` 미충족)
     *       → {@code SecurityConfig.accessDeniedHandler}가 처리 (현재는 빈 body 403,
     *       후속 Story에서 본 핸들러 응답 형식과 통일 예정)</li>
     * </ul>
     *
     * <p>로그 레벨은 WARN — 권한 거부는 4xx지만 보안 추적 가치가 높아 INFO보다 격상 (Epic 3 결정).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.AUTH_FORBIDDEN;
        try {
            MDC.put(MDC_ERROR_CODE, ec.getCode());
            log.warn("access.denied method={} path={} detail={}", req.getMethod(), req.getRequestURI(), ex.getMessage());
            return errorResponse(ec, req);
        } finally {
            MDC.remove(MDC_ERROR_CODE);
        }
    }

    /**
     * fallback — 명시 매핑되지 않은 모든 예외. ERROR 레벨 + stack trace.
     * Product 1의 알림(v2) 채널 구독 대상은 본 핸들러가 출력하는 ERROR 로그.
     * 신규 예외 추가 시 본 fallback에 의존하지 말고 명시 핸들러를 추가 (Epic 3 결정).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        try {
            MDC.put(MDC_ERROR_CODE, ec.getCode());
            log.error("unexpected.exception type={}", ex.getClass().getName(), ex);
            return errorResponse(ec, req);
        } finally {
            MDC.remove(MDC_ERROR_CODE);
        }
    }

    /**
     * 모든 핸들러의 응답 빌더 — 응답 message는 ErrorCode enum 정적 메시지로 고정.
     * detail(BusinessException.withDetail의 인자)은 로그에만 남고 응답 body에 노출되지 않는다.
     */
    private ResponseEntity<ErrorResponse> errorResponse(ErrorCode ec, HttpServletRequest req) {
        return ResponseEntity.status(ec.getStatus())
                             .body(new ErrorResponse(
                                     ec.getCode(),
                                     ec.getMessage(),
                                     req.getRequestURI(),
                                     OffsetDateTime.now()));
    }

    @Getter
    @AllArgsConstructor
    static class ErrorResponse {
        private String code;
        private String message;
        private String path;
        private OffsetDateTime timestamp;
    }
}
