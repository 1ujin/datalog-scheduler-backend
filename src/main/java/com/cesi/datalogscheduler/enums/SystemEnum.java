package com.cesi.datalogscheduler.enums;

/**
 * 测试系统枚举类
 */
public enum SystemEnum {
    J750,
    ULTRA_FLEX,
    ADV93000,
    LINUX,
    WINDOWS;

    public static SystemEnum ofName(String name) {
        if (name != null && !name.equals("")) {
            for (SystemEnum value : SystemEnum.values()) {
                if (value.name().equals(name.toUpperCase())) {
                    return value;
                }
            }
        }
        return null;
    }
}
