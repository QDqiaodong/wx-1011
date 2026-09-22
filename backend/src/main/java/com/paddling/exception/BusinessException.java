package com.paddling.exception;

/**
 * 业务规则异常（如已封账、参数非法等），由全局异常处理器统一转成明确的失败提示。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
