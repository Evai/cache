package com.heimdall.redis.cache.spring.boot.starter;


import com.fasterxml.jackson.databind.JavaType;
import com.google.common.collect.Lists;
import com.heimdall.redis.cache.core.*;
import com.heimdall.redis.cache.core.annotation.CacheAble;
import com.heimdall.redis.cache.core.exception.GetLockFailedException;
import com.heimdall.redis.cache.core.exception.IllegalEntityException;
import com.heimdall.redis.cache.core.exception.IllegalEntityValueException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author crh
 * @date 2019-09-23
 * @description
 */
public class CacheComponent {

    public static final Logger log = LoggerFactory.getLogger(CacheComponent.class);

    private final IRedisClient redisClient;
    private final RedisCacheProperties redisCacheProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheLock cacheLock;
    private final JacksonSerializer jacksonSerializer;

    /**
     * 异步更新缓存线程池
     */
    private final ExecutorService cacheExecutor;

    public CacheComponent(IRedisClient redisClient, RedisCacheProperties redisCacheProperties, RedisTemplate<String, Object> redisTemplate, CacheLock cacheLock, JacksonSerializer jacksonSerializer, ExecutorService cacheExecutor) {
        this.redisClient = redisClient;
        this.redisCacheProperties = redisCacheProperties;
        this.redisTemplate = redisTemplate;
        this.cacheLock = cacheLock;
        this.jacksonSerializer = jacksonSerializer;
        this.cacheExecutor = cacheExecutor;
    }

    /**
     * 自增锁次数并设置过期时间
     *
     * @param key
     * @param step
     * @param seconds
     * @return
     */
    public Long incrByEx(String key, long step, long seconds) {
        return redisTemplate.execute((RedisCallback<Long>) redisConnection ->
                redisConnection.eval(CacheConstant.LuaScript.INCRBY_EXPIRE.getBytes(), ReturnType.INTEGER, 2, key.getBytes(), StringUtils.EMPTY.getBytes(), String.valueOf(step).getBytes(), String.valueOf(seconds).getBytes()));
    }

    /**
     * 自减锁次数，如果小于等于0释放锁
     *
     * @param key
     * @param step
     * @return
     */
    public Long decrByRelease(String key, long step) {
        return redisTemplate.execute((RedisCallback<Long>) redisConnection ->
                redisConnection.eval(CacheConstant.LuaScript.DECRBY_RELEASE.getBytes(), ReturnType.INTEGER, 1, key.getBytes(), String.valueOf(step).getBytes()));
    }

    /**
     * 设置 string 并设置过期时间
     *
     * @param writeKey 锁key
     * @param key      存入数据key
     * @param value    存入数据值
     * @param seconds  过期时间
     * @param <T>
     */
    public <T> Boolean setExWithNotExist(String writeKey, String key, T value, long seconds) {
        String result = this.beanToString(value);
        return redisTemplate.execute((RedisCallback<Boolean>) redisConnection ->
                redisConnection.eval(CacheConstant.LuaScript.SET_WITH_NOT_EXIST.getBytes(), ReturnType.BOOLEAN, 2, writeKey.getBytes(), key.getBytes(), String.valueOf(seconds).getBytes(), result.getBytes()));
    }

    /**
     * 新增写锁
     *
     * @param key
     * @param seconds
     * @return
     */
    public Long tryWriteLock(String key, long seconds) {
        return this.incrByEx(key, 1, seconds);
    }

    /**
     * 释放写锁
     *
     * @param key
     * @return
     */
    public Long releaseWriteLock(String key) {
        return this.decrByRelease(key, 1);
    }


