package com.clinicalai.gateway;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.clinicalai.gateway.error.RateLimitExceededException;
import com.clinicalai.gateway.service.UsageMeteringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class UsageMeteringServiceTest {

  @Mock private StringRedisTemplate redis;
  @Mock private ValueOperations<String, String> ops;

  @Test
  void monitorModeNeverBlocksEvenOverBudget() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.increment(anyString())).thenReturn(9999L);

    UsageMeteringService service = new UsageMeteringService(redis, 600, "monitor");

    assertThatNoException().isThrownBy(() -> service.admit("tenant-a"));
  }

  @Test
  void enforceModeBlocksOverBudget() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.increment(anyString())).thenReturn(601L);

    UsageMeteringService service = new UsageMeteringService(redis, 600, "enforce");

    assertThatThrownBy(() -> service.admit("tenant-a"))
        .isInstanceOf(RateLimitExceededException.class);
  }

  @Test
  void recordTokensAccumulatesWithoutBlocking() {
    when(redis.opsForValue()).thenReturn(ops);
    when(ops.increment(anyString(), anyLong())).thenReturn(120L);

    UsageMeteringService service = new UsageMeteringService(redis, 600, "monitor");

    assertThatNoException().isThrownBy(() -> service.recordTokens("tenant-a", 120));
  }
}
