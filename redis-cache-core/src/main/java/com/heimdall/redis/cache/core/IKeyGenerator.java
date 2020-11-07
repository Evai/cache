package com.heimdall.redis.cache.core;

import java.lang.reflect.Method;

/**
 * @author crh
 * @since 2020/11/3
 */
public interface IKeyGenerator {

    /**
     * 缓存key生成方法
     *
     * @param target
     * @param method
     * @param methodParams
     * @return
     */
    String generate(Object target, Method method, MethodParam[] methodParams);

}
