package com.heimdall.redis.cache.core.exception;

/**
 * @author crh
 * @date 2019-06-11
 * @description
 */
public class IllegalEntityValueException extends RuntimeException {

    public IllegalEntityValueException(String message) {
        super(message);
    }

    public IllegalEntityValueException(Throwable cause) {
        super(cause);
    }

    public IllegalEntityValueException(String message, Throwable cause) {
        super(message, cause);
    }

}
