package com.elina.authorization.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis cache configuration for master codes.
 * Provides caching with configurable TTL and graceful fallback when Redis is unavailable.
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheConfig.class);

    @Value("${master-data.cache.ttl-minutes:30}")
    private int cacheTtlMinutes;

    @Value("${master-data.cache.enabled:true}")
    private boolean cacheEnabled;

    @Bean
    @Primary
    @ConditionalOnProperty(name = "master-data.cache.enabled", havingValue = "true", matchIfMissing = true)
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        try {
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(cacheTtlMinutes))
                    .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues();

            RedisCacheManager manager = RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(config)
                    .transactionAware()
                    .build();

            logger.info("Redis cache manager initialized with TTL: {} minutes", cacheTtlMinutes);
            return manager;
        } catch (Exception e) {
            logger.warn("Failed to initialize Redis cache manager. Falling back to NoOpCacheManager. Error: {}", e.getMessage());
            return new NoOpCacheManager();
        }
    }

    @Bean
    @ConditionalOnProperty(name = "master-data.cache.enabled", havingValue = "false")
    public CacheManager noOpCacheManager() {
        logger.info("Redis caching is disabled. Using NoOpCacheManager.");
        return new NoOpCacheManager();
    }
}

