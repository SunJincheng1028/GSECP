package com.gsecp.common;

/**
 * 统一响应状态码，与 HTTP 状态保持一致，便于前端拦截器统一处理。
 */
public enum ResultCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限执行此操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源状态冲突"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