    /**
     * 查询缓存，如果key对应的value不存在，存储新的值，并设置过期时间，同步返回最新值
     * 同时检查该缓存是否即将到期，并延长过期时间，防止缓存穿透
     *
     * @param key
     * @param javaType
     * @param seconds
     * @param supplier
     * @param <T>
     * @return
     */
    <T> T getCache(String key, int seconds, int asyncSeconds, JavaType javaType, Supplier<T> supplier) {
        String value = (String) redisClient.get(key);
        // 缓存值不存在或已失效
        if (value == null) {
            return this.writeData(key, seconds, supplier);
        } else {
            // 如果到期时间 < 设置的到期时间，更新缓存数据，防止缓存穿透
            Long expired = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (expired == null || expired < asyncSeconds) {
                // 异步更新方法
                Runnable runnable = () -> this.writeData(key, seconds, supplier);
                asyncUpdateCache(key, runnable);
            }
            // 说明数据库没有该值，无需重复查询数据库
            if (CacheConstant.NULL.equals(value)) {
                return null;
            }
            return this.stringToBean(value, javaType);
        }
    }

    /**
     * 查询实体类缓存，如果key对应的value不存在，存储新的值，并设置过期时间，同步返回最新值
     * 同时检查该缓存是否即将到期，并延长过期时间，防止缓存穿透
     *
     * @param clazz
     * @param seconds
     * @param supplier
     * @param <T>
     * @return
     */
    public <T> T getEntityCache(CacheKey cacheKey, int seconds, int asyncSeconds, Class<T> clazz, CacheAble cacheAble, Supplier<T> supplier) {
        // 通过索引key查询到主键key
        String primaryKey = (String) redisClient.get(cacheKey.getIndexKey());
        // 不存在或已失效
        if (primaryKey == null) {
            return getEntityResult(cacheKey, seconds, cacheAble, supplier);
        } else {
            // 说明数据库没有该值，无需重复查询数据库
            if (StringUtils.equals(primaryKey, CacheConstant.NULL)) {
                return null;
            }

            // 如果存在主键key，查询是否有值
            String entityJson = (String) redisClient.get(primaryKey);
            // 如果不存在，执行业务逻辑查询数据
            if (entityJson == null) {
                return getEntityResult(cacheKey, seconds, cacheAble, supplier);
            }
            // 如果到期时间 < 设置的到期时间，更新缓存数据，防止缓存穿透
            Long primaryKeyExpired = redisClient.getExpire(primaryKey, TimeUnit.SECONDS);
            Long indexKeyExpired = redisClient.getExpire(cacheKey.getIndexKey(), TimeUnit.SECONDS);
            if (indexKeyExpired == null || indexKeyExpired < asyncSeconds) {
                redisClient.expire(cacheKey.getIndexKey(), seconds, TimeUnit.SECONDS);
            }
            if (primaryKeyExpired == null || primaryKeyExpired < asyncSeconds) {
                // 异步更新方法
                Runnable runnable = () -> getEntityResult(cacheKey, seconds, cacheAble, supplier);
                asyncUpdateCache(primaryKey, runnable);
            }

            // 说明数据库没有该值，无需重复查询数据库
            if (StringUtils.equals(entityJson, CacheConstant.NULL)) {
                return null;
            }

            return this.stringToBean(entityJson, clazz);
        }
    }

