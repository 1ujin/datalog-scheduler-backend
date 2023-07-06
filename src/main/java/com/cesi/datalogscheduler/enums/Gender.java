package com.cesi.datalogscheduler.enums;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum Gender {
    MALE((byte) 0, "男"),
    FEMALE((byte) 1, "女");

    private final byte code;
    private final String zh_CN;

    Gender(byte code, String zh_CN) {
        this.code = code;
        this.zh_CN = zh_CN;
    }

    public byte getCode() {
        return code;
    }

    public String getZh_CN() {
        return zh_CN;
    }

    private static final Map<String, Gender> zh_CNStringToEnum = Stream.of(values()).collect(Collectors.toMap(Gender::getZh_CN, e -> e));

    private static final Map<Byte, Gender> codeToEnum = Stream.of(values()).collect(Collectors.toMap(Gender::getCode, e -> e));

    public static Optional<Gender> ofZh_CN(String zh_CNString) {
        return Optional.ofNullable(zh_CNStringToEnum.get(zh_CNString));
    }

    public static Optional<Gender> ofCode(byte code) {
        return Optional.ofNullable(codeToEnum.get(code));
    }
}
