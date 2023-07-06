package com.cesi.datalogscheduler.controller;

import com.cesi.datalogscheduler.entity.EntityList;
import com.cesi.datalogscheduler.entity.Tester;
import com.cesi.datalogscheduler.mapper.TesterMapper;
import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/tester")
@RequiredArgsConstructor
public class TesterController {
    private final TesterMapper mapper;

    @GetMapping
    public ResponseWrapper get() {
        try {
            return ResponseWrapper.ok().setData(mapper.findAll());
        } catch (Exception e) {
            log.error("查询数据库失败", e);
            return ResponseWrapper.error().setMsg("查询数据库失败");
        }
    }

    @PostMapping
    public ResponseWrapper post(@Valid @RequestBody EntityList<Tester> testerList) {
        for (Tester tester : testerList) {
            if (tester == null ) {
                return ResponseWrapper.error().setMsg("不能为空");
            }
            if (Strings.isBlank(tester.getName())) {
                return ResponseWrapper.error().setMsg("姓名不能为空");
            }
            if (Strings.isBlank(tester.getPinyin())) {
                return ResponseWrapper.error().setMsg("拼音不能为空");
            }
            if (Strings.isBlank(tester.getAbbreviation())) {
                return ResponseWrapper.error().setMsg("简写不能为空");
            }
            if (tester.getAge() != null && tester.getAge() > 100) {
                return ResponseWrapper.error().setMsg("非法的年龄");
            }
            if (tester.getGender() != null && (tester.getGender() > 1 || tester.getGender() < 0)) {
                return ResponseWrapper.error().setMsg("非法的性别");
            }
        }
        try {
            int rows = mapper.insert(testerList);
            log.info("插入成功数量: " + rows);
            return ResponseWrapper.ok().setData(rows);
        } catch (Exception e) {
            log.error("插入数据库失败", e);
            return ResponseWrapper.error().setMsg("插入数据库失败");
        }
    }

    @PutMapping
    public ResponseWrapper put(@Valid @RequestBody EntityList<Tester> testerList) {
        for (Tester tester : testerList) {
            if (tester == null || tester.getId() == null) {
                return ResponseWrapper.error().setMsg("ID不能为空");
            }
            if (Strings.isBlank(tester.getName())) {
                return ResponseWrapper.error().setMsg("姓名不能为空");
            }
            if (tester.getName().length() > 100) {
                return ResponseWrapper.error().setMsg("姓名过长");
            }
            if (Strings.isBlank(tester.getPinyin())) {
                return ResponseWrapper.error().setMsg("拼音不能为空");
            }
            if (Strings.isBlank(tester.getAbbreviation())) {
                return ResponseWrapper.error().setMsg("简写不能为空");
            }
            if (tester.getAge() != null && tester.getAge() > 100) {
                return ResponseWrapper.error().setMsg("非法的年龄");
            }
            if (tester.getGender() != null && (tester.getGender() > 1 || tester.getGender() < 0)) {
                return ResponseWrapper.error().setMsg("非法的性别");
            }
        }
        try {
            int rows = mapper.update(testerList);
            log.info("修改成功数量: " + rows);
            return ResponseWrapper.ok().setData(rows);
        } catch (Exception e) {
            log.error("修改数据库失败", e);
            return ResponseWrapper.error().setMsg("修改数据库失败");
        }
    }

    @DeleteMapping
    public ResponseWrapper delete(@RequestBody EntityList<Tester> testerList) {
        for (Tester tester : testerList) {
            if (tester == null || tester.getId() == null) {
                return ResponseWrapper.error().setMsg("ID不能为空");
            }
        }
        try {
            int rows = mapper.delete(testerList);
            log.info("删除成功数量: " + rows);
            return ResponseWrapper.ok().setData(rows);
        } catch (Exception e) {
            log.error("删除数据库失败", e);
            return ResponseWrapper.error().setMsg("删除数据库失败");
        }
    }
}
