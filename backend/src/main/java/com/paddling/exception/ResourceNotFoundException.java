package com.paddling.exception;

/** 请求的资源不存在，映射 HTTP 404（例如查询尚未封账的月份）。 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(404, 404, message);
    }
}
