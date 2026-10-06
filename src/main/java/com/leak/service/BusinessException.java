package com.leak.service;

/** 业务规则违例，映射为 400 响应。 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
