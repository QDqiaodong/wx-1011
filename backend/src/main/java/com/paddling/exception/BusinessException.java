package com.paddling.exception;

/**
 * 业务异常基类：携带 HTTP 状态码，由 GlobalExceptionHandler 映射。
 */
public class BusinessException extends RuntimeException {
    private final int code;
    private final int httpStatus;

    public BusinessException(String message) {
        this(400, 400, message);
    }

    public BusinessException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
