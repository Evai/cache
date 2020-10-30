package com.heimdall.redis.cache.core.exception;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class IllegalKeyIdException extends RuntimeException {

    public IllegalKeyIdException(String message) {
        super(message);
    }

    public IllegalKeyIdException(Throwable cause) {
        super(cause);
    }

    public IllegalKeyIdException(String message, Throwable cause) {
        super(message, cause);
    }

}
