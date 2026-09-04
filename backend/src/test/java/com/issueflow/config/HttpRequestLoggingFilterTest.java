package com.issueflow.config;

import com.issueflow.constants.LoggingConstants;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestLoggingFilterTest {

    private final HttpRequestLoggingFilter filter = new HttpRequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void usesPatternRouteAndClearsRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/issues/10/status");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/issues/{id}/status");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {
            assertThat(MDC.get(LoggingConstants.REQUEST_ID)).isNotBlank();
            ((MockHttpServletResponse) res).setStatus(200);
        };

        filter.doFilter(request, response, chain);

        assertThat(HttpRequestLoggingFilter.resolveRoute(request)).isEqualTo("/api/issues/{id}/status");
        assertThat(MDC.get(LoggingConstants.REQUEST_ID)).isNull();
    }

    @Test
    void skipsCorsPreflightAndDocs() {
        MockHttpServletRequest options = new MockHttpServletRequest("OPTIONS", "/api/issues");
        MockHttpServletRequest swagger = new MockHttpServletRequest("GET", "/swagger-ui/index.html");

        assertThat(filter.shouldNotFilter(options)).isTrue();
        assertThat(filter.shouldNotFilter(swagger)).isTrue();
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", "/api/issues"))).isFalse();
    }

    @Test
    void treatsOutboundJobPollingAsNoisyRead() {
        assertThat(HttpRequestLoggingFilter.isNoisySuccessfulRead(
                "GET",
                "/api/issues/{id}/outbound-jobs",
                200
        )).isTrue();
        assertThat(HttpRequestLoggingFilter.isNoisySuccessfulRead("POST", "/api/issues/{id}/outbound-jobs", 200))
                .isFalse();
        assertThat(HttpRequestLoggingFilter.httpOutcome(404)).isEqualTo(LoggingConstants.OUTCOME_CLIENT_ERROR);
        assertThat(HttpRequestLoggingFilter.httpOutcome(503)).isEqualTo(LoggingConstants.OUTCOME_SERVER_ERROR);
    }
}
