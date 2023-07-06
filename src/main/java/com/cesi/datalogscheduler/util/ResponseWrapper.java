package com.cesi.datalogscheduler.util;

import com.cesi.datalogscheduler.enums.ResponseCodeEnum;

import java.util.HashMap;

@SuppressWarnings({"serial", "unused"})
public class ResponseWrapper extends HashMap<String, Object> {

    public static ResponseWrapper of(ResponseCodeEnum responseCodeEnum) {
        return new ResponseWrapper().setCodeEnum(responseCodeEnum);
    }

    public static ResponseWrapper ok() {
        return ResponseWrapper.of(ResponseCodeEnum.OK);
    }

    public static ResponseWrapper error() {
        return ResponseWrapper.of(ResponseCodeEnum.ERROR);
    }

    public static ResponseWrapper invalid() {
        return ResponseWrapper.of(ResponseCodeEnum.INVALID);
    }

    public ResponseWrapper setCodeEnum(ResponseCodeEnum responseCodeEnum) {
        put("code", responseCodeEnum.getCode());
        put("msg", responseCodeEnum.getMsg());
        return this;
    }

    public Integer getCode() {
        return (Integer) get("code");
    }

    public ResponseWrapper setCode(int code) {
        put("code", code);
        return this;
    }

    public Object getData() {
        return get("data");
    }

    public ResponseWrapper setData(Object data) {
        put("data", data);
        return this;
    }

    public String getMsg() {
        return String.valueOf(get("msg"));
    }

    public ResponseWrapper setMsg(String data) {
        put("msg", data);
        return this;
    }

    @Override
    public ResponseWrapper put(String key, Object value) {
        super.put(key, value);
        return this;
    }
}
