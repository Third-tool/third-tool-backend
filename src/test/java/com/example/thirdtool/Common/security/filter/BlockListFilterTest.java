package com.example.thirdtool.Common.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("BlockListFilter")
class BlockListFilterTest {

    private BlockListFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new BlockListFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @Test
    @DisplayName(".php 끝 URI는 404 즉시 차단")
    void php_blocked() throws Exception {
        request.setRequestURI("/admin/x.php");

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName(".aspx 끝 URI는 404 즉시 차단")
    void aspx_blocked() throws Exception {
        request.setRequestURI("/something.aspx");

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("/wp- 포함 URI는 404 즉시 차단")
    void wpPrefix_blocked() throws Exception {
        request.setRequestURI("/api/wp-login");

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("/cgi-bin/ 포함 URI는 404 즉시 차단")
    void cgiBin_blocked() throws Exception {
        request.setRequestURI("/cgi-bin/whatever");

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    @DisplayName("정상 URI는 다음 필터로 통과")
    void normal_passes() throws Exception {
        request.setRequestURI("/cards");

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
