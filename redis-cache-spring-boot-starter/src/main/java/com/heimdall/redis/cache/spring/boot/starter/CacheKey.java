package com.heimdall.redis.cache.spring.boot.starter;

/**
 * @author crh
 * @date 2019-06-23
 * description 缓存key传输对象
 */
public class CacheKey {

    /**
     *
     */
    private final String keyPrefix;

    /**
     * 主键key，存放实体类对象
     */
    private final String primaryKey;

    /**
     * 索引key，存主键id，可以关联到主键key的实体类
     */
    private final String indexKey;

    public CacheKey(String keyPrefix, String primaryKey, String indexKey) {
        this.keyPrefix = keyPrefix;
        this.primaryKey = primaryKey;
        this.indexKey = indexKey;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public String getIndexKey() {
        return indexKey;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }
}
