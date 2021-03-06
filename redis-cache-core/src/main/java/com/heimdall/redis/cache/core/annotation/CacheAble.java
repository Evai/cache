package com.heimdall.redis.cache.core.annotation;


import java.lang.annotation.*;

/**
 * @author crh
 * @date 2019-06-17
 * @description 实体类缓存注解，缓存值和索引key，有新增，删除动作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheAble {
    /**
     * 缓存key, 不填则使用默认的DefaultKeyGenerator生成器, 支持spel表达式
     */
    String key() default "";

    /**
     * 缓存过期随机时间，可以自定义随机时间范围
     */
    int[] expired() default {300, 600};

    /**
     * 异步更新缓存时间阈值，当过期时间小于该值时，会查询数据库最新的数据同步到缓存中（CacheAction为SELECT有效）
     */
    int asyncSeconds() default 30;

}
