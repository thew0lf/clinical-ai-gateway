package com.clinicalai.gateway.service;

import com.clinicalai.gateway.error.RateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Per tenant usage metering for cost and abuse observability. The control plane does not throttle AI
 * by default. In MONITOR mode, the default, it records request counts and token usage per tenant and
 * logs when a tenant crosses a soft budget, without ever blocking a request. ENFORCE mode is
 * available for tenants or environments that opt into a hard budget, and is off by default.
 *
 * <p>Centralizing usage here gives one place to reason about spend across the whole assistant fleet,
 * and the same key strategy feeds dashboards and alerts.
 */
@Service
public class UsageMeteringService {

  private static final Logger log = LoggerFactory.getLogger(UsageMeteringService.class);

  enum Mode {
    MONITOR,
    ENFORCE
  }

  private final StringRedisTemplate redis;
  private final int requestsPerMinute;
  private final Mode mode;

  public UsageMeteringService(
      StringRedisTemplate redis,
      @Value("${app.usage.requests-per-minute:600}") int requestsPerMinute,
      @Value("${app.usage.mode:monitor}") String mode) {
    this.redis = redis;
    this.requestsPerMinute = requestsPerMinute;
    this.mode = Mode.valueOf(mode.trim().toUpperCase());
  }

  /**
   * Records a request against the per tenant minute window. Never blocks in MONITOR mode. Throws in
   * ENFORCE mode once the soft budget is crossed.
   */
  public void admit(String tenantId) {
    long window = Instant.now().getEpochSecond() / 60;
    String key = "usage:req:" + tenantId + ":" + window;
    Long count = redis.opsForValue().increment(key);
    if (count != null && count == 1L) {
      redis.expire(key, Duration.ofSeconds(90));
    }
    if (count != null && count > requestsPerMinute) {
      if (mode == Mode.ENFORCE) {
        throw new RateLimitExceededException(
            "request budget exceeded for tenant " + tenantId);
      }
      log.warn(
          "tenant {} crossed the soft request budget: {} of {} this minute",
          tenantId,
          count,
          requestsPerMinute);
    }
  }

  /** Accumulates token usage per tenant for cost observability. Never blocks. */
  public void recordTokens(String tenantId, int tokens) {
    long dayWindow = Instant.now().getEpochSecond() / 86400;
    String key = "usage:tok:" + tenantId + ":" + dayWindow;
    Long total = redis.opsForValue().increment(key, tokens);
    if (total != null && total == tokens) {
      redis.expire(key, Duration.ofDays(2));
    }
  }

  /** Requests recorded for the tenant in the current minute window. */
  public long currentRequestCount(String tenantId) {
    long window = Instant.now().getEpochSecond() / 60;
    return readLong("usage:req:" + tenantId + ":" + window);
  }

  /** Tokens accumulated for the tenant in the current day window. */
  public long tokensToday(String tenantId) {
    long dayWindow = Instant.now().getEpochSecond() / 86400;
    return readLong("usage:tok:" + tenantId + ":" + dayWindow);
  }

  /** Per tenant request budget used for the soft warning and for enforce mode. */
  public int requestBudgetPerMinute() {
    return requestsPerMinute;
  }

  /** Current metering mode, MONITOR or ENFORCE. */
  public String mode() {
    return mode.name();
  }

  private long readLong(String key) {
    String value = redis.opsForValue().get(key);
    return value == null ? 0L : Long.parseLong(value);
  }
}
