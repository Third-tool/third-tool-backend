package com.example.thirdtool.Common.Exception;


import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 핸들러 — 예외 유형별 로그 레벨을 명시 분리한다 (Story 3-1, ADR011 참조).
 *
 * <p>매핑 표:
 * <ul>
 *   <li>{@link BusinessException} → WARN, stack trace 미포함 (예상 가능한 비정상)</li>
 *   <li>{@link MethodArgumentNotValidException}, {@link ConstraintViolationException} → INFO (4xx 검증)</li>
 *   <li>{@link AuthenticationException} → INFO (인증 실패는 빈번한 정상 분기)</li>
 *   <li>{@link AccessDeniedException} → WARN (보안 추적 가치 — Story 5-3 결정 유지)</li>
 *   <li>{@link MaxUploadSizeExceededException} → INFO (4xx 검증의 일종)</li>
 *   <li>{@link Exception} fallback → ERROR + stack trace (예상치 못한 5xx)</li>
 * </ul>
 *
 * <p>핸들러 진입 직후 {@code MDC.put("errorCode", ...)}로 로그 수집기의 집계 키를 주입한다.
 * MDC clear는 {@code MdcLoggingFilter.finally}가 책임진다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String MDC_ERROR_CODE = "errorCode";
    static final String INTERNAL_ERROR_CODE = "INTERNAL_ERROR";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                        HttpServletRequest req) {
        ErrorCode ec = ex.getErrorCode();
        MDC.put(MDC_ERROR_CODE, ec.getCode());
        log.warn("business.exception code={} message={}", ec.getCode(), ex.getMessage());
        return ResponseEntity
                .status(ec.getStatus())
                .body(new ErrorResponse(
                        ec.getCode(),
                        ex.getMessage(),
                        req.getRequestURI(),
                        OffsetDateTime.now()
                ));
    }

    //파일 관련 예외 처리
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException e) {
        MDC.put(MDC_ERROR_CODE, "PAYLOAD_TOO_LARGE");
        log.info("upload.size.exceeded maxSize={}", e.getMaxUploadSize());
        Map<String, String> response = new HashMap<>();
        response.put("error", "파일 용량 초과");
        response.put("message", "허용된 최대 업로드 크기를 초과했습니다.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        MDC.put(MDC_ERROR_CODE, ErrorCode.INVALID_INPUT.getCode());
        log.info("validation.failed errors={}", ex.getBindingResult().getAllErrors().size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(),
                                     ErrorCode.INVALID_INPUT.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest req) {
        MDC.put(MDC_ERROR_CODE, ErrorCode.INVALID_INPUT.getCode());
        log.info("validation.constraint.violation violations={}", ex.getConstraintViolations().size());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(),
                                     ErrorCode.INVALID_INPUT.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
    }

    /**
     * Spring Security의 {@link AuthenticationException} — 토큰 없음·만료·서명 불일치 등.
     * 인증 실패는 빈번한 정상 분기이므로 INFO. stack trace 미포함.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.UNAUTHORIZED;
        MDC.put(MDC_ERROR_CODE, ec.getCode());
        log.info("authentication.failed type={} message={}", ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity.status(ec.getStatus())
                             .body(new ErrorResponse(ec.getCode(), ec.getMessage(),
                                                     req.getRequestURI(), OffsetDateTime.now()));
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
     * <p>응답 메시지는 보안상 ErrorCode enum 메시지("접근 권한이 없습니다.")로 통일하며,
     * Controller에서 던진 구체 메시지는 서버 로그에만 기록.
     *
     * <p>로그 레벨은 WARN — 권한 거부는 4xx지만 보안 추적 가치가 높아 INFO보다 격상
     * (Epic 3 결정).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ErrorCode ec = ErrorCode.AUTH_FORBIDDEN;
        MDC.put(MDC_ERROR_CODE, ec.getCode());
        log.warn("access.denied method={} path={} detail={}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(ec.getStatus())
                             .body(new ErrorResponse(ec.getCode(), ec.getMessage(),
                                                     req.getRequestURI(), OffsetDateTime.now()));
    }

    /**
     * fallback — 명시 매핑되지 않은 모든 예외. ERROR 레벨 + stack trace.
     * Product 1의 알림(v2) 채널 구독 대상은 본 핸들러가 출력하는 ERROR 로그.
     * 신규 예외 추가 시 본 fallback에 의존하지 말고 명시 핸들러를 추가 (Epic 3 결정).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex, HttpServletRequest req) {
        MDC.put(MDC_ERROR_CODE, INTERNAL_ERROR_CODE);
        log.error("unexpected.exception type={}", ex.getClass().getName(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                             .body(new ErrorResponse(INTERNAL_ERROR_CODE,
                                     "서버 내부 오류가 발생했습니다.", req.getRequestURI(), OffsetDateTime.now()));
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
