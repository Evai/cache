package com.heimdall.redis.cache.core.exception;

/**
 * @author crh
 * @date 2019-06-11
 * @description 获取锁失败异常
 */
public class GetLockFailedException extends Exception {

    public GetLockFailedException() {}

    public GetLockFailedException(String message) {
        super(message);
    }

    public GetLockFailedException(Throwable cause) {
        super(cause);
    }

    public GetLockFailedException(String message, Throwable cause) {
        super(message, cause);
    }

}
