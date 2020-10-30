package com.heimdall.redis.cache.core.annotation;


import com.heimdall.redis.cache.core.KeyFormat;

import java.lang.annotation.*;

/**
 * @author crh
 * @date 2019-06-17
 * @description 公共查询缓存注解，只做缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheAble {

    /**
     * 缓存key, 支持spel表达式
     */
    String key();

    /**
     * 是否取类名拼接key
     */
    boolean addKeyClass() default true;

    /**
     * 是否取方法名拼接key
     */
    boolean addKeyMethod() default true;

    /**
     * key格式转换
     */
    KeyFormat keyFormat() default KeyFormat.CAMEL;

    /**
     * 缓存名称前缀
     */
    String keyPrefix() default "";

    /**
     * 缓存名称后缀
     */
    String keySuffix() default "";

    /**
     * 缓存过期随机时间，可以自定义随机时间范围
     */
    int[] expired() default {45, 90};

    /**
     * 异步更新缓存时间阈值，当过期时间小于该值时，会查询数据库最新的数据同步到缓存中
     */
    int asyncSeconds() default 30;

}
