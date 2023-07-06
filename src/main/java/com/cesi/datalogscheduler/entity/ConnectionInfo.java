package com.cesi.datalogscheduler.entity;

import com.cesi.datalogscheduler.annotation.Value;
import lombok.Data;

import javax.validation.constraints.*;

/**
 * 远程连接信息实体类
 */
@Data
public class ConnectionInfo {
    /**
     * ID
     */
    private Integer id;
    /**
     * 主机地址
     */
    @NotBlank(message = "IP地址不能为空")
    @Pattern(regexp = "^((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}$|localhost", message = "错误的IP地址")
    private String host;
    /**
     * 端口号
     */
    @NotNull(message = "端口号不能为空")
    @Min(value = 0L, message = "端口号范围应为0~65535")
    @Max(value = 0xFFFFL, message = "端口号范围应为0~65535")
    private Integer port;
    /**
     * 登录名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 100, message = "用户名长度小于等于100")
    private String username;
    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码长度小于等于100")
    private String password;
    /**
     * 远程存储路径
     */
    @NotBlank(message = "远程存储路径不能为空")
    @Size(max = 200, message = "远程存储路径长度小于等于200")
    private String prefixRemotePath;
    /**
     * 本地存储路径
     */
    @NotBlank(message = "本地存储路径不能为空")
    @Size(max = 200, message = "本地存储路径长度小于等于200")
    private String prefixLocalPath;
    /**
     * 挂载存储路径
     */
    @NotBlank(message = "挂载存储路径不能为空")
    @Size(max = 200, message = "挂载存储路径长度小于等于200")
    private String prefixVolumePath;
    /**
     * 测试系统类型
     */
    @NotBlank(message = "测试系统类型不能为空")
    @Value(strings = {"ADV93000", "J750", "ULTRA_FLEX"}, message = "未知的测试系统名称")
    private String system;
    /**
     * 机台名称
     */
    @NotBlank(message = "机台名称不能为空")
    @Size(max = 50, message = "机台名称长度小于等于50")
    private String computerName;
    /**
     * 是否同步
     */
    private boolean sync;
}
