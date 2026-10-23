package com.payflow.rate;

import com.payflow.exception.RateLimitExceededException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void checkRateLimit(String key, int maxRequests, Duration window) {
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(maxRequests, window));

        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for key: {}", key);
            throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
        }
    }

    private Bucket createBucket(int maxRequests, Duration window) {
        Bandwidth limit = Bandwidth.classic(maxRequests, Refill.greedy(maxRequests, window));
        return Bucket.builder().addLimit(limit).build();
    }
}