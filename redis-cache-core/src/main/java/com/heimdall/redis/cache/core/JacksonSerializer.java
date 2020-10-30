package com.heimdall.redis.cache.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author crh
 * @date 2020-09-12
 */
public class JacksonSerializer implements IJsonSerializer {

    private final ObjectMapper objectMapper;

    public JacksonSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String serializer(Object obj) {
        if (obj == null) {
            return "{}";
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new com.heimdall.redis.cache.core.exception.JsonProcessingException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserializer(String json, Class<?> clz) {
        if (json == null || clz == null) {
            return null;
        }
        try {
            return (T) (clz.equals(String.class) ? json : objectMapper.readValue(json, clz));
        } catch (JsonProcessingException e) {
            throw new com.heimdall.redis.cache.core.exception.JsonProcessingException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T deserializer(String json, TypeReference<T> typeReference) {
        if (json == null || typeReference == null) {
            return null;
        }
        try {
            return (T) (typeReference.getType().equals(String.class) ? json : objectMapper.readValue(json, typeReference));
        } catch (JsonProcessingException e) {
            throw new com.heimdall.redis.cache.core.exception.JsonProcessingException(e);
        }
    }

    @Override
    public <T> T deserializer(String json, Class<?> clz, Class<?>... genericElements) {
        if (json == null || clz == null) {
            return null;
        }
        JavaType javaType = this.getJavaType(clz, genericElements);
        return this.deserializer(json, javaType);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserializer(String json, JavaType javaType) {
        if (json == null || javaType == null) {
            return null;
        }
        try {
            return (T) (javaType.getRawClass().equals(String.class) ? json : objectMapper.readValue(json, javaType));
        } catch (JsonProcessingException e) {
            throw new com.heimdall.redis.cache.core.exception.JsonProcessingException(e);
        }
    }

    /**
     * 获取泛型的Collection Type
     *
     * @param clz
     * @param genericElements
     * @return
     */
    public JavaType getJavaType(Class<?> clz, Class<?>... genericElements) {
        return objectMapper
                .getTypeFactory()
                .constructParametricType(clz, genericElements);
    }

}
