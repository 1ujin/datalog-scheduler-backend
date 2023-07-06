package com.cesi.datalogscheduler.enums;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 老炼阶段枚举类
 */
public enum AgingPhase {
    /**
     * 老炼前
     */
    LAOLIANQIAN,
    /**
     * 老炼后
     */
    LAOLIANHOU,
    /**
     * 鉴定
     */
    JD;

    private static final Map<String, AgingPhase> stringToEnum = Stream.of(values()).collect(Collectors.toMap(AgingPhase::toString, e -> e));

    public static Optional<AgingPhase> ofName(String name) {
        return Optional.ofNullable(stringToEnum.get(name.toUpperCase()));
    }
}
