package com.cesi.datalogscheduler.enums;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件结果类型枚举类
 */
public enum ResultEnum {
    /**
     * 通过
     */
    PASSED(""),
    /**
     * 未通过
     */
    FAILED("_failed"),
    /**
     * 过程文件
     */
    PROCESS(""),
    /**
     * 报告文件
     */
    REPORT("_x");

    private final String suffix;

    ResultEnum(String suffix) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return suffix;
    }

    private static final Map<String, ResultEnum> stringToEnum = Stream.of(values()).collect(Collectors.toMap(ResultEnum::toString, e -> e));

    public static Optional<ResultEnum> ofName(String name) {
        return Optional.ofNullable(stringToEnum.get(name.toUpperCase()));
    }
}
