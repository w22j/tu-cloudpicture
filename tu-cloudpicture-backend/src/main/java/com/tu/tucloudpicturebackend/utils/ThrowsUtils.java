package com.tu.tucloudpicturebackend.utils;

import com.tu.tucloudpicturebackend.common.ErrorCode;
import com.tu.tucloudpicturebackend.exception.BusinessException;

/**
 * 异常抛出工具类
 */
public class ThrowsUtils {

    public static void throwIf(boolean condition, RuntimeException runtimeException) {
        if (condition) {
            throw runtimeException;
        }
    }

    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
