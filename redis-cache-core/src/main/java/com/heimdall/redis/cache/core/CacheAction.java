package com.heimdall.redis.cache.core;

/**
 * @author crh
 * @date 2019-06-17
 * @description
 */
public enum CacheAction {
    /**
     * 新增数据，主键为自增id，根据自增id删除缓存
     */
    INSERT,
    /**
     * 先查询是否已经有缓存，有会使用缓存，没有则会执行方法并缓存。
     */
    SELECT,
    /**
     * 删除缓存
     */
    DEL,
    ;
}
