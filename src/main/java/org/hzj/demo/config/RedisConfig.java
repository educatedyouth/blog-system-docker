package org.hzj.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Spring Cache 的 Redis 配置类
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {

        // 1. 创建 Jackson (JSON) 序列化器
        ObjectMapper objectMapper = new ObjectMapper();

        // 2. (关键) 注册 JavaTimeModule，让 Jackson 知道如何序列化和反序列化
        //    LocalDateTime (我们之前 createTime 字段的问题)
        objectMapper.registerModule(new JavaTimeModule());

        // 3. (关键) 启用“默认类型记录”
        //    这是为了解决反序列化复杂类型（如 List<Post>）时，
        //    Jackson 不知道具体类型的问题。
        //    它会在 JSON 中存入一个 "@class" 字段，
        //    例如: ["java.util.ArrayList", [{ "@class": "com.example.blogsystem.model.Post", ... }]]
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 4. 创建 String（字符串）序列化器 (用于 Key)
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        // 5. 创建我们的最终配置
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // --- Key 的序列化 ---
                // 默认的 key 是 JdkSerialization... (就是 \xac\xed)
                // 我们把它改成 String，这样我们的 key "post::1"
                // 在 Redis 里就显示 "post::1"，而不是一堆乱码
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))

                // --- Value 的序列化 ---
                // 默认的 value 是 JdkSerialization... (\xac\xed)
                // 我们把它改成我们刚配置好的 JSON 序列化器
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))

                // (可选) 设置缓存默认过期时间，例如 30 分钟
                // .entryTtl(Duration.ofMinutes(30))

                // 禁用缓存 null 值（防止缓存穿透）
                .disableCachingNullValues();

        return config;
    }
}