package com.heimdall.redis.cache.spring.boot.starter;

import com.fasterxml.jackson.databind.JavaType;
import com.heimdall.redis.cache.core.IKeyGenerator;
import com.heimdall.redis.cache.core.JacksonSerializer;
import com.heimdall.redis.cache.core.annotation.CachePut;
import com.heimdall.redis.cache.core.exception.IllegalEntityException;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Type;

/**
 * @author crh
 * @date 2019/6/19
 * @description 缓存切面
 */
@Aspect
@Order(50)
public class CachePutAspect {

    private final CacheComponent cacheComponent;
    private final JacksonSerializer jacksonSerializer;
    private final RedisCacheProperties redisCacheProperties;
    private final IKeyGenerator keyGenerator;

    public CachePutAspect(CacheComponent cacheComponent, JacksonSerializer jacksonSerializer, RedisCacheProperties redisCacheProperties, IKeyGenerator keyGenerator) {
        this.cacheComponent = cacheComponent;
        this.jacksonSerializer = jacksonSerializer;
        this.redisCacheProperties = redisCacheProperties;
        this.keyGenerator = keyGenerator;
    }

    @Around(value = "@annotation(cachePut)")
    public Object around(ProceedingJoinPoint pjp, CachePut cachePut) throws Throwable {
        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;
        int expiredSeconds = CacheKeyUtils.randomExpired(cachePut.expired());
        // 方法返回类型
        Class returnType = methodSignature.getReturnType();

        // 方法返回类型
        Object[] args = pjp.getArgs();

        String key = CacheKeyUtils.generateKey(cachePut.key(), keyGenerator, pjp.getTarget(), methodSignature, args);

        String keyPrefix = CacheKeyUtils.getKeyPrefix(redisCacheProperties.getKeyPrefix());

        return cacheComponent.writeData(keyPrefix + key, expiredSeconds, () -> proceed(pjp));
    }


    /**
     * 执行业务逻辑
     *
     * @param pjp
     * @return
     */
    private Object proceed(ProceedingJoinPoint pjp) {
        try {
            return pjp.proceed();
        } catch (Throwable throwable) {
            doThrow(throwable);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void doThrow(Throwable e) throws E {
        throw (E) e;
    }

}
