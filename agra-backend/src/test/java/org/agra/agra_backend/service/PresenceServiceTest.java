package org.agra.agra_backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private PresenceService service;

    @Test
    void markOnlineStoresPresence() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.expire(any(), any(Duration.class))).thenReturn(true);
        ReflectionTestUtils.setField(service, "ttlSeconds", 30L);

        service.markOnline("user-1", "session-1");

        verify(setOperations).add("presence:user:user-1:sessions", "session-1");
        verify(valueOperations).set("presence:user:user-1", "ONLINE");
    }

    @Test
    void refreshUpdatesTtl() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.expire(any(), any(Duration.class))).thenReturn(true);

        service.refresh("user-1", "session-1");

        verify(setOperations).add("presence:user:user-1:sessions", "session-1");
        verify(redisTemplate, atLeastOnce()).expire(eq("presence:user:user-1:sessions"), any(Duration.class));
    }

    @Test
    void markOfflineDeletesWhenNoSessions() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("presence:user:user-1:sessions")).thenReturn(0L);

        service.markOfflineIfNoSessions("user-1", "session-1");

        verify(redisTemplate).delete("presence:user:user-1:sessions");
        verify(redisTemplate).delete("presence:user:user-1");
    }

    @Test
    void markOfflineKeepsWhenSessionsRemain() {
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("presence:user:user-1:sessions")).thenReturn(2L);
        when(redisTemplate.expire(any(), any(Duration.class))).thenReturn(true);

        service.markOfflineIfNoSessions("user-1", "session-1");

        verify(redisTemplate, never()).delete("presence:user:user-1:sessions");
        verify(redisTemplate, atLeastOnce()).expire(eq("presence:user:user-1"), any(Duration.class));
    }

    @Test
    void isOnlineChecksKey() {
        when(redisTemplate.hasKey("presence:user:user-1")).thenReturn(true);

        assertThat(service.isOnline("user-1")).isTrue();
        assertThat(service.isOnline(" ")).isFalse();
    }

    @Test
    void countOnlineUsersUsesSessionKeys() {
        when(redisTemplate.keys("presence:user:*:sessions")).thenReturn(Set.of("k1", "k2"));

        assertThat(service.countOnlineUsers()).isEqualTo(2);
    }

    @Test
    void countOnlineSessionsSumsSizes() {
        when(redisTemplate.keys("presence:user:*:sessions")).thenReturn(Set.of("k1", "k2"));
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("k1")).thenReturn(2L);
        when(setOperations.size("k2")).thenReturn(1L);

        assertThat(service.countOnlineSessions()).isEqualTo(3L);
    }
}
