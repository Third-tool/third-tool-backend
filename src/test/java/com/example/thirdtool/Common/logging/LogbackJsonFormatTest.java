package com.example.thirdtool.Common.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.mock.env.MockEnvironment;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * logback-spring.xml의 profile별 Appender 분기를 Spring 컨텍스트 부팅 없이 검증한다.
 * SpringBootJoranConfigurator + MockEnvironment로 <springProfile> 태그를 정확히 처리하고,
 * ConsoleAppender의 OutputStream을 ByteArrayOutputStream으로 교체해 출력을 캡처한다.
 * ADR008 결정 사항 (profile 분기·LogstashEncoder·MDC 화이트리스트) 회귀 방지용.
 */
class LogbackJsonFormatTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LoggerContext loggerContext;
    private ByteArrayOutputStream capturedOutput;

    @AfterEach
    void resetLogbackContext() {
        MDC.clear();
        if (loggerContext != null) {
            loggerContext.reset();
        }
    }

    @Test
    void prod_profile_로그가_JSON_한_줄로_출력되고_필수_필드가_모두_존재한다() throws Exception {
        configureLogback("prod");
        Logger logger = LoggerFactory.getLogger("c.t.controller.CardController");

        logger.info("Card list fetched");

        String line = readSingleLine();
        JsonNode json = MAPPER.readTree(line);

        assertThat(json.get("@timestamp")).as("@timestamp").isNotNull();
        assertThat(json.get("level").asText()).isEqualTo("INFO");
        assertThat(json.get("logger_name").asText()).isEqualTo("c.t.controller.CardController");
        assertThat(json.get("thread_name")).as("thread_name").isNotNull();
        assertThat(json.get("message").asText()).isEqualTo("Card list fetched");
        assertThat(json.get("application").asText()).isEqualTo("thirdtool");
    }

    @Test
    void dev_profile_로그도_prod와_동일하게_JSON_한_줄로_출력된다() throws Exception {
        configureLogback("dev");
        Logger logger = LoggerFactory.getLogger("c.t.controller.CardController");

        logger.info("Card list fetched");

        JsonNode json = MAPPER.readTree(readSingleLine());
        assertThat(json.get("application").asText()).isEqualTo("thirdtool");
        assertThat(json.get("message").asText()).isEqualTo("Card list fetched");
    }

    @Test
    void local_profile_로그는_JSON이_아닌_평문으로_출력된다() throws Exception {
        configureLogback("local");
        Logger logger = LoggerFactory.getLogger("c.t.controller.CardController");

        logger.info("plain message");

        String captured = capturedOutput.toString(StandardCharsets.UTF_8);
        assertThat(captured).doesNotStartWith("{");
        assertThat(captured).contains("c.t.controller.CardController");
        assertThat(captured).contains("plain message");
        assertThat(captured).contains("INFO");
    }

    @Test
    void prod_profile에서_한글_메시지가_JSON_message_필드에_그대로_보존된다() throws Exception {
        configureLogback("prod");
        Logger logger = LoggerFactory.getLogger("c.t.test");

        logger.info("카드를 찾을 수 없습니다");

        JsonNode json = MAPPER.readTree(readSingleLine());
        assertThat(json.get("message").asText()).isEqualTo("카드를 찾을 수 없습니다");
    }

    @Test
    void prod_profile에서_멀티라인_예외_스택이_한_JSON_라인_안에_이스케이프된다() throws Exception {
        configureLogback("prod");
        Logger logger = LoggerFactory.getLogger("c.t.test");

        logger.error("err occurred", new RuntimeException("boom"));

        String line = readSingleLine();
        assertThat(line.lines().count()).as("한 줄로 직렬화되어야 한다").isEqualTo(1);

        JsonNode json = MAPPER.readTree(line);
        String stackTrace = json.get("stack_trace").asText();
        assertThat(stackTrace).contains("RuntimeException");
        assertThat(stackTrace).contains("boom");
        assertThat(stackTrace).contains("\n");
    }

    @Test
    void prod_profile에서_특수문자가_포함된_logger_이름도_JSON_파싱이_가능하다() throws Exception {
        configureLogback("prod");
        Logger logger = LoggerFactory.getLogger("a$b.Inner");

        logger.info("msg");

        JsonNode json = MAPPER.readTree(readSingleLine());
        assertThat(json.get("logger_name").asText()).isEqualTo("a$b.Inner");
    }

    @Test
    void prod_profile에서_화이트리스트_외_MDC_키는_JSON에_노출되지_않는다() throws Exception {
        configureLogback("prod");
        Logger logger = LoggerFactory.getLogger("c.t.test");
        MDC.put("foo", "bar");

        logger.info("with mdc");

        JsonNode json = MAPPER.readTree(readSingleLine());
        assertThat(json.has("foo")).as("화이트리스트 외 키는 차단되어야 한다").isFalse();
    }

    @Test
    void prod_profile에서_화이트리스트_MDC_키는_JSON에_노출된다() throws Exception {
        configureLogback("prod");
        Logger logger = LoggerFactory.getLogger("c.t.test");
        MDC.put("requestId", "abc-123");

        logger.info("with mdc");

        JsonNode json = MAPPER.readTree(readSingleLine());
        assertThat(json.get("requestId").asText()).isEqualTo("abc-123");
    }

    /**
     * logback-spring.xml을 LogbackLoggingSystem으로 초기화하고, 활성 ConsoleAppender의
     * OutputStream을 ByteArrayOutputStream으로 교체해 한 테스트 내 출력을 캡처한다.
     * 동기 ConsoleAppender 가정 — AsyncAppender 도입 시 캡처 패턴을 ListAppender 기반으로 재설계 필요.
     * Spring Boot 내부 API(LogbackLoggingSystem) 의존 — 메이저 업그레이드 시 호환성 회귀 점검 (ADR008).
     */
    private void configureLogback(String activeProfile) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(activeProfile);
        LoggingInitializationContext initCtx = new LoggingInitializationContext(env);

        LogbackLoggingSystem system = new LogbackLoggingSystem(getClass().getClassLoader());
        system.cleanUp();
        system.beforeInitialize();
        system.initialize(initCtx, "classpath:logback-spring.xml", null);

        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        capturedOutput = new ByteArrayOutputStream();
        ConsoleAppender<ILoggingEvent> appender = findConsoleAppender();
        appender.setOutputStream(capturedOutput);
    }

    @SuppressWarnings("unchecked")
    private ConsoleAppender<ILoggingEvent> findConsoleAppender() {
        ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        Iterator<ch.qos.logback.core.Appender<ILoggingEvent>> it = root.iteratorForAppenders();
        while (it.hasNext()) {
            ch.qos.logback.core.Appender<ILoggingEvent> appender = it.next();
            if (appender instanceof ConsoleAppender<?>) {
                return (ConsoleAppender<ILoggingEvent>) appender;
            }
        }
        throw new IllegalStateException(
                "root logger에 ConsoleAppender가 없다 — logback-spring.xml profile 분기 점검");
    }

    private String readSingleLine() {
        return capturedOutput.toString(StandardCharsets.UTF_8).trim();
    }
}
