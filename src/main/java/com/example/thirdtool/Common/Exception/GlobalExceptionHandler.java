package com.example.thirdtool.Common.Exception;


import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex,
                                                        HttpServletRequest req) {

        ErrorCode ec = ex.getErrorCode();

        return ResponseEntity.status(ec.getStatus())
                             .body(new ErrorResponse(ec.getCode(), ec.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                             .body(new ErrorResponse(ErrorCode.INVALID_INPUT.getCode(),
                                     ErrorCode.INVALID_INPUT.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
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