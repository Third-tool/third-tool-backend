package com.example.thirdtool.Common.logging.filter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@DisplayName("MdcLoggingFilter")
class MdcLoggingFilterTest {

    private MdcLoggingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger filterLogger;

    @BeforeEach
    void setUp() {
        filter = new MdcLoggingFilter();
        request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/api/v1/cards");
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);

        filterLogger = (Logger) LoggerFactory.getLogger(MdcLoggingFilter.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        filterLogger.addAppender(logAppender);

        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        filterLogger.detachAppender(logAppender);
        logAppender.stop();
        MDC.clear();
    }

    @Test
    @DisplayName("요청에 X-Request-Id 헤더가 없으면 서버가 UUID를 생성해 응답에 echo한다")
    void 요청에_X_Request_Id_헤더가_없으면_서버가_UUID를_생성해_응답에_echo한다() throws Exception {
        filter.doFilter(request, response, chain);

        String echoed = response.getHeader(MdcLoggingFilter.HEADER);
        assertThat(echoed).isNotBlank();
        assertThat(echoed).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("유입된 X-Request-Id가 정상이면 그대로 사용하고 응답에 echo한다")
    void 유입된_X_Request_Id가_정상이면_그대로_사용하고_응답에_echo한다() throws Exception {
        request.addHeader(MdcLoggingFilter.HEADER, "client-abc-123");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(MdcLoggingFilter.HEADER)).isEqualTo("client-abc-123");
    }

    @Test
    @DisplayName("MDC에 requestId·traceId·method·path가 모두 주입되고 traceId는 requestId와 같다")
    void MDC에_requestId_traceId_method_path가_모두_주입되고_traceId는_requestId와_같다() throws Exception {
        request.addHeader(MdcLoggingFilter.HEADER, "req-99");
        AtomicReference<Map<String, String>> snapshot = new AtomicReference<>();
        doAnswer(captureMdcSnapshot(snapshot)).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        Map<String, String> captured = snapshot.get();
        assertThat(captured).isNotNull();
        assertThat(captured.get(MdcLoggingFilter.MDC_REQUEST_ID)).isEqualTo("req-99");
        assertThat(captured.get(MdcLoggingFilter.MDC_TRACE_ID)).isEqualTo("req-99");
        assertThat(captured.get(MdcLoggingFilter.MDC_METHOD)).isEqualTo("GET");
        assertThat(captured.get(MdcLoggingFilter.MDC_PATH)).isEqualTo("/api/v1/cards");
    }

