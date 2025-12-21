package org.agra.agra_backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

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
}
