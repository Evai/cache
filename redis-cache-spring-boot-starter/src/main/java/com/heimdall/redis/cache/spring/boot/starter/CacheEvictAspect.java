package com.heimdall.redis.cache.spring.boot.starter;

import com.heimdall.redis.cache.core.annotation.CacheEvict;
import com.heimdall.redis.cache.core.annotation.CachePut;
import com.heimdall.redis.cache.core.exception.IllegalEntityException;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

/**
 * @author crh
 * @date 2019/6/19
 * @description 缓存切面
 */
@Aspect
@Order(50)
public class CacheEvictAspect {

    private final CacheComponent cacheComponent;

    public CacheEvictAspect(CacheComponent cacheComponent) {
        this.cacheComponent = cacheComponent;
    }

    @Around(value = "@annotation(cacheEvict)")
    public Object around(ProceedingJoinPoint pjp, CacheEvict cacheEvict) throws Throwable {
         cacheComponent.evictData(cacheEvict.key(), cacheEvict.pattern());
         return pjp.proceed();
    }

}
