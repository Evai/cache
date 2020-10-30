package com.heimdall.redis.cache.core.annotation;




import com.heimdall.redis.cache.core.CacheAction;
import com.heimdall.redis.cache.core.KeyFormat;

import java.lang.annotation.*;

/**
 * @author crh
 * @date 2019-06-17
 * @description 实体类缓存注解，缓存值和索引key，有新增，删除动作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheAbleEntity {
    /**
     * 缓存id，如果是以"#"开头，则获取当前方法参数值
     */
    String key();

    /**
     * 缓存动作
     */
    CacheAction action();

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
    int[] expired() default {300, 600};

    /**
     * 异步更新缓存时间阈值，当过期时间小于该值时，会查询数据库最新的数据同步到缓存中（CacheAction为SELECT有效）
     */
    int asyncSeconds() default 30;

    /**
     * 更新数据的加锁时间，单位秒（CacheAction为UPDATE有效）
     */
    int lockSeconds() default 30;

}
