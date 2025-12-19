package org.agra.agra_backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final Logger log = LoggerFactory.getLogger(PresenceService.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${presence.ttl-seconds:60}")
    private long ttlSeconds;

    public void markOnline(String userId, String sessionId) {
        if (isBlank(userId) || isBlank(sessionId)) {
            return;
        }
        String sessionsKey = sessionsKey(userId);
        redisTemplate.opsForSet().add(sessionsKey, sessionId);
        expire(sessionsKey);

        String statusKey = statusKey(userId);
        redisTemplate.opsForValue().set(statusKey, "ONLINE");
        expire(statusKey);
        log.debug("Presence: user={} session={} marked ONLINE (ttl={}s)", userId, sessionId, ttlSeconds);
    }

    public void refresh(String userId, String sessionId) {
        if (isBlank(userId)) return;
        String sessionsKey = sessionsKey(userId);
        if (!isBlank(sessionId)) {
            redisTemplate.opsForSet().add(sessionsKey, sessionId);
        }
        expire(sessionsKey);
        expire(statusKey(userId));
        log.trace("Presence: refreshed ttl for user={} session={}", userId, sessionId);
    }

    public void markOfflineIfNoSessions(String userId, String sessionId) {
        if (isBlank(userId)) return;
        String sessionsKey = sessionsKey(userId);
        if (!isBlank(sessionId)) {
            redisTemplate.opsForSet().remove(sessionsKey, sessionId);
        }
        Long remaining = redisTemplate.opsForSet().size(sessionsKey);
        if (remaining == null || remaining == 0) {
            redisTemplate.delete(sessionsKey);
            redisTemplate.delete(statusKey(userId));
            log.debug("Presence: user={} marked OFFLINE (no active sessions)", userId);
        } else {
            expire(sessionsKey);
            expire(statusKey(userId));
            log.debug("Presence: user={} disconnect session={}, remainingSessions={}", userId, sessionId, remaining);
        }
    }

    public boolean isOnline(String userId) {
        if (isBlank(userId)) return false;
        Boolean exists = redisTemplate.hasKey(statusKey(userId));
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Count unique online users by the number of active session sets.
     */
    public long countOnlineUsers() {
        Set<String> sessionKeys = redisTemplate.keys("presence:user:*:sessions");
        return sessionKeys == null ? 0L : sessionKeys.size();
    }

    /**
     * Count active WebSocket sessions tracked in presence.
     */
    public long countOnlineSessions() {
        Set<String> sessionKeys = redisTemplate.keys("presence:user:*:sessions");
        if (sessionKeys == null || sessionKeys.isEmpty()) return 0L;
        return sessionKeys.stream()
                .mapToLong(k -> {
                    Long sz = redisTemplate.opsForSet().size(k);
                    return sz == null ? 0L : sz;
                })
                .sum();
    }

    private void expire(String key) {
        if (key == null) return;
        redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
    }

    private String sessionsKey(String userId) {
        return "presence:user:" + userId + ":sessions";
    }

    private String statusKey(String userId) {
        return "presence:user:" + userId;
    }

    private boolean isBlank(String in) {
        return in == null || in.isBlank();
    }
}
