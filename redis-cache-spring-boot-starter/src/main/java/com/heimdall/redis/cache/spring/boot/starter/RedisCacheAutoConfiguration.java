package com.heimdall.redis.cache.spring.boot.starter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.heimdall.redis.cache.core.*;
import com.heimdall.redis.cache.core.util.ThreadPoolUtils;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author crh
 * @date 2019-10-27
 * description
 */
@Configuration
@ConditionalOnClass
@EnableConfigurationProperties(RedisCacheProperties.class)
public class RedisCacheAutoConfiguration {

    @Resource
    private  RedisTemplate<String, Object> redisTemplate;

    @Resource
    private  RedissonClient redissonClient;

    @Bean
    @ConditionalOnMissingBean
    public RedisCacheProperties redisCacheProperties() {
        RedisCacheProperties redisCacheProperties = new RedisCacheProperties();
        redisCacheProperties.setKeyPrefix(CacheConstant.CACHE_PREFIX);
        return redisCacheProperties;
    }

    @Bean
    public CacheLock cacheLock() {
        return new RedisLock(redissonClient, redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public ExecutorService cacheExecutor(RedisCacheProperties redisCacheProperties) {
        return ThreadPoolUtils.newThreadPoolExecutor("redis-cache-executor", ThreadPoolUtils.CORE_POOL_SIZE, ThreadPoolUtils.CORE_POOL_SIZE * 2, 60L, TimeUnit.SECONDS, 500, new ThreadPoolExecutor.AbortPolicy());
    }

    @Bean
    public IJsonSerializer jsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        // ignore null field
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // ignore empty bean convert json error
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // ignore field in json, but not in bean convert error
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new JacksonSerializer(objectMapper);
    }

    @Bean
    public JacksonSerializer jacksonSerializer() {
        return (JacksonSerializer) jsonSerializer();
    }

    @Bean
    public IRedisClient redisClient() {
        return new RedisClient(redisTemplate, jsonSerializer());
    }

    @Bean
    public IRedisClient redisService() {
        return new RedisClient(redisTemplate, jsonSerializer());
    }

    public CacheComponent cacheComponent() {
        return new CacheComponent(redisClient(), redisCacheProperties(), redisTemplate,cacheLock(),jacksonSerializer(), cacheExecutor(redisCacheProperties()));
    }

    @Bean
    public CacheAbleAspect cacheAbleAspect() {
        return new CacheAbleAspect(cacheComponent(), jacksonSerializer(), redisCacheProperties());
    }

    @Bean
    public CacheAbleEntityAspect cacheAbleEntityAspect() {
        return new CacheAbleEntityAspect(cacheComponent(), redisCacheProperties());
    }

}
