package com.cesi.datalogscheduler.controller;

import com.cesi.datalogscheduler.entity.ResultInfo;
import com.cesi.datalogscheduler.mapper.ResultInfoMapper;
import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/result")
@RequiredArgsConstructor
public class ResultInfoController {
    private final ResultInfoMapper resultInfoMapper;

    @GetMapping("/all")
    public ResponseWrapper getAllResultInfo(
            int page,
            int limit,
            @RequestParam(required = false) String beginDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) List<String> testerNames,
            @RequestParam(required = false) List<String> models,
            @RequestParam(required = false) String chipId) {
        int offset = (page - 1) * limit;
        int count = resultInfoMapper.countAll(beginDate, endDate, testerNames, models, chipId);
        List<ResultInfo> list = resultInfoMapper.findAll(limit, offset, beginDate, endDate, testerNames, models, chipId);
        return ResponseWrapper.ok().setData(list).put("count", count);
    }

    @GetMapping("/models")
    public ResponseWrapper getAllModels() {
        List<String> list = resultInfoMapper.getAllModels();
        return ResponseWrapper.ok().setData(list);
    }
}
