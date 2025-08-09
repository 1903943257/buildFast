package com.jiaojiao.yuaicodemother.exception;

public class BusinessException extends RuntimeException {
    /**
     * 错误码
     */

    private final int code;

    public BusinessException(final int code, final String message) {
        super(message);
        this.code = code;
    }
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    // 这里是重载，根据输入参数匹配不同方法
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}

