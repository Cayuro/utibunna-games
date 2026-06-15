package com.utibunna.games.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis wiring. The Lettuce {@link RedisConnectionFactory} is auto-configured from
 * {@code spring.data.redis.*}; we only declare the template.
 *
 * <p>We use a {@link StringRedisTemplate} and store game state as plain JSON strings (no
 * {@code @RedisHash}, no polymorphic type hints) so we keep exact control of the key
 * {@code game:{roomId}}. Serialization uses Spring's managed Jackson {@code ObjectMapper}.</p>
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
