package com.heimdall.redis.cache.spring.boot.starter;


import com.heimdall.redis.cache.core.CacheConstant;
import com.heimdall.redis.cache.core.IKeyGenerator;
import com.heimdall.redis.cache.core.MethodParam;
import org.apache.commons.lang3.RandomUtils;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

/**
 * @author crh
 * @date 2019-06-23
 * description
 */
public class CacheKeyUtils {

    public static String generateKey(String key, IKeyGenerator keyGenerator, Object target, MethodSignature methodSignature, Object[] args) {
        MethodParam[] methodParams = new MethodParam[args.length];
        for (int i = 0; i < args.length; i++) {
            methodParams[i] = new MethodParam(methodSignature.getParameterNames()[i], args[i]);
        }
        if (key.isEmpty()) {
            key = keyGenerator.generate(target, methodSignature.getMethod(), methodParams);
        } else {
            SpelExpressionParser spelExpressionParser = new SpelExpressionParser();
            EvaluationContext context = new StandardEvaluationContext();
            for (MethodParam param : methodParams) {
                context.setVariable(param.getName(), param.getValue());
            }
            key = spelExpressionParser.parseExpression(key).getValue(context, String.class);
        }
        return key;
    }

    public static String getKeyPrefix(String globalKeyPrefix) {
        String keyPrefix = "";
        if (StringUtils.hasText(globalKeyPrefix)) {
            keyPrefix += globalKeyPrefix + CacheConstant.COLON;
        }
        return keyPrefix;
    }

    /**
     * 生成随机过期时间，防止缓存雪崩
     *
     * @param expired
     * @return
     */
    public static int randomExpired(int[] expired) {
        if (expired.length > 1) {
            int start = expired[0];
            int end = expired[1];
            return RandomUtils.nextInt(start, end);
        } else if (expired.length == 1 && expired[0] > 0) {
            return expired[0];
        }
        return RandomUtils.nextInt(30, 60);
    }

    public static String assemblePrimaryKey(RedisCacheProperties redisCacheProperties, String className, Object value) {
        return CacheKeyUtils.getKeyPrefix(redisCacheProperties.getKeyPrefix()) +
                className +
                CacheConstant.COLON +
                redisCacheProperties.getPrimaryKey() +
                CacheConstant.COLON +
                value;
    }

}
