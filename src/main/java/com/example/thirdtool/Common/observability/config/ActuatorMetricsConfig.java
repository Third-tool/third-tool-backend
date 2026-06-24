package com.example.thirdtool.Common.observability.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Actuator/Micrometer 공통 메트릭 구성 (Story 4-1, 0-b 메트릭 Epic 1).
 *
 * <p>application.yml은 .gitignore 대상이라 운영 환경별 갱신을 보장할 수 없다. 본 Java 설정은
 * 환경 무관하게 빌드 산출물에 포함되어 다음 두 책임을 단일 진실 소스로 처리한다.
 * <ul>
 *   <li>모든 메트릭에 {@code application=thirdtool} 공통 태그 부착 — 다중 서비스 환경에서 구분</li>
 *   <li>{@code http.server.requests} 메트릭에 histogram bucket + P50·P95·P99 등록 —
 *       Prometheus {@code histogram_quantile()}로 P95/P99 패널 계산 가능</li>
 * </ul>
 *
 * <p>endpoint 노출(`management.endpoints.web.exposure.include` 등)은 Spring Boot가 부트스트랩 단에서
 * properties로 읽으므로 Java 설정으로 대체 불가. 사용자 환경의 application.yml에서 명시한다 (가이드:
 * {@code docs/operations/troubleshooting/}).
 */
@Configuration
public class ActuatorMetricsConfig {

    static final String APPLICATION_TAG_KEY = "application";
    static final String APPLICATION_TAG_VALUE = "thirdtool";
    static final String HTTP_SERVER_REQUESTS = "http.server.requests";
    static final String URI_TAG = "uri";
    static final String ACTUATOR_URI_PREFIX = "/actuator";

    /**
     * 모든 Meter에 {@code application=thirdtool} 공통 태그 + {@code /actuator/**} URI의
     * {@code http.server.requests} 메트릭 deny를 등록한다.
     *
     * <p>actuator deny 사유: 10초 간격 Prometheus scrape가 자체 시계열을 누적하면 카디널리티
     * 노이즈만 증가하고 관측 가치는 없다. {@code MdcLoggingFilter} SKIP은 로그만 차단하므로
     * 메트릭 측 차단은 본 MeterFilter가 책임.
     *
     * <p>{@link MeterFilter} 빈 자동 감지 패턴 대신 {@link MeterRegistryCustomizer} 안에서
     * 명시 등록하는 이유: Spring Boot 3.5 + Prometheus registry 조합에서 MeterFilter 빈이
     * PrometheusMeterRegistry까지 일관 적용되지 않는 케이스가 발견됨 (통합 테스트에서 catch).
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> applicationCommonTag() {
        return registry -> {
            registry.config().commonTags(APPLICATION_TAG_KEY, APPLICATION_TAG_VALUE);
            registry.config().meterFilter(MeterFilter.deny(id -> {
                if (!id.getName().startsWith(HTTP_SERVER_REQUESTS)) {
                    return false;
                }
                String uri = id.getTag(URI_TAG);
                return uri != null && uri.startsWith(ACTUATOR_URI_PREFIX);
            }));
        };
    }

    /**
     * {@code http.server.requests} 시리즈에 percentile histogram bucket을 활성화하고
     * P50/P95/P99 정량 분포를 등록한다. application.yml의 동등 설정:
     * <pre>
     * management.metrics.distribution.percentiles-histogram.http.server.requests: true
     * management.metrics.distribution.percentiles.http.server.requests: 0.5, 0.95, 0.99
     * </pre>
     */
    @Bean
    public MeterFilter httpServerRequestsDistribution() {
        return new MeterFilter() {
            @Override
            public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
                if (id.getName().startsWith(HTTP_SERVER_REQUESTS)) {
                    return DistributionStatisticConfig.builder()
                            .percentilesHistogram(true)
                            .percentiles(0.5, 0.95, 0.99)
                            .build()
                            .merge(config);
                }
                return config;
            }
        };
    }

}
