package com.payflow.rate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.exception.ErrorResponse;
import com.payflow.exception.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * Token-bucket limit on the transfer endpoint: 10 requests per minute per client IP.
 * Filters run outside @RestControllerAdvice, so the 429 body is written here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // Skip CORS preflight requests; only real transfers consume tokens
        if (path.startsWith("/api/v1/transactions/transfer") && !"OPTIONS".equalsIgnoreCase(request.getMethod())) {
            String key = request.getRemoteAddr() + ":" + path;
            try {
                rateLimitService.checkRateLimit(key, 10, Duration.ofMinutes(1));
            } catch (RateLimitExceededException ex) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", "60");
                objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        "Too many transfers. Please wait a minute and try again.",
                        request.getRequestURI(),
                        Instant.now().toEpochMilli()));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
