package org.agra.agra_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void redisCacheConfigurationBuilds() {
        RedisConfig config = new RedisConfig();

        RedisCacheConfiguration configuration = config.redisCacheConfiguration();

        assertThat(configuration).isNotNull();
    }

    @Test
    void cacheManagerBuilds() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisCacheManager manager = config.cacheManager(factory, config.redisCacheConfiguration());

        assertThat(manager).isNotNull();
    }

    @Test
    void cacheObjectMapperWritesIsoDates() throws Exception {
        RedisConfig config = new RedisConfig();

        String json = config.buildCacheObjectMapper()
                .writeValueAsString(LocalDateTime.of(2025, 12, 21, 14, 13, 46));

        assertThat(json).contains("2025-12-21T14:13:46");
    }
}
