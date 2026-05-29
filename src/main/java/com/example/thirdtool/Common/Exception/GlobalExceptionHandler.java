package com.example.thirdtool.Common.Exception;


import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                        HttpServletRequest req) {

        ErrorCode ec = ex.getErrorCode();

        return ResponseEntity
                .status(ec.getStatus())
                .body(new ErrorResponse(
                        ec.getCode(),
                        ex.getMessage(),   // detail 포함된 메시지 사용
                        req.getRequestURI(),
                        OffsetDateTime.now()
                ));
    }

    //파일 관련 예외 처리
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxSizeException(MaxUploadSizeExceededException e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "파일 용량 초과");
        response.put("message", "허용된 최대 업로드 크기를 초과했습니다.");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(),
                                     ErrorCode.INVALID_INPUT.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
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
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("[AccessDenied] {} {} - {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        ErrorCode ec = ErrorCode.AUTH_FORBIDDEN;
        return ResponseEntity.status(ec.getStatus())
                             .body(new ErrorResponse(ec.getCode(), ec.getMessage(),
                                                     req.getRequestURI(), OffsetDateTime.now()));
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