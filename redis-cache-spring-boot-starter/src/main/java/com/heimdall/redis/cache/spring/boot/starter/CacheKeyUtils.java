package com.heimdall.redis.cache.spring.boot.starter;


import com.heimdall.redis.cache.core.CacheConstant;
import com.heimdall.redis.cache.core.KeyFormat;
import com.heimdall.redis.cache.core.annotation.CacheAbleEntity;
import com.heimdall.redis.cache.core.exception.IllegalGenericTypeException;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.ParameterizedType;

/**
 * @author crh
 * @date 2019-06-23
 * description
 */
public class CacheKeyUtils {

    public static String getKeyPrefix(CacheAbleEntity cacheAbleEntity, String globalKeyPrefix) {
        String keyPrefix = "";
        if (org.springframework.util.StringUtils.hasText(cacheAbleEntity.keyPrefix())) {
            keyPrefix += cacheAbleEntity.keyPrefix() + CacheConstant.COLON;
        } else if (org.springframework.util.StringUtils.hasText(globalKeyPrefix)) {
            keyPrefix += globalKeyPrefix + CacheConstant.COLON;
        }
        return keyPrefix;
    }

    public static String getKeySuffix(CacheAbleEntity cacheAbleEntity, String globalKeySuffix) {
        String keySuffix = "";
        if (org.springframework.util.StringUtils.hasText(cacheAbleEntity.keySuffix())) {
            keySuffix += cacheAbleEntity.keySuffix() + CacheConstant.COLON;
        } else if (org.springframework.util.StringUtils.hasText(globalKeySuffix)) {
            keySuffix += globalKeySuffix + CacheConstant.COLON;
        }
        return keySuffix;
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

    public static String assemblePrimaryKey(CacheAbleEntity cacheAbleEntity, RedisCacheProperties redisCacheProperties, String className, Object value) {
        return CacheKeyUtils.getKeyPrefix(cacheAbleEntity, redisCacheProperties.getKeyPrefix()) +
                CacheKeyUtils.getKeySuffix(cacheAbleEntity, redisCacheProperties.getKeySuffix()) +
                className +
                CacheConstant.COLON +
                CacheConstant.PK +
                CacheConstant.COLON +
                value;
    }

    public Class getGenericType(Object obj, int index) {
        try {
            return (Class) ((ParameterizedType) obj
                    .getClass()
                    .getGenericSuperclass())
                    .getActualTypeArguments()[index];
        } catch (Exception e) {
            throw new IllegalGenericTypeException("not found generic type");
        }
    }

}
