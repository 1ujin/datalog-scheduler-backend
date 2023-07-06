package com.cesi.datalogscheduler.controller;

import com.cesi.datalogscheduler.entity.BackupInfo;
import com.cesi.datalogscheduler.entity.EntityList;
import com.cesi.datalogscheduler.mapper.BackupInfoMapper;
import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/backup-info")
@RequiredArgsConstructor
public class BackupInfoController {
    private final BackupInfoMapper mapper;

    @GetMapping
    public ResponseWrapper get() {
        return ResponseWrapper.ok().setData(mapper.findAll());
    }

    @PostMapping
    public ResponseWrapper post(@Valid @RequestBody EntityList<BackupInfo> list) {
        for (BackupInfo backup : list) {
            if (backup == null) {
                return ResponseWrapper.error().setMsg("ID不能为空");
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
    public ResponseWrapper put(@Valid @RequestBody EntityList<BackupInfo> list) {
        for (BackupInfo backup : list) {
            if (backup == null || backup.getId() == null) {
                return ResponseWrapper.error().setMsg("ID不能为空");
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
    public ResponseWrapper delete(@RequestBody EntityList<BackupInfo> list) {
        for (BackupInfo backup : list) {
            if (backup == null || backup.getId() == null) {
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
