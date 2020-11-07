package com.heimdall.redis.cache.spring.boot.starter;

import com.heimdall.redis.cache.core.CacheConstant;
import com.heimdall.redis.cache.core.IKeyGenerator;
import com.heimdall.redis.cache.core.MethodParam;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * @author crh
 * @since 2020/11/3
 */
public class DefaultKeyGenerator implements IKeyGenerator {

    @Override
    public String generate(Object target, Method method, MethodParam[] methodParams) {
        StringBuilder key = new StringBuilder();
        String classSimpleName = target.getClass().getSimpleName();

        key.append(classSimpleName)
                .append(CacheConstant.COLON)
                .append(method.getName())
                .append(CacheConstant.COLON)
                .append(Arrays.deepHashCode(methodParams));

        return key.toString();
    }

}
