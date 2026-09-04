package com.issueflow.config;

import com.issueflow.constants.LoggingConstants;
import com.issueflow.logging.OperationalLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return LoggingConstants.HTTP_SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(LoggingConstants.REQUEST_ID, requestId);
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            int status = response.getStatus();
            String route = resolveRoute(request);
            OperationalLog.Fields fields = OperationalLog.event(LoggingConstants.EVENT_HTTP_REQUEST)
                    .put(LoggingConstants.HTTP_METHOD, request.getMethod())
                    .put(LoggingConstants.HTTP_ROUTE, route)
                    .put(LoggingConstants.HTTP_STATUS, status)
                    .put(LoggingConstants.DURATION_MS, durationMs)
                    .put(LoggingConstants.OUTCOME, httpOutcome(status));
            if (isNoisySuccessfulRead(request.getMethod(), route, status)) {
                fields.debug(LOGGER);
            } else if (status >= 500) {
                fields.warn(LOGGER);
            } else {
                fields.info(LOGGER);
            }
            MDC.remove(LoggingConstants.REQUEST_ID);
        }
    }

    public static String resolveRoute(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern instanceof String route && !route.isBlank()) {
            return route;
        }
        return request.getRequestURI();
    }

    public static String httpOutcome(int status) {
        if (status >= 500) {
            return LoggingConstants.OUTCOME_SERVER_ERROR;
        }
        if (status >= 400) {
            return LoggingConstants.OUTCOME_CLIENT_ERROR;
        }
        return LoggingConstants.OUTCOME_SUCCESS;
    }

    public static boolean isNoisySuccessfulRead(String method, String route, int status) {
        return "GET".equalsIgnoreCase(method)
                && status < 400
                && route != null
                && route.contains("outbound-jobs");
    }
}