    @Test
    @DisplayName("chain doFilter 호출 후 MDC가 clear된다")
    void chain_doFilter_호출_후_MDC가_clear된다() throws Exception {
        filter.doFilter(request, response, chain);

        Map<String, String> remaining = MDC.getCopyOfContextMap();
        assertThat(remaining == null || remaining.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("request.end 로그에 status와 durationMs가 INFO 레벨로 포함된다")
    void request_end_로그에_status와_durationMs가_INFO_레벨로_포함된다() throws Exception {
        response.setStatus(200);

        filter.doFilter(request, response, chain);

        ILoggingEvent endEvent = findEventStartingWith("request.end");
        assertThat(endEvent.getLevel()).isEqualTo(Level.INFO);
        String formatted = endEvent.getFormattedMessage();
        assertThat(formatted).contains("status=200");
        assertThat(formatted).containsPattern("durationMs=\\d+");
    }

    @Test
    @DisplayName("유입된 X-Request-Id가 빈 문자열이면 서버 UUID로 대체한다")
    void 유입된_X_Request_Id가_빈_문자열이면_서버_UUID로_대체한다() throws Exception {
        request.addHeader(MdcLoggingFilter.HEADER, "");

        filter.doFilter(request, response, chain);

        String echoed = response.getHeader(MdcLoggingFilter.HEADER);
        assertThat(echoed).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("유입된 X-Request-Id가 공백만 있으면 서버 UUID로 대체한다")
    void 유입된_X_Request_Id가_공백만_있으면_서버_UUID로_대체한다() throws Exception {
        request.addHeader(MdcLoggingFilter.HEADER, "   ");

        filter.doFilter(request, response, chain);

        String echoed = response.getHeader(MdcLoggingFilter.HEADER);
        assertThat(echoed).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("유입된 X-Request-Id가 정확히 64자이면 그대로 사용한다 (경계값)")
    void 유입된_X_Request_Id가_정확히_64자이면_그대로_사용한다() throws Exception {
        String boundary = "a".repeat(MdcLoggingFilter.MAX_LEN);
        request.addHeader(MdcLoggingFilter.HEADER, boundary);

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(MdcLoggingFilter.HEADER)).isEqualTo(boundary);
    }

    @Test
    @DisplayName("유입된 X-Request-Id가 65자이면 서버 UUID로 대체한다 (경계값 +1)")
    void 유입된_X_Request_Id가_65자이면_서버_UUID로_대체한다() throws Exception {
        String overflow = "a".repeat(MdcLoggingFilter.MAX_LEN + 1);
        request.addHeader(MdcLoggingFilter.HEADER, overflow);

        filter.doFilter(request, response, chain);

        String echoed = response.getHeader(MdcLoggingFilter.HEADER);
        assertThat(echoed).isNotEqualTo(overflow);
        assertThat(echoed).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 있으면 clientIp는 가장 좌측 IP를 사용한다")
    void X_Forwarded_For_헤더가_있으면_clientIp는_가장_좌측_IP를_사용한다() throws Exception {
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8, 9.9.9.9");

        filter.doFilter(request, response, chain);

        ILoggingEvent startEvent = findEventStartingWith("request.start");
        assertThat(startEvent.getFormattedMessage()).contains("clientIp=1.2.3.4");
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더에 단일 IP만 있으면 콤마 없이 그대로 사용한다")
    void X_Forwarded_For_헤더에_단일_IP만_있으면_콤마_없이_그대로_사용한다() throws Exception {
        request.addHeader("X-Forwarded-For", "203.0.113.5");

        filter.doFilter(request, response, chain);

        ILoggingEvent startEvent = findEventStartingWith("request.start");
        assertThat(startEvent.getFormattedMessage()).contains("clientIp=203.0.113.5");
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더가 없으면 clientIp는 remoteAddr를 사용한다")
    void X_Forwarded_For_헤더가_없으면_clientIp는_remoteAddr를_사용한다() throws Exception {
        request.setRemoteAddr("10.0.0.42");

        filter.doFilter(request, response, chain);

        ILoggingEvent startEvent = findEventStartingWith("request.start");
        assertThat(startEvent.getFormattedMessage()).contains("clientIp=10.0.0.42");
    }

    @Test
    @DisplayName("유입된 X-Request-Id가 앞뒤 공백을 포함하면 trim 후 사용한다")
    void 유입된_X_Request_Id가_앞뒤_공백을_포함하면_trim_후_사용한다() throws Exception {
        request.addHeader(MdcLoggingFilter.HEADER, "  client-id-42  ");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(MdcLoggingFilter.HEADER)).isEqualTo("client-id-42");
    }

    @Test
    @DisplayName("유입된 X-Request-Id가 탭·개행 등 공백만이면 서버 UUID로 대체한다")
    void 유입된_X_Request_Id가_탭_개행_등_공백만이면_서버_UUID로_대체한다() throws Exception {
        request.addHeader(MdcLoggingFilter.HEADER, "\t\n  \t");

        filter.doFilter(request, response, chain);

        String echoed = response.getHeader(MdcLoggingFilter.HEADER);
        assertThat(echoed).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("/actuator/health 경로는 필터가 스킵되어 MDC도 응답 헤더도 주입되지 않는다")
    void actuator_health_경로는_필터가_스킵되어_MDC도_응답_헤더도_주입되지_않는다() throws Exception {
        request.setRequestURI("/actuator/health");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(MdcLoggingFilter.HEADER)).isNull();
        Map<String, String> remaining = MDC.getCopyOfContextMap();
        assertThat(remaining == null || remaining.isEmpty()).isTrue();
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    @DisplayName("/health 경로도 필터가 스킵된다")
    void health_경로도_필터가_스킵된다() throws Exception {
        request.setRequestURI("/health");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(MdcLoggingFilter.HEADER)).isNull();
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    @DisplayName("/actuator/prometheus 경로도 필터가 스킵된다 (Story 4-1 prefix 매칭 확장 회귀)")
    void actuator_prometheus_경로도_필터가_스킵된다() throws Exception {
        request.setRequestURI("/actuator/prometheus");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(MdcLoggingFilter.HEADER)).isNull();
        assertThat(logAppender.list).isEmpty();
    }

    @Test
    @DisplayName("/actuator-자체-아닌-prefix는 스킵되지 않는다 (false positive 차단)")
    void 유사한_prefix_경로는_스킵되지_않는다() throws Exception {
        // /actuators (s 포함) 또는 /actuator-metrics 같은 false positive 패턴은 스킵 X
        request.setRequestURI("/actuator-metrics");

        filter.doFilter(request, response, chain);

        // 정상 필터 동작 — 응답 헤더 + 로그 출력
        assertThat(response.getHeader(MdcLoggingFilter.HEADER)).isNotBlank();
        assertThat(logAppender.list).isNotEmpty();
    }

    @Test
    @DisplayName("chain doFilter에서 예외가 throw되어도 MDC가 clear된다")
    void chain_doFilter에서_예외가_throw되어도_MDC가_clear된다() throws Exception {
        doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class)
                .hasMessage("boom");

        Map<String, String> remaining = MDC.getCopyOfContextMap();
        assertThat(remaining == null || remaining.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("chain doFilter에서 예외가 throw되어도 응답 헤더 X-Request-Id는 set된다")
    void chain_doFilter에서_예외가_throw되어도_응답_헤더_X_Request_Id는_set된다() throws Exception {
        request.addHeader(MdcLoggingFilter.HEADER, "preserve-id");
        doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class);

        assertThat(response.getHeader(MdcLoggingFilter.HEADER)).isEqualTo("preserve-id");
    }

    @Test
    @DisplayName("chain doFilter에서 예외가 throw되어도 request.end 로그가 INFO로 남는다")
    void chain_doFilter에서_예외가_throw되어도_request_end_로그가_INFO로_남는다() throws Exception {
        doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilter(request, response, chain))
                .isInstanceOf(ServletException.class);

        ILoggingEvent endEvent = findEventStartingWith("request.end");
        assertThat(endEvent.getLevel()).isEqualTo(Level.INFO);
        assertThat(endEvent.getFormattedMessage()).containsPattern("durationMs=\\d+");
    }

    // ---- helpers ----

    private static org.mockito.stubbing.Answer<Void> captureMdcSnapshot(AtomicReference<Map<String, String>> sink) {
        return (InvocationOnMock invocation) -> {
            sink.set(MDC.getCopyOfContextMap());
            return null;
        };
    }

    private ILoggingEvent findEventStartingWith(String prefix) {
        List<ILoggingEvent> events = logAppender.list;
        return events.stream()
                .filter(e -> e.getFormattedMessage().startsWith(prefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "기대한 로그(" + prefix + ")가 캡처되지 않음. captured=" + events));
    }
}
