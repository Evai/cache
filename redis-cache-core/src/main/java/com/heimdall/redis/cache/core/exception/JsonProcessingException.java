package com.heimdall.redis.cache.core.exception;

/**
 * @author crh
 * @date 2019-06-11
 */
public class JsonProcessingException extends RuntimeException {

    public JsonProcessingException() {}

    public JsonProcessingException(String message) {
        super(message);
    }

    public JsonProcessingException(Throwable cause) {
        super(cause);
    }

    public JsonProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

}
