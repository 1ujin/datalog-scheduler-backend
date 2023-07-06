package com.cesi.datalogscheduler.enums;

public enum ResponseCodeEnum {
    /**
     * 操作成功
     */
    OK(0, "操作成功"),
    /**
     * 操作失败
     */
    ERROR(1, "操作失败"),
    /**
     * 验证失败
     */
    INVALID(2, "验证失败");

    /**
     * 自定义状态码
     */
    private final int code;
    /**
     * 自定义描述
     */
    private final String msg;

    ResponseCodeEnum(int code, String message) {
        this.code = code;
        this.msg = message;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
