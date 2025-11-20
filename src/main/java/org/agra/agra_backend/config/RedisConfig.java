package org.agra.agra_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer(redisObjectMapper())
                        )
                );
    }

    /**
     * Provide per-cache TTLs so dynamic feeds stay fresh while heavier aggregates can live longer.
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration baseConfig) {
        return (RedisCacheManagerBuilder builder) -> {
            Map<String, RedisCacheConfiguration> configs = new HashMap<>();
            configs.put("courses:all", baseConfig.entryTtl(Duration.ofMinutes(15)));
            configs.put("courses:detail", baseConfig.entryTtl(Duration.ofMinutes(30)));
            configs.put("courses:country", baseConfig.entryTtl(Duration.ofMinutes(30)));
            configs.put("courses:domain", baseConfig.entryTtl(Duration.ofMinutes(30)));
            configs.put("courses:featured", baseConfig.entryTtl(Duration.ofMinutes(30)));

            configs.put("feed:recent", baseConfig.entryTtl(Duration.ofSeconds(60)));
            configs.put("feed:topPosts", baseConfig.entryTtl(Duration.ofMinutes(5)));

            configs.put("users:profile", baseConfig.entryTtl(Duration.ofMinutes(10)));
            configs.put("users:dashboard", baseConfig.entryTtl(Duration.ofMinutes(5)));

            configs.put("news:list", baseConfig.entryTtl(Duration.ofMinutes(10)));
            configs.put("news:latest", baseConfig.entryTtl(Duration.ofMinutes(30)));

            builder.withInitialCacheConfigurations(configs);
        };
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
