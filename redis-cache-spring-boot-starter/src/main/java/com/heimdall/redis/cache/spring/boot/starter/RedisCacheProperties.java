package com.heimdall.redis.cache.spring.boot.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author crh
 * @date 2019-07-12
 * @description 缓存默认全局配置参数
 */
@ConfigurationProperties("com.heimdall.redis.cache")
public class RedisCacheProperties {
    /**
     * 主键
     */
    private String primaryKey = "id";
    /**
     * 缓存key前缀
     */
    private String keyPrefix = "Heimdall";

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

}
