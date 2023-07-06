package com.cesi.datalogscheduler.controller;

import com.cesi.datalogscheduler.entity.ConnectionInfo;
import com.cesi.datalogscheduler.entity.EntityList;
import com.cesi.datalogscheduler.mapper.ConnectionInfoMapper;
import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.Inet4Address;
import java.net.UnknownHostException;

@Slf4j
@RestController
@RequestMapping("/connection")
@RequiredArgsConstructor
public class ConnectionInfoController {
    private final ConnectionInfoMapper mapper;

    @GetMapping
    public ResponseWrapper get() {
        return ResponseWrapper.ok().setData(mapper.findAll());
    }

    @PostMapping
    public ResponseWrapper post(@Valid @RequestBody EntityList<ConnectionInfo> list) {
        for (ConnectionInfo connection : list) {
            if (connection == null) {
                return ResponseWrapper.error().setMsg("ID不能为空");
            }
            try {
                if (connection.getHost() != null && !connection.getHost().equals(Inet4Address.getByName(connection.getHost()).getHostAddress())) {
                    return ResponseWrapper.error().setMsg("非法的IP地址");
                }
            } catch (UnknownHostException e) {
                log.error("IP解析错误", e);
                return ResponseWrapper.error().setMsg("非法的IP地址");
            }
            if (connection.getPort() == null || connection.getPort() < 1 || connection.getPort() > 0xFFFF) {
                return ResponseWrapper.error().setMsg("非法的端口号");
            }
            if (Strings.isBlank(connection.getUsername())) {
                return ResponseWrapper.error().setMsg("用户名不能为空");
            }
            if (Strings.isBlank(connection.getPassword())) {
                return ResponseWrapper.error().setMsg("密码不能为空");
            }
            if (Strings.isBlank(connection.getPrefixRemotePath())) {
                return ResponseWrapper.error().setMsg("目标主机路径前缀不能为空");
            }
            if (Strings.isBlank(connection.getPrefixLocalPath())) {
                return ResponseWrapper.error().setMsg("本地路径前缀不能为空");
            }
            if (Strings.isBlank(connection.getPrefixVolumePath())) {
                return ResponseWrapper.error().setMsg("挂载路径前缀不能为空");
            }
            if (Strings.isBlank(connection.getSystem())) {
                return ResponseWrapper.error().setMsg("测试系统类型不能为空");
            }
            if (Strings.isBlank(connection.getComputerName())) {
                return ResponseWrapper.error().setMsg("机台名不能为空");
            }
        }
        try {
            int rows = mapper.insert(list);
            log.info("插入成功数量: " + rows);
            return ResponseWrapper.ok().setData(rows);
        } catch (Exception e) {
            log.error("插入数据库失败", e);
            return ResponseWrapper.error().setMsg("插入数据库失败");
        }
    }

    @PutMapping
    public ResponseWrapper put(@Valid @RequestBody EntityList<ConnectionInfo> list) {
        for (ConnectionInfo connection : list) {
            if (connection == null || connection.getId() == null) {
                return ResponseWrapper.error().setMsg("ID不能为空");
            }
            try {
                if (connection.getHost() != null && !connection.getHost().equals(Inet4Address.getByName(connection.getHost()).getHostAddress())) {
                    return ResponseWrapper.error().setMsg("非法的IP地址");
                }
            } catch (UnknownHostException e) {
                log.error("IP解析错误", e);
                return ResponseWrapper.error().setMsg("非法的IP地址");
            }
            if (connection.getPort() == null || connection.getPort() < 1 || connection.getPort() > 0xFFFF) {
                return ResponseWrapper.error().setMsg("非法的端口号");
            }
            if (Strings.isBlank(connection.getUsername())) {
                return ResponseWrapper.error().setMsg("用户名不能为空");
            }
            if (Strings.isBlank(connection.getPassword())) {
                return ResponseWrapper.error().setMsg("密码不能为空");
            }
            if (Strings.isBlank(connection.getPrefixRemotePath())) {
                return ResponseWrapper.error().setMsg("目标主机路径前缀不能为空");
            }
            if (Strings.isBlank(connection.getPrefixLocalPath())) {
                return ResponseWrapper.error().setMsg("本地路径前缀不能为空");
            }
            if (Strings.isBlank(connection.getPrefixVolumePath())) {
                return ResponseWrapper.error().setMsg("挂载路径前缀不能为空");
            }
            if (Strings.isBlank(connection.getSystem())) {
                return ResponseWrapper.error().setMsg("测试系统类型不能为空");
            }
            if (Strings.isBlank(connection.getComputerName())) {
                return ResponseWrapper.error().setMsg("机台名不能为空");
            }
        }
        try {
            int rows = mapper.update(list);
            log.info("修改成功数量: " + rows);
            return ResponseWrapper.ok().setData(rows);
        } catch (Exception e) {
            log.error("修改数据库失败", e);
            return ResponseWrapper.error().setMsg("修改数据库失败");
        }
    }

    @DeleteMapping
    public ResponseWrapper delete(@RequestBody EntityList<ConnectionInfo> list) {
        for (ConnectionInfo connection : list) {
            if (connection == null || connection.getId() == null) {
                return ResponseWrapper.error().setMsg("ID不能为空");
            }
        }
        try {
            int rows = mapper.delete(list);
            log.info("删除成功数量: " + rows);
            return ResponseWrapper.ok().setData(rows);
        } catch (Exception e) {
            log.error("删除数据库失败", e);
            return ResponseWrapper.error().setMsg("删除数据库失败");
        }
    }
}
