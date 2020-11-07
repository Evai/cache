package com.heimdall.redis.cache.spring.boot.starter;

import com.fasterxml.jackson.databind.JavaType;
import com.heimdall.redis.cache.core.IKeyGenerator;
import com.heimdall.redis.cache.core.JacksonSerializer;
import com.heimdall.redis.cache.core.annotation.CacheAble;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * @author crh
 * @date 2019/6/19
 * @description 缓存切面
 */
@Aspect
@Order(50)
public class CacheAbleAspect {

    private final CacheComponent cacheComponent;
    private final JacksonSerializer jacksonSerializer;
    private final RedisCacheProperties redisCacheProperties;
    private final IKeyGenerator keyGenerator;

    public CacheAbleAspect(CacheComponent cacheComponent, JacksonSerializer jacksonSerializer, RedisCacheProperties redisCacheProperties, IKeyGenerator keyGenerator) {
        this.cacheComponent = cacheComponent;
        this.jacksonSerializer = jacksonSerializer;
        this.redisCacheProperties = redisCacheProperties;
        this.keyGenerator = keyGenerator;
    }

    @Around(value = "@annotation(cacheAble)")
    public Object around(ProceedingJoinPoint pjp, CacheAble cacheAble) throws Throwable {
        int asyncSeconds = cacheAble.asyncSeconds();
        int[] expired = cacheAble.expired();
        int expiredSeconds = CacheKeyUtils.randomExpired(expired);

        Object[] args = pjp.getArgs();
        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;

        // 方法返回类型
        Class returnType = methodSignature.getReturnType();

        String key = CacheKeyUtils.generateKey(cacheAble.key(), keyGenerator, pjp.getTarget(), methodSignature, args);

        String keyPrefix = CacheKeyUtils.getKeyPrefix(redisCacheProperties.getKeyPrefix());

        boolean match = Arrays.asList(returnType.getInterfaces()).contains(IEntity.class);

        if (match) {
            CacheKey cacheKey = assembleCacheKey(keyPrefix, key, args, methodSignature);
            return cacheComponent.getEntityCache(cacheKey, expiredSeconds, asyncSeconds, returnType, cacheAble, () -> proceed(pjp));
        }

        Type[] types = BeanUtils.getMethodGenericClass(methodSignature);
        JavaType javaType;
        if (types.length > 0) {
            Class[] classes = BeanUtils.toArray(types, Class.class);
            javaType = jacksonSerializer.getJavaType(returnType, classes);
        } else {
            javaType = jacksonSerializer.getJavaType(returnType);
        }
        return cacheComponent.getCache(keyPrefix + key, expiredSeconds, asyncSeconds, javaType, () -> this.proceed(pjp));
    }

    /**
     * 组合为最终的缓存key
     *
     * @param args
     * @param keyPrefix
     * @param key
     * @param methodSignature
     * @return
     */
    public CacheKey assembleCacheKey(String keyPrefix, String key, Object[] args, MethodSignature methodSignature) {
        String indexKey = keyPrefix + key;
        String primaryKey = null;
        Class returnType = methodSignature.getReturnType();
        String className = returnType.getSimpleName();

        if (args.length == 1) {
            Object value = args[0];
            String[] parameterNames = methodSignature.getParameterNames();
            String parameterName = parameterNames[0];
            //如果请求参数只有一个, 并且是Long类型且参数名为配置的主键名, 设置主键key
            if (value instanceof Long && redisCacheProperties.getPrimaryKey().equals(parameterName)) {
                primaryKey = CacheKeyUtils.assemblePrimaryKey(redisCacheProperties, className, value);
            }
        }

        return new CacheKey(keyPrefix, primaryKey, indexKey);
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
