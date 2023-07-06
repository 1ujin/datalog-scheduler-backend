package com.cesi.datalogscheduler.controller;

import com.cesi.datalogscheduler.entity.ResultCount;
import com.cesi.datalogscheduler.mapper.ResultInfoMapper;
import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/count")
@RequiredArgsConstructor
public class CountController {
    private final ResultInfoMapper resultInfoMapper;

    @GetMapping("/{period}")
    public ResponseWrapper getResultCount(
            @PathVariable String period,
            @RequestParam(required = false) String beginDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<String> testerNames,
            @RequestParam(required = false) String computerName) {
        if ("day".equals(period)) {
            LocalDate beginLocalDate, endLocalDate;
            if (beginDate == null) {
                beginLocalDate = LocalDate.now();
            } else {
                beginLocalDate = LocalDate.parse(beginDate, DateTimeFormatter.BASIC_ISO_DATE);
            }
            if (endDate == null) {
                endLocalDate = LocalDate.now();
            } else {
                endLocalDate = LocalDate.parse(endDate, DateTimeFormatter.BASIC_ISO_DATE);
            }
            if (beginLocalDate.plusYears(1L).isBefore(endLocalDate)) {
                return ResponseWrapper.error().put("msg", "数据量过大");
            }
        }
        List<ResultCount> list;
        switch (period) {
            case "day":
                list = resultInfoMapper.countByDay(testerNames, computerName, beginDate, endDate);
                break;
            case "week":
                list = resultInfoMapper.countByWeek(testerNames, computerName, beginDate, endDate);
                break;
            case "month":
                list = resultInfoMapper.countByMonth(testerNames, computerName, beginDate, endDate);
                break;
            default:
                list = Collections.emptyList();
                break;
        }
        Map<String, List<Integer>> map = new HashMap<>();
        Set<String> xAxis = new LinkedHashSet<>();
        for (ResultCount resultCount : list) {
            String name = resultCount.getName();
            if (name == null) {
                continue;
            }
            if (!map.containsKey(name)) {
                map.put(name, new ArrayList<>());
            }
            map.get(name).add(resultCount.getCount());
            if (map.get(name).size() > 366) {
                return ResponseWrapper.error().put("msg", "数据量过大");
            }
            switch (period) {
                case "day":
                    xAxis.add(Optional.ofNullable(resultCount.getDate()).orElse("?"));
                    break;
                case "week":
                    xAxis.add(Optional.ofNullable(resultCount.getWeekBegin()).orElse("?") + '-' + Optional.ofNullable(resultCount.getWeekEnd()).orElse("?"));
                    break;
                case "month":
                    xAxis.add(Optional.ofNullable(resultCount.getYearMonth()).orElse("?"));
                    break;
                default:
                    xAxis.add("?");
                    break;
            }
        }
        ResponseWrapper model = ResponseWrapper.ok();
        model.put("counts", map);
        model.put("xAxis", xAxis);
        return model;
    }

    @GetMapping("/error")
    public ResponseWrapper getErrorCount(
            @RequestParam(required = false) String beginDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<String> testerNames,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String computerName,
            @RequestParam(required = false, defaultValue = "false") boolean needReport) {
        // 所有芯片型号
        List<String> models;
        if (Strings.isBlank(model)) {
            models = resultInfoMapper.getAllModels();
        } else {
            models = Collections.singletonList(model);
        }
        Map<String, List<Integer>> map = new LinkedHashMap<>();
        for (String m : models) {
            // 文件名重复
            List<ResultCount> duplicateFilenameList = resultInfoMapper.duplicateFilenameCount(testerNames, m, computerName, beginDate, endDate);
            fillErrorMap(map, duplicateFilenameList, 0);
            // 文件内容错误
            List<ResultCount> errorContentList = resultInfoMapper.errorContentCount(testerNames, m, computerName, beginDate, endDate);
            fillErrorMap(map, errorContentList, 1);
            // Passed存成Failed
            List<ResultCount> p2fList = resultInfoMapper.passedToFailedCount(testerNames, m, computerName, beginDate, endDate);
            fillErrorMap(map, p2fList, 2);
            // Failed存成Passed
            List<ResultCount> f2pList = resultInfoMapper.failedToPassedCount(testerNames, m, computerName, beginDate, endDate);
            fillErrorMap(map, f2pList, 3);
            // 文件大小错误
            List<ResultCount> filesizeErrorList = resultInfoMapper.filesizeErrorCount(testerNames, m, computerName, beginDate, endDate);
            fillErrorMap(map, filesizeErrorList, 4);
            // 测试项错误
            List<ResultCount> testSuiteErrorList = resultInfoMapper.testSuiteErrorCount(testerNames, m, computerName, beginDate, endDate);
            fillErrorMap(map, testSuiteErrorList, 5);
            if (needReport) {
                // 过程缺少报告
                List<ResultCount> missReportList = resultInfoMapper.missReportCount(testerNames, m, computerName, beginDate, endDate);
                fillErrorMap(map, missReportList, 6);
                // 报告缺少过程
                List<ResultCount> missProcessList = resultInfoMapper.missProcessCount(testerNames, m, computerName, beginDate, endDate);
                fillErrorMap(map, missProcessList, 7);
            }
        }
        return ResponseWrapper.ok().setData(map);
    }

    private void fillErrorMap(Map<String, List<Integer>> map, List<ResultCount> list, int index) {
        for (ResultCount resultCount : list) {
            String name = resultCount.getName();
            int count = resultCount.getCount();
            if (!map.containsKey(name)) {
                map.put(name, new ArrayList<>());
            }
            List<Integer> value = map.get(name);
            if (index >= value.size()) {
                for (int i = value.size(); i < index; i++) {
                    value.add(0);
                }
                value.add(count);
            } else {
                value.set(index, value.get(index) + count);
            }
        }
    }
}
