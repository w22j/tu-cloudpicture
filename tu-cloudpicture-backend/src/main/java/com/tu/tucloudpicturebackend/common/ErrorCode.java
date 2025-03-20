package com.tu.tucloudpicturebackend.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "操作成功"),
    PARAMS_ERROR(40000, "请求参数错误"),
    NO_LOGIN_AUTH_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "未授权"),
    FORBIDDEN_ERROR(40300, "禁止访问"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),
    SYSTEM_ERROR(50000, "系统错误"),
    OPERATION_ERROR(50001, "操作失败")
    ;

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
