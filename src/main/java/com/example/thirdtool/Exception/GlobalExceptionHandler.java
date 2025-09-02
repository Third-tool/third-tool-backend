package com.example.thirdtool.Exception;


import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // IllegalArgumentException을 처리하는 핸들러
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        // 예외 메시지를 받아와서 클라이언트에게 반환할 JSON 응답을 생성
        Map<String, String> errorResponse = Map.of("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    // ✅ JPA의 DataIntegrityViolationException 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        String message = "같은 단어로 덱을 만들 수 없습니다!!";

        // 실제 원인에 따라 더 구체적인 메시지를 전달할 수 있음
        if (ex.getCause() instanceof SQLIntegrityConstraintViolationException) {
            message = "같은 단어로 덱을 만들 수 없습니다!!";
        }

        Map<String, String> errorResponse = Map.of("error", message);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
}
