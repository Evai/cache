package com.heimdall.redis.cache.core.exception;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class IllegalEntityException extends RuntimeException {

    public IllegalEntityException(String message) {
        super(message);
    }

    public IllegalEntityException(Throwable cause) {
        super(cause);
    }

    public IllegalEntityException(String message, Throwable cause) {
        super(message, cause);
    }

}
