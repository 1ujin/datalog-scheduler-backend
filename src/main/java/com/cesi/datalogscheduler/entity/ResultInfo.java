package com.cesi.datalogscheduler.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 结果信息实体类
 */
@Data
public class ResultInfo {
    /**
     * 型号
     */
    private String model;
    /**
     * 批次
     */
    private String batch;
    /**
     * 老炼阶段
     */
    private String agingPhase;
    /**
     * 老炼结束时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyyMMdd")
    @JsonSerialize(using = LocalDateSerializer.class)       // 序列化
    @JsonDeserialize(using = LocalDateDeserializer.class)   // 反序列化
    private LocalDate agingEndTime;
    /**
     * 测试开始时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyyMMdd")
    @JsonSerialize(using = LocalDateSerializer.class)       // 序列化
    @JsonDeserialize(using = LocalDateDeserializer.class)   // 反序列化
    private LocalDate testBeginTime;
    /**
     * 温度
     */
    private Integer temperature;
    /**
     * 鉴定分组名称
     */
    private String jdGroup;
    /**
     * 测试人员姓名简写
     */
    private String testerNameAbbr;
    /**
     * 测试人员姓名
     */
    private String testerName;
    /**
     * 芯片编号
     */
    private String chipId;
    /**
     * 文件路径
     */
    private String path;
    /**
     * 重复文件路径
     */
    private String duplicatePath;
    /**
     * 文件名通过情况
     */
    private String surfaceResult;
    /**
     * 实际通过情况
     */
    private String realResult;
    /**
     * 机台编号
     */
    private String computerName;
    /**
     * 测试系统
     */
    private String system;
    /**
     * 结果文件类型
     */
    private String resultType;
    /**
     * 内容错误
     */
    private boolean error;
    /**
     * 文件大小
     */
    private Long filesize;
    /**
     * 测试项
     */
    private Set<String> testSuite;
    /**
     * 测试项数量
     */
    private int testSuiteCount;
    /**
     * 创建记录时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)       // 序列化
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)   // 反序列化
    private LocalDateTime createTime;

    public String getTestSuite() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(testSuite);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setTestSuite(String testSuite) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            this.testSuite = mapper.readValue(testSuite, new TypeReference<HashSet<String>>() {});
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void setTestSuite(Set<String> testSuite) {
        this.testSuite = testSuite;
    }

    public Integer getTestSuiteCount() {
        if (testSuite == null) {
            return null;
        }
        return testSuite.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResultInfo that = (ResultInfo) o;

        if (!Objects.equals(model, that.model)) return false;
        if (!Objects.equals(batch, that.batch)) return false;
        if (!Objects.equals(agingPhase, that.agingPhase)) return false;
        if (!Objects.equals(temperature, that.temperature)) return false;
        if (!Objects.equals(jdGroup, that.jdGroup)) return false;
        if (!Objects.equals(chipId, that.chipId)) return false;
        return Objects.equals(resultType, that.resultType);
    }

    @Override
    public int hashCode() {
        int result = model.hashCode();
        result = 0x1F * result + batch.hashCode();
        result = 0x1F * result + agingPhase.hashCode();
        result = 0x1F * result + (temperature != null ? temperature.hashCode() : 0);
        result = 0x1F * result + (jdGroup != null ? jdGroup.hashCode() : 0);
        result = 0x1F * result + chipId.hashCode();
        result = 0x1F * result + resultType.hashCode();
        return result;
    }
}
