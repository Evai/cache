package com.heimdall.redis.cache.spring.boot.starter;

import com.heimdall.redis.cache.core.IJsonSerializer;
import com.heimdall.redis.cache.core.IRedisClient;
import com.heimdall.redis.cache.core.ScanCursor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class RedisClient implements IRedisClient {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IJsonSerializer jsonSerializer;

    public RedisClient(RedisTemplate<String, Object> redisTemplate, IJsonSerializer jsonSerializer) {
        this.redisTemplate = redisTemplate;
        this.jsonSerializer = jsonSerializer;
    }

    @Override
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, long seconds) {
        redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
    }

    @Override
    public <T> T get(String key, Class<T> clazz) {
        Object value = this.get(key);
        if (!(value instanceof String)) {
            return clazz.cast(value);
        }
        return jsonSerializer.deserializer((String) value, clazz);
    }

    @Override
    public Long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }

    @Override
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, timeUnit));
    }

    @Override
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public void keys(String pattern, Consumer<Set<String>> consumer) {
        Set<String> sets = new HashSet<>();
        long cursorId = 0L;
        for (; ; ) {
            com.heimdall.redis.cache.core.ScanCursor<String> scanCursor = this.scan(cursorId, pattern);
            if (scanCursor.getCursorId() == 0L || CollectionUtils.isEmpty(scanCursor.getItems())) {
                break;
            }
            consumer.accept(sets);
            cursorId = scanCursor.getCursorId();
        }
    }

    @Override
    public com.heimdall.redis.cache.core.ScanCursor<String> scan(Long cursorId, String pattern) {
        Cursor<byte[]> cursor = redisTemplate.execute((RedisCallback<Cursor<byte[]>>) redisConnection -> redisConnection.scan(
                ScanOptions.scanOptions()
                        .count(cursorId == null ? 0L : cursorId)
                        .match(pattern)
                        .build()
        ));
        if (cursor == null || cursor.getCursorId() == 0L) {
            return null;
        }

        Set<String> set = new HashSet<>();
        while (cursor.hasNext()) {
            set.add(new String(cursor.next()));
        }
        return new ScanCursor<>(cursor.getCursorId(), set);
    }

    @Override
    public Long unlink(Collection<String> keys) {
        return redisTemplate.unlink(keys);
    }

    @Override
    public boolean unlink(String key) {
        return Boolean.TRUE.equals(redisTemplate.unlink(key));
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.execute((RedisCallback<Boolean>) redisConnection -> redisConnection.exists(key.getBytes())));
    }

    @Override
    public Long size(String key) {
        return redisTemplate.opsForList().size(key);
    }

    @Override
    public Object leftPop(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }

    @Override
    public Object leftPop(String key, long timeout, TimeUnit unit) {
        return redisTemplate.opsForList().leftPop(key, timeout, unit);
    }

    @Override
    public Object multi(Runnable runnable) {
        SessionCallback<Object> sessionCallback = new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                redisOperations.multi();
                runnable.run();
                return redisOperations.exec();
            }
        };
        return redisTemplate.execute(sessionCallback);
    }
}
