package com.example.thirdtool.Common.Exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.validation.ConstraintViolation;
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
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @DisplayName("BusinessException은 WARN 레벨 + stack trace 미포함 + 응답에 ErrorCode 정적 메시지만 노출")
    void BusinessException은_WARN_레벨_stack_미포함_응답에_ErrorCode_정적_메시지만_노출() {
        BusinessException ex = new BusinessException(ErrorCode.CARD_NOT_FOUND);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBusiness(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.CARD_NOT_FOUND.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.CARD_NOT_FOUND.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.CARD_NOT_FOUND.getMessage());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getThrowableProxy()).as("WARN은 stack trace 미포함").isNull();
        assertThat(event.getFormattedMessage()).contains("business.exception");
        assertThat(event.getFormattedMessage()).contains(ErrorCode.CARD_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("BusinessException.withDetail의 detail은 로그에만 남고 응답에는 노출되지 않는다 (input reflection 차단)")
    void BusinessException_detail은_로그에만_남고_응답에는_노출되지_않는다() {
        String userInput = "id=42-or-sensitive-leak";
        BusinessException ex = BusinessException.withDetail(ErrorCode.CARD_NOT_FOUND, userInput);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleBusiness(ex, request);

        assertThat(response.getBody().getMessage())
                .as("응답 message는 ErrorCode 정적 메시지만")
                .isEqualTo(ErrorCode.CARD_NOT_FOUND.getMessage())
                .doesNotContain(userInput);

        ILoggingEvent event = singleEvent();
        assertThat(event.getFormattedMessage())
                .as("로그에는 detail까지 포함된 ex.getMessage() 노출")
                .contains(userInput);
    }

    @Test
    @DisplayName("MethodArgumentNotValidException은 INFO 레벨 + 응답 INVALID_INPUT")
    void MethodArgumentNotValidException은_INFO_레벨_응답_INVALID_INPUT() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors())
                .thenReturn(List.of(new ObjectError("dto", "violation1"), new ObjectError("dto", "violation2")));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("validation.failed").contains("errors=2");
    }

    @Test
    @DisplayName("ConstraintViolationException은 INFO 레벨 + 다중 violations 크기 로그 포함")
    void ConstraintViolationException은_INFO_레벨_다중_violations_크기_로그() {
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        violations.add(mock(ConstraintViolation.class));
        violations.add(mock(ConstraintViolation.class));
        ConstraintViolationException ex = new ConstraintViolationException("constraint failed", violations);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleConstraintViolation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("violations=2");
    }

    @Test
    @DisplayName("AuthenticationException(BadCredentials)은 INFO 레벨 + 응답 UNAUTHORIZED")
    void AuthenticationException_BadCredentials은_INFO_레벨() {
        BadCredentialsException ex = new BadCredentialsException("invalid");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAuthentication(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("authentication.failed");
        assertThat(event.getFormattedMessage()).contains("BadCredentialsException");
    }

    @Test
    @DisplayName("AuthenticationException 하위 타입(InsufficientAuthentication)도 동일 INFO 처리 + 타입명 로그")
    void AuthenticationException_InsufficientAuthentication도_INFO_타입명_로그() {
        InsufficientAuthenticationException ex = new InsufficientAuthenticationException("auth required");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAuthentication(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getStatus());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("InsufficientAuthenticationException");
    }

    @Test
    @DisplayName("AccessDeniedException은 WARN 레벨 + 응답 AUTH_FORBIDDEN")
    void AccessDeniedException은_WARN_레벨_응답_AUTH_FORBIDDEN() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleAccessDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN.getStatus());
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.AUTH_FORBIDDEN.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).contains("access.denied");
    }

    @Test
    @DisplayName("MaxUploadSizeExceededException은 INFO 레벨 + ErrorResponse 포맷으로 통일 + PAYLOAD_TOO_LARGE 응답")
    void MaxUploadSizeExceededException은_INFO_레벨_ErrorResponse_포맷_PAYLOAD_TOO_LARGE() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10_000_000L);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleMaxSizeException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE.getMessage());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("upload.size.exceeded");
    }

    @Test
    @DisplayName("fallback Exception은 ERROR 레벨 + stack trace 포함 + INTERNAL_ERROR 응답")
    void fallback_Exception은_ERROR_레벨_stack_포함_INTERNAL_ERROR() {
        NullPointerException ex = new NullPointerException("boom");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleUnknown(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR.getCode());

        ILoggingEvent event = singleEvent();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getThrowableProxy()).as("fallback은 stack trace 포함").isNotNull();
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(NullPointerException.class.getName());
        assertThat(event.getFormattedMessage()).contains("unexpected.exception");
    }

    @Test
    @DisplayName("모든 핸들러는 try-finally로 MDC.errorCode를 즉시 정리한다 (비-HTTP 경로 누수 차단)")
    void 모든_핸들러는_try_finally로_MDC_errorCode를_즉시_정리한다() {
        handler.handleBusiness(new BusinessException(ErrorCode.CARD_NOT_FOUND), request);
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isNull();

        handler.handleAccessDenied(new AccessDeniedException("x"), request);
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isNull();

        handler.handleAuthentication(new BadCredentialsException("x"), request);
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isNull();

        handler.handleUnknown(new NullPointerException("x"), request);
        assertThat(MDC.get(GlobalExceptionHandler.MDC_ERROR_CODE)).isNull();
    }

    private ILoggingEvent singleEvent() {
        assertThat(logAppender.list)
                .as("핸들러 호출 후 로그 이벤트가 정확히 1건 캡처되어야 한다")
                .hasSize(1);
        return logAppender.list.get(0);
    }
}
