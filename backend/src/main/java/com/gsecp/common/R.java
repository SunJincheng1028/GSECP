package com.gsecp.common;

import java.time.Instant;

/**
 * 统一响应体：{ code, message, data, timestamp }。
 */
public class R<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;

    public R() {
        this.timestamp = Instant.now().toEpochMilli();
    }

    private R(int code, String message, T data) {
        this();
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.code(), ResultCode.SUCCESS.message(), data);
    }

    public static R<Void> ok() {
        return new R<>(ResultCode.SUCCESS.code(), ResultCode.SUCCESS.message(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.code(), resultCode.message(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return new R<>(resultCode.code(), message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
