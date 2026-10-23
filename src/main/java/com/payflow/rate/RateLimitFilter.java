package com.payflow.rate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Apply rate limiting to specific endpoints
        String path = request.getServletPath();

        if (path.startsWith("/api/v1/transactions/transfer")) {
            String userKey = request.getRemoteAddr() + ":" + path;

            rateLimitService.checkRateLimit(userKey, 10, java.time.Duration.ofMinutes(1));
        }

        filterChain.doFilter(request, response);
    }
}