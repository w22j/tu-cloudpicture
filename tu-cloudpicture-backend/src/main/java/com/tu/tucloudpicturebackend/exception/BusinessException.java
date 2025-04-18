package com.tu.tucloudpicturebackend.exception;

import com.tu.tucloudpicturebackend.common.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException{

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.SYSTEM_ERROR.getCode();
    }
}
