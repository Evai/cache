package com.heimdall.redis.cache.core.annotation;


import java.lang.annotation.*;

/**
 * @author crh
 * @date 2019-06-17
 * @description 更新缓存注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePut {

    /**
     * 缓存key, 不填则使用默认的DefaultKeyGenerator生成器, 支持spel表达式
     */
    String key() default "";

    /**
     * 缓存过期随机时间，可以自定义随机时间范围
     */
    int[] expired() default {300, 600};

}
