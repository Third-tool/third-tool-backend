package com.example.thirdtool.Common.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 requestId를 부여하고 MDC·응답 헤더로 전파하는 필터 (Story 2-1).
 *
 * <p>SecurityFilterChain 가장 앞단에서 동작하며 다음을 보장한다.
 * <ul>
 *   <li>X-Request-Id 헤더가 정상이면 그대로 사용, 없거나 trim 후 blank/{@value #MAX_LEN}자 초과면 서버가 UUID 생성</li>
 *   <li>MDC 키 4종(requestId, traceId(=requestId), method, path)을 logback-spring.xml 화이트리스트와 정합하게 주입</li>
 *   <li>응답 헤더 X-Request-Id echo back — 클라이언트가 동일 ID로 문의·추적 가능</li>
 *   <li>request.start / request.end 로그 INFO + status + durationMs</li>
 *   <li>예외 경로 포함 finally 블록에서 {@link MDC#clear()} — 스레드 풀 재사용 시 누수 차단</li>
 *   <li>{@code /actuator/health} · {@code /health} 경로는 노이즈 차단을 위해 필터 자체 스킵</li>
 *   <li>ERROR dispatch 재진입 차단({@link #shouldNotFilterErrorDispatch()}) — 요청당 단일 로그·헤더 보장</li>
 * </ul>
 *
 * <p>{@code clientIp}는 {@code X-Forwarded-For} 우선·{@code RemoteAddr} fallback. <b>신뢰 경계는
 * ALB/CloudFront/Nginx 등 신뢰 LB 뒤 배포를 가정</b> — LB 없는 직접 노출 환경에서는 spoofing 위험으로
 * 로그 신뢰도가 떨어진다. Product 5/6 인프라 배포 후 활용도 상승.
 *
 * <p>userId 주입은 Story 2-2 별도 처리(logback 화이트리스트엔 등록되어 있으나 본 Story 단계엔 주입자 없음).
 * traceId == requestId는 v1 단순화 — 분산 트레이싱 도입 시 분리. 자세한 결정 배경은 ADR008.
 */
@Slf4j
public class MdcLoggingFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Request-Id";
    static final String MDC_REQUEST_ID = "requestId";
    static final String MDC_TRACE_ID = "traceId";
    static final String MDC_METHOD = "method";
    static final String MDC_PATH = "path";
    static final int MAX_LEN = 64;

    private static final String ACTUATOR_PREFIX = "/actuator/";
    private static final Set<String> SKIP_EXACT_PATHS = Set.of("/health", "/actuator");

    /**
     * 노이즈 차단을 위해 actuator 경로 전체와 비-actuator health probe를 스킵.
     * Story 4-1로 {@code /actuator/prometheus}가 10초 간격 스크랩되면 request.start/end가
     * 그만큼 출력되므로 prefix 매칭으로 한 번에 차단.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith(ACTUATOR_PREFIX) || SKIP_EXACT_PATHS.contains(uri);
    }

    /**
     * ERROR dispatch(예: {@code sendError(403)} 결과의 컨테이너 재dispatch) 시 필터 재진입을 차단한다.
     * 재진입을 허용하면 요청당 request.start/end 로그가 2배로 출력되고, 응답 헤더 X-Request-Id가
     * 새 UUID로 덮어씌워질 위험이 있다.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER));
        long startedAt = System.currentTimeMillis();
        try {
            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_TRACE_ID, requestId);
            MDC.put(MDC_METHOD, request.getMethod());
            MDC.put(MDC_PATH, request.getRequestURI());
            response.setHeader(HEADER, requestId);

            log.info("request.start clientIp={}", clientIp(request));

            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("request.end status={} durationMs={}", response.getStatus(), durationMs);
            MDC.clear();
        }
    }

    private String resolveRequestId(String incoming) {
        if (incoming == null) {
            return UUID.randomUUID().toString();
        }
        String trimmed = incoming.trim();
        if (trimmed.isEmpty() || trimmed.length() > MAX_LEN) {
            return UUID.randomUUID().toString();
        }
        return trimmed;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
