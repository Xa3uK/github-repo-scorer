package com.koval.githubreposcorer.config;

import com.koval.githubreposcorer.api.response.PopularRepositoriesResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class CacheConfig {

    @Bean
    RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(ObjectMapper objectMapper) {
        return builder -> {
            var serializer = new JacksonJsonRedisSerializer<>(objectMapper, PopularRepositoriesResponse.class);
            var pair = RedisSerializationContext.SerializationPair.fromSerializer(serializer);

            builder.withCacheConfiguration(
                "popularRepositories",
                RedisCacheConfiguration.defaultCacheConfig()
                    .serializeValuesWith(pair)
                    .entryTtl(Duration.ofMinutes(30))
            );
        };
    }

    @Bean
    public static BeanPostProcessor cacheManagerLoggingDecorator() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof CacheManager cm && !(bean instanceof LoggingCacheManager)) {
                    return new LoggingCacheManager(cm);
                }
                return bean;
            }
        };
    }

    static class LoggingCacheManager implements CacheManager {

        private final CacheManager delegate;

        LoggingCacheManager(CacheManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public Cache getCache(String name) {
            Cache cache = delegate.getCache(name);
            return cache == null ? null : new LoggingCache(cache);
        }

        @Override
        public Collection<String> getCacheNames() {
            return delegate.getCacheNames();
        }
    }

    static class LoggingCache implements Cache {

        private static final Logger log = LoggerFactory.getLogger(LoggingCache.class);

        private final Cache delegate;

        LoggingCache(Cache delegate) {
            this.delegate = delegate;
        }

        @Override
        public Cache.ValueWrapper get(Object key) {
            ValueWrapper value = delegate.get(key);
            if (value != null) {
                log.info("Cache HIT [{}] key={}", getName(), key);
            } else {
                log.info("Cache MISS [{}] key={}", getName(), key);
            }
            return value;
        }

        @Override public String getName()                                              { return delegate.getName(); }
        @Override public Object getNativeCache()                                       { return delegate.getNativeCache(); }
        @Override public <T> T get(Object key, Class<T> type)                         { return delegate.get(key, type); }
        @Override public <T> T get(Object key, Callable<T> valueLoader)               { return delegate.get(key, valueLoader); }
        @Override public void put(Object key, Object value)                            { delegate.put(key, value); }
        @Override public ValueWrapper putIfAbsent(Object key, Object value)            { return delegate.putIfAbsent(key, value); }
        @Override public void evict(Object key)                                        { delegate.evict(key); }
        @Override public boolean evictIfPresent(Object key)                            { return delegate.evictIfPresent(key); }
        @Override public void clear()                                                  { delegate.clear(); }
        @Override public boolean invalidate()                                          { return delegate.invalidate(); }
    }
}
