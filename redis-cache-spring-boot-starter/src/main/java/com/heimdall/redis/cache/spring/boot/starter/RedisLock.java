package com.heimdall.redis.cache.spring.boot.starter;

import com.heimdall.redis.cache.core.CacheConstant;
import com.heimdall.redis.cache.core.CacheLock;
import com.heimdall.redis.cache.core.exception.GetLockFailedException;
import com.heimdall.redis.cache.core.util.ThreadPoolUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * @author crh
 * @date 2019/6/19
 * @description 分布式锁
 */
public class RedisLock implements CacheLock {

    private static final Logger log = LoggerFactory.getLogger(RedisLock.class);

    private final RedissonClient redissonClient;

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisLock(RedissonClient redissonClient, RedisTemplate<String, Object> redisTemplate) {
        this.redissonClient = redissonClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Lock getLock(String key) {
        return redissonClient.getLock(key);
    }

    @Override
    public boolean tryLock(Lock lock, long expired) {
        if (!(lock instanceof RLock)) {
            return false;
        }

        RLock rLock = (RLock) lock;
        try {
            return rLock.tryLock(0, expired, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("thread-{} interrupt", Thread.currentThread().getName());
            return false;
        }
    }

    @Override
    public <R> R tryLock(String key, long waitTime, long expired, Supplier<R> supplier) throws GetLockFailedException {
        RLock lock = redissonClient.getLock(key);
        boolean getLock = false;
        try {
            getLock = lock.tryLock(waitTime, expired, TimeUnit.SECONDS);
            if (!getLock) {
                throw new GetLockFailedException();
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("thread-{} interrupt", Thread.currentThread().getName());
            return null;
        } finally {
            if (getLock) {
                lock.unlock();
            }
        }
    }

    @Override
    public <R> R tryLock(String key, long expired, Supplier<R> supplier) throws GetLockFailedException {
        return tryLock(key, 0, expired, supplier);
    }

    @Override
    public void tryLock(String key, long expired, Runnable runnable) throws GetLockFailedException {
        tryLock(key, 0, expired, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 续期
     *
     * @param key
     * @param expiring
     * @return
     */
    private void renewalKey(String key, long expiring) {
        // 查询当前key的过期时间
        Long expired = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (expired == null || expired == -2) {
            log.warn("lock key: [{}] is not exist", key);
            return;
        }
        // 如果剩余过期时间小于设置的过期时间，续期
        if (expired < expiring) {
            redisTemplate.expire(key, expiring, TimeUnit.SECONDS);
        }
    }

    @Override
    public <T> T renewalLock(String key, long expired, Supplier<T> supplier) throws GetLockFailedException {
        // 过期时间不能太短，防止后面执行过程中获取不到key的情况
        if (expired < CacheConstant.SECOND_OF_10) {
            expired = CacheConstant.SECOND_OF_10;
        }
        RLock lock = redissonClient.getLock(key);
        boolean getLock = false;
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            getLock = lock.tryLock(0, expired, TimeUnit.SECONDS);
            if (!getLock) {
                throw new GetLockFailedException();
            }
            ExecutorService service = ThreadPoolUtils.newSingleThreadExecutor("renewal-lock-main");
            long finalExpiring = expired;
            service.execute(() -> {
                // 默认执行时间为过期时间的一半，保证能在过期之前续期
                long executeTime = finalExpiring / 2;
                for (; ; ) {
                    try {
                        boolean isFinish = countDownLatch.await(executeTime, TimeUnit.SECONDS);
                        if (isFinish) {
                            break;
                        }
                        renewalKey(key, finalExpiring);
                    } catch (InterruptedException e) {
                        log.error("renewalLock error, key: [{}]", key, e);
                        break;
                    }
                }
            });
            service.shutdown();
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("thread-{} interrupt", Thread.currentThread().getName());
            return null;
        } finally {
            countDownLatch.countDown();
            if (getLock) {
                lock.unlock();
            }
        }
    }

    @Override
    public <T> T renewalLock(String key, Supplier<T> supplier) throws GetLockFailedException {
        return renewalLock(key, CacheConstant.SECOND_OF_30, supplier);
    }

}
