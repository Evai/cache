package com.heimdall.redis.cache.spring.boot.starter;

import com.heimdall.redis.cache.core.CacheAction;
import com.heimdall.redis.cache.core.CacheConstant;
import com.heimdall.redis.cache.core.annotation.CacheAbleEntity;
import com.heimdall.redis.cache.core.exception.IllegalEntityException;
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

import java.util.Arrays;

/**
 * @author crh
 * @date 2019/6/19
 * @description 缓存切面
 */
@Aspect
@Order(50)
public class CacheAbleEntityAspect {

    private final CacheComponent cacheComponent;
    private final RedisCacheProperties redisCacheProperties;

    public CacheAbleEntityAspect(CacheComponent cacheComponent, RedisCacheProperties redisCacheProperties) {
        this.cacheComponent = cacheComponent;
        this.redisCacheProperties = redisCacheProperties;
    }

    @Around(value = "@annotation(cacheAbleEntity)")
    public Object around(ProceedingJoinPoint pjp, CacheAbleEntity cacheAbleEntity) throws Throwable {
        int asyncSeconds = cacheAbleEntity.asyncSeconds();
        int[] expired = cacheAbleEntity.expired();
        int lockSeconds = cacheAbleEntity.lockSeconds();
        CacheAction cacheAction = cacheAbleEntity.action();

        Object[] args = pjp.getArgs();
        Signature signature = pjp.getSignature();
        MethodSignature methodSignature = (MethodSignature) signature;

        String[] parameterNames = methodSignature.getParameterNames();

        SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
        EvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        String key = spelExpressionParser.parseExpression(cacheAbleEntity.key()).getValue(context, String.class);

        String keyPrefix = assembleKeyPrefix(cacheAbleEntity, pjp, methodSignature);

        CacheKey cacheKey = assembleCacheKey(keyPrefix, key, args, methodSignature, cacheAbleEntity);

        switch (cacheAction) {
            case INSERT_AUTO:
                return cacheComponent.insertAutoData(cacheKey, lockSeconds, () -> proceed(pjp));
            case DEL:
                return cacheComponent.writeData(cacheKey, lockSeconds, () -> proceed(pjp));
            case SELECT:
            default:
                // 方法返回类型
                Class returnType = methodSignature.getReturnType();

                Arrays.stream(returnType.getInterfaces())
                        .filter(IEntity.class::equals)
                        .findFirst()
                        .orElseThrow(() -> new IllegalEntityException("method return type must be implement IEntity"));

                int expiredSeconds = CacheKeyUtils.randomExpired(expired);
                return cacheComponent.getEntityCache(cacheKey, expiredSeconds, asyncSeconds, returnType, cacheAbleEntity, () -> proceed(pjp));
        }
    }

    private String assembleKeyPrefix(CacheAbleEntity cacheAbleEntity, ProceedingJoinPoint pjp, MethodSignature methodSignature) {
        String keyPrefix = "";

        String classSimpleName = pjp.getTarget().getClass().getSimpleName();

        keyPrefix += CacheKeyUtils.getKeyPrefix(cacheAbleEntity, redisCacheProperties.getKeyPrefix());

        if (cacheAbleEntity.addKeyClass()) {
            keyPrefix += classSimpleName + CacheConstant.COLON;
        }
        if (cacheAbleEntity.addKeyMethod()) {
            keyPrefix += methodSignature.getName() + CacheConstant.COLON;
        }

        keyPrefix += CacheKeyUtils.getKeySuffix(cacheAbleEntity, redisCacheProperties.getKeySuffix());

        return keyPrefix;
    }


    /**
     * 组合为最终的缓存key
     *
     * @param args
     * @param keyPrefix
     * @param key
     * @param methodSignature
     * @param cacheAbleEntity
     * @return
     */
    public CacheKey assembleCacheKey(String keyPrefix, String key, Object[] args, MethodSignature methodSignature, CacheAbleEntity cacheAbleEntity) {
        String indexKey = keyPrefix + key;
        String primaryKey = null;
        Class returnType = methodSignature.getReturnType();
        String className = returnType.getSimpleName();

        if (args.length == 1) {
            Object value = args[0];
            String[] parameterNames = methodSignature.getParameterNames();
            String parameterName = parameterNames[0];
            //如果请求参数只有一个, 并且是Long类型且参数名为id, 设置主键key
            if (value instanceof Long && CacheConstant.PK.equals(parameterName)) {
                primaryKey = CacheKeyUtils.assemblePrimaryKey(cacheAbleEntity, redisCacheProperties, className, value);
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
