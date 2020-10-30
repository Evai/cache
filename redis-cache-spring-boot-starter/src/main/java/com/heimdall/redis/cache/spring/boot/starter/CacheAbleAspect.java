package com.heimdall.redis.cache.spring.boot.starter;

import com.fasterxml.jackson.databind.JavaType;
import com.heimdall.redis.cache.core.annotation.CacheAble;
import com.heimdall.redis.cache.core.CacheConstant;
import com.heimdall.redis.cache.core.JacksonSerializer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;

/**
 * @author crh
 * @date 2019/6/19
 * @description 通用缓存切面
 */
@Aspect
@Order(50)
public class CacheAbleAspect {

    private final CacheComponent cacheComponent;
    private final JacksonSerializer jacksonSerializer;
    private final RedisCacheProperties redisCacheProperties;

    public CacheAbleAspect(CacheComponent cacheComponent, JacksonSerializer jacksonSerializer, RedisCacheProperties redisCacheProperties) {
        this.cacheComponent = cacheComponent;
        this.jacksonSerializer = jacksonSerializer;
        this.redisCacheProperties = redisCacheProperties;
    }

    @Around(value = "@annotation(cacheAble)")
    public Object around(ProceedingJoinPoint pjp, CacheAble cacheAble) throws Throwable {
        int asyncSeconds = cacheAble.asyncSeconds();
        int[] expired = cacheAble.expired();

        Signature signature = pjp.getSignature();
        Object[] args = pjp.getArgs();
        MethodSignature methodSignature = (MethodSignature) signature;

        int expiredSeconds = CacheKeyUtils.randomExpired(expired);

        // 方法返回类型
        Class returnType = methodSignature.getReturnType();
        Type[] types = BeanUtils.getMethodGenericClass(methodSignature);

        JavaType javaType;
        if (types.length > 0) {
            Class[] classes = BeanUtils.toArray(types, Class.class);
            javaType = jacksonSerializer.getJavaType(returnType, classes);
        } else {
            javaType = jacksonSerializer.getJavaType(returnType);
        }

        String[] parameterNames = methodSignature.getParameterNames();

        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
        EvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        String key = spelExpressionParser.parseExpression(cacheAble.key()).getValue(context, String.class);

        String keyPrefix = assembleKeyPrefix(cacheAble, pjp, methodSignature);

        return cacheComponent.getCache(keyPrefix + key, expiredSeconds, asyncSeconds, javaType, () -> this.proceed(pjp));
    }

    private String assembleKeyPrefix(CacheAble cacheAble, ProceedingJoinPoint pjp, MethodSignature methodSignature) {
        String keyPrefix = "";

        String keyNamePrefix = redisCacheProperties.getKeyPrefix();
        String keyNameSuffix = redisCacheProperties.getKeySuffix();

        if (StringUtils.hasText(cacheAble.keyPrefix())) {
            keyPrefix += cacheAble.keyPrefix() + CacheConstant.COLON;
        } else if (StringUtils.hasText(keyNamePrefix)) {
            keyPrefix += keyNamePrefix + CacheConstant.COLON;
        }
        if (cacheAble.addKeyClass()) {
            keyPrefix += pjp.getTarget().getClass().getSimpleName() + CacheConstant.COLON;
        }
        if (cacheAble.addKeyMethod()) {
            keyPrefix += methodSignature.getName() + CacheConstant.COLON;
        }
        if (StringUtils.hasText(cacheAble.keySuffix())) {
            keyPrefix += cacheAble.keySuffix() + CacheConstant.COLON;
        } else if (StringUtils.hasText(keyNameSuffix)) {
            keyPrefix += keyNameSuffix + CacheConstant.COLON;
        }
        return keyPrefix;
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
