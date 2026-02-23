package com.webknot.kpi.logging;

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

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long startedAtNanos = System.nanoTime();
        String method = request.getMethod();
        String pathWithQuery = request.getRequestURI() +
                (request.getQueryString() == null ? "" : "?" + request.getQueryString());
        String clientIp = resolveClientIp(request);
        String userAgent = String.valueOf(request.getHeader("User-Agent"));
        boolean thrown = false;

        try {
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            thrown = true;
            long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
            log.error("HTTP {} {} failed after {}ms clientIp={} status={} message={}",
                    method, pathWithQuery, durationMs, clientIp, response.getStatus(), e.getMessage(), e);
            throw e;
        } finally {
            if (!thrown) {
                long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000L;
                int status = response.getStatus();
                if (status >= 500) {
                    log.error("HTTP {} {} -> {} in {}ms clientIp={} ua={}",
                            method, pathWithQuery, status, durationMs, clientIp, userAgent);
                } else if (status >= 400) {
                    log.warn("HTTP {} {} -> {} in {}ms clientIp={} ua={}",
                            method, pathWithQuery, status, durationMs, clientIp, userAgent);
                } else {
                    log.info("HTTP {} {} -> {} in {}ms clientIp={}",
                            method, pathWithQuery, status, durationMs, clientIp);
                }
            }
            MDC.remove("requestId");
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] parts = forwardedFor.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                return parts[0].trim();
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
