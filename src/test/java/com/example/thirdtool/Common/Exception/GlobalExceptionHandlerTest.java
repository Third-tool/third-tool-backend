package com.example.thirdtool.Common.Exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler 로그 레벨 분리")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger handlerLogger;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest("GET", "/api/v1/test");

        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);

        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(logAppender);
        logAppender.stop();
        MDC.clear();
    }

    @Test
    @DisplayName("BusinessException은 WARN 레벨 + stack trace 미포함 + MDC errorCode 주입")
    void BusinessException은_WARN_레벨_stack_미포함_MDC_errorCode_주입() {
        BusinessException ex = new BusinessException(ErrorCode.CARD_NOT_FOUND);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBusiness(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.CARD_NOT_FOUND.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.CARD_NOT_FOUND.getCode());
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isEqualTo(ErrorCode.CARD_NOT_FOUND.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getThrowableProxy()).as("WARN은 stack trace 미포함").isNull();
        assertThat(event.getFormattedMessage()).contains("business.exception");
        assertThat(event.getFormattedMessage()).contains(ErrorCode.CARD_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException은 INFO 레벨 + MDC INVALID_INPUT 주입")
    void MethodArgumentNotValidException은_INFO_레벨_MDC_INVALID_INPUT() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(Collections.emptyList());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT.getCode());
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isEqualTo(ErrorCode.INVALID_INPUT.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getThrowableProxy()).isNull();
        assertThat(event.getFormattedMessage()).contains("validation.failed");
    }

    @Test
    @DisplayName("ConstraintViolationException은 INFO 레벨 + MDC INVALID_INPUT 주입")
    void ConstraintViolationException은_INFO_레벨_MDC_INVALID_INPUT() {
        ConstraintViolationException ex = new ConstraintViolationException("constraint failed", Collections.emptySet());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT.getCode());
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isEqualTo(ErrorCode.INVALID_INPUT.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("validation.constraint.violation");
    }

    @Test
    @DisplayName("AuthenticationException은 INFO 레벨 + MDC UNAUTHORIZED 주입 (빈번한 정상 분기)")
    void AuthenticationException은_INFO_레벨_MDC_UNAUTHORIZED() {
        BadCredentialsException ex = new BadCredentialsException("invalid");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAuthentication(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("authentication.failed");
    }

    @Test
    @DisplayName("AccessDeniedException은 WARN 레벨 + MDC AUTH_FORBIDDEN (보안 추적 가치)")
    void AccessDeniedException은_WARN_레벨_MDC_AUTH_FORBIDDEN() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN.getCode());
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isEqualTo(ErrorCode.AUTH_FORBIDDEN.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("access.denied");
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException은 INFO 레벨 + MDC PAYLOAD_TOO_LARGE")
    void MaxUploadSizeExceededException은_INFO_레벨_MDC_PAYLOAD_TOO_LARGE() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10_000_000L);

        ResponseEntity<Map<String, String>> response = handler.handleMaxSizeException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isEqualTo("PAYLOAD_TOO_LARGE");

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("upload.size.exceeded");
    }

    @Test
    @DisplayName("fallback Exception은 ERROR 레벨 + stack trace 포함 + MDC INTERNAL_ERROR")
    void fallback_Exception은_ERROR_레벨_stack_포함_MDC_INTERNAL_ERROR() {
        NullPointerException ex = new NullPointerException("boom");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleUnknown(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo(GlobalExceptionHandler.INTERNAL_ERROR_CODE);
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isEqualTo(GlobalExceptionHandler.INTERNAL_ERROR_CODE);

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getThrowableProxy())
                .as("fallback은 stack trace 포함")
                .isNotNull();
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(NullPointerException.class.getName());
        assertThat(event.getFormattedMessage()).contains("unexpected.exception");
    }

    @Test
    @DisplayName("BusinessException의 ex.getMessage()는 응답 body의 message에 사용된다 (detail 보존)")
    void BusinessException_message는_응답_body에_사용된다() {
        BusinessException ex = BusinessException.withDetail(ErrorCode.CARD_NOT_FOUND, "id=42");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBusiness(ex, request);

        assertThat(response.getBody().getMessage()).contains(ErrorCode.CARD_NOT_FOUND.getMessage());
        assertThat(response.getBody().getMessage()).contains("id=42");
    }

    private ILoggingEvent singleEvent() {
        assertThat(logAppender.list)
                .as("핸들러 호출 후 로그 이벤트가 정확히 1건 캡처되어야 한다")
                .hasSize(1);
        return logAppender.list.get(0);
    }
}
