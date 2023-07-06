package com.cesi.datalogscheduler.entity;

import com.cesi.datalogscheduler.annotation.Value;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 测试人员
 */
@Data
public class Tester {
    /**
     * ID
     */
    private Integer id;
    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 20, message = "姓名长度大于等于2且小于等于20")
    private String name;
    /**
     * 全拼
     */
    @NotBlank(message = "拼音不能为空")
    @Size(min = 2, max = 100, message = "拼音长度大于等于2且小于等于100")
    private String pinyin;
    /**
     * 简写
     */
    @NotBlank(message = "简写不能为空")
    @Size(min = 2, max = 10, message = "简写长度大于等于2小于等于10")
    private String abbreviation;
    /**
     * 性别
     */
    @Value(numbers = {0, 1}, message = "性别值必须为0或1")
    private Byte gender;
    /**
     * 年龄
     */
    @Min(value = 18L, message = "年龄不得小于18")
    @Max(value = 65L, message = "年龄不得大于65")
    private Byte age;
}
