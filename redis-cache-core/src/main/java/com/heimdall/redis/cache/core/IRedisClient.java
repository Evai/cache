package com.heimdall.redis.cache.core;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author crh
 * @description
 * @date 2019-12-01
 */
public interface IRedisClient {

    Object get(String key);

    void set(String key, String value, long seconds);

    <T> T get(String key, Class<T> clazz);

    Long getExpire(String key, TimeUnit timeUnit);

    boolean expire(String key, long timeout, TimeUnit timeUnit);

    boolean delete(String key);

    /**
     * 获取keys，指定 pattern
     *
     * @param pattern "*keyName*"
     */
    void keys(String pattern, Consumer<Set<String>> consumer);

    ScanCursor<String> scan(Long cursorId, String pattern);

    /**
     * 批量删除key，指定 key 集合
     *
     * @param keys
     */
    Long unlink(Collection<String> keys);

    boolean unlink(String key);

    boolean exists(String key);

    Long size(String key);

    Object leftPop(String key);

    Object leftPop(String key, long timeout, TimeUnit unit);

    /**
     * 批处理
     *
     * @param runnable
     */
    Object multi(Runnable runnable);
}
