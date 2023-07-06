package com.cesi.datalogscheduler.entity;

import lombok.Data;

/**
 * 统计数量实体类
 */
@Data
public class ResultCount {
    /**
     * 数量
     */
    private int count;
    /**
     * 测试人员姓名
     */
    private String name;
    /**
     * 芯片型号
     */
    private String model;
    /**
     * 测试机台
     */
    private String computerName;
    /**
     * 日期
     */
    private String date;
    /**
     * 年月
     */
    private String yearMonth;
    /**
     * 年周
     */
    private String yearWeek;
    /**
     * 周开始日期
     */
    private String weekBegin;
    /**
     * 周结束
     */
    private String weekEnd;
}
