package com.heimdall.redis.cache.core.annotation;


import java.lang.annotation.*;

/**
 * @author crh
 * @date 2019-06-17
 * @description 删除缓存注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {

    /**
     * 缓存key, 不填则使用默认的DefaultKeyGenerator生成器, 支持spel表达式
     */
    String key() default "";

    /**
     * key是否是模糊匹配
     */
    boolean pattern() default false;

}
