package com.heimdall.redis.cache.core;

/**
 * @author crh
 * @date 2020-09-12
 */
public interface IJsonSerializer {
    /**
     * 序列化
     *
     * @param object 待序列化的实体
     * @return json
     * @throws Exception
     */
    String serializer(Object object);

    /**
     * 反序列化
     * @param json json String
     * @param clz 反序列化生成的类
     * @param <T>
     * @return T
     * @throws Exception
     */
    <T> T deserializer(String json, Class<?> clz);

    /**
     * 反序列化
     * @param json json String
     * @param clz 反序列化生成的类
     * @param genericElements 泛型类
     * @param <T>
     * @return T
     * @throws Exception
     */
    <T> T deserializer(String json, Class<?> clz, Class<?>... genericElements);

}
