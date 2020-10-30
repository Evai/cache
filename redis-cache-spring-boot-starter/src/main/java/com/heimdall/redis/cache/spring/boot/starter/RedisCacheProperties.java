package com.heimdall.redis.cache.spring.boot.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author crh
 * @date 2019-07-12
 * @description 缓存默认全局配置参数
 */
@ConfigurationProperties("com.heimdall.redis.cache")
public class RedisCacheProperties {

    private String keyPrefix;
    private String keySuffix;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getKeySuffix() {
        return keySuffix;
    }

    public void setKeySuffix(String keySuffix) {
        this.keySuffix = keySuffix;
    }

}