    private void asyncUpdateCache(String key, Runnable runnable) {
        // 先查询是否已经有该key的读锁，如果有直接返回，避免不必要的查询给数据库造成压力
        final String readKey = getReadLockKey(key);
        try {
            cacheLock.tryLock(readKey, CacheConstant.SECOND_OF_10, () -> {
                try {
                    // 异步更新值
                    cacheExecutor.execute(() -> {
                        try {
                            runnable.run();
                        } catch (Exception e) {
                            log.error("asyncUpdateCache error", e);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    // 当队列任务无法继续执行时，直接让主线程更新缓存
                    runnable.run();
                }
            });
        } catch (GetLockFailedException e) {
            log.warn("getReadLock key: [{}] failed", key);
        }
    }

    private <T> T getEntityResult(CacheKey cacheKey, int seconds, CacheAble cacheAble, Supplier<T> supplier) {
        T result = supplier.get();
        String primaryKey = null;
        // 这里存索引key，值为主键key
        if (result == null) {
            setNullValue(cacheKey.getIndexKey());
            return null;
        } else if (StringUtils.isNotBlank(cacheKey.getPrimaryKey())) {
            primaryKey = cacheKey.getPrimaryKey();
        } else {
            Long id = ((IEntity) result).getId();
            if (id == null) {
                throw new IllegalEntityValueException("result entity id must not be null, entity class name: " + result.getClass().getSimpleName());
            }
            String className = result.getClass().getSimpleName();

            primaryKey = CacheKeyUtils.assemblePrimaryKey(redisCacheProperties, className, id);
        }

        // 判断写锁是否存在，存在则不放入缓存，这里存的是实体类
        Boolean isSuccess = this.setExWithNotExist(getWriteLockKey(primaryKey), primaryKey, result, seconds);
        if (Boolean.TRUE.equals(isSuccess)) {
            // 这里存索引key，值为主键key
            redisClient.set(cacheKey.getIndexKey(), primaryKey, seconds);
        }
        return result;
    }

    private void setNullValue(String key) {
        redisClient.set(key, CacheConstant.NULL, CacheConstant.SECOND_OF_10);
    }

    <T> T writeData(String key, int seconds, Supplier<T> supplier) {
        T result = supplier.get();
        redisClient.set(key, this.beanToString(result), seconds);
        return result;
    }

    void evictData(String key, boolean isPattern) {
        if (isPattern) {
            redisClient.keys(key, redisClient::unlink);
        } else {
            redisClient.unlink(key);
        }
    }

    /**
     * 读锁key
     *
     * @param key
     * @return
     */
    private String getReadLockKey(String key) {
        return CacheConstant.READ_LOCK + key;
    }

    /**
     * 写锁key
     *
     * @param key
     * @return
     */
    private String getWriteLockKey(String key) {
        return CacheConstant.WRITE_LOCK + key;
    }

    private void delete(CacheKey cacheKey) {
        redisClient.unlink(Lists.newArrayList(cacheKey.getPrimaryKey(), cacheKey.getIndexKey()));
    }

    public void delPattern(String key) {
        long cursorId = 0L;
        for (; ; ) {
            ScanCursor<String> scanCursor = redisClient.scan(cursorId, "*" + key + "*");
            if (scanCursor == null || CollectionUtils.isEmpty(scanCursor.getItems())) {
                break;
            }
            redisClient.unlink(scanCursor.getItems());
            cursorId = scanCursor.getCursorId();
        }
    }

//    /**
//     * 删除指定缓存key
//     *
//     * @param keyNamePrefix
//     * @param keyNameSuffix
//     * @param keyNameClass
//     * @param keyFormat
//     */
//    public Long deleteByKey(String keyNamePrefix, String keyNameSuffix, Class keyNameClass, KeyFormat keyFormat, Object keyObj) {
//        String key = cacheKeyUtils.assembleKey(keyNamePrefix, keyNameSuffix, keyNameClass, keyFormat, keyObj);
//        return redisClient.unlink(Collections.singleton(key));
//    }


    /**
     * String 转 实体类
     *
     * @param value
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T stringToBean(String value, Class<T> clazz) {
        return jacksonSerializer.deserializer(value, clazz);
    }

    /**
     * String 转 实体类
     *
     * @param value
     * @param javaType
     * @param <T>
     * @return
     */
    public <T> T stringToBean(String value, JavaType javaType) {
        return jacksonSerializer.deserializer(value, javaType);
    }

    /**
     * 对象 转 String
     *
     * @param obj
     * @param <T>
     * @return
     */
    public <T> String beanToString(T obj) {
        if (obj == null) {
            return CacheConstant.NULL;
        }
        Class<?> clazz = obj.getClass();
        if (String.class == clazz) {
            return (String) obj;
        } else if (BeanUtils.isNumber(clazz)) {
            return String.valueOf(obj);
        } else {
            return jacksonSerializer.serializer(obj);
        }
    }

}
