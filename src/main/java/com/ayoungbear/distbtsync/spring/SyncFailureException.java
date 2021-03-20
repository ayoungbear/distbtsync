package com.ayoungbear.distbtsync.spring;

/**
 * 同步失败异常.
 * 
 * @author yangzexiong
 */
@SuppressWarnings("serial")
public class SyncFailureException extends RuntimeException {

    public SyncFailureException(String msg) {
        super(msg);
    }

    public SyncFailureException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
