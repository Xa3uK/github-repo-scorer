package com.koval.githubreposcorer.config;

import com.koval.githubreposcorer.api.response.PopularRepositoriesResponse;
import java.time.Duration;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
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
}
