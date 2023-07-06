package com.cesi.datalogscheduler.controller;

import com.cesi.datalogscheduler.component.SyncScheduler;
import com.cesi.datalogscheduler.service.SyncScheduledTaskService;
import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {
    private final SyncScheduledTaskService service;

    @GetMapping("/open")
    public ResponseWrapper open() {
        SyncScheduler.openSync();
        log.info("open sync");
        return ResponseWrapper.ok();
    }

    @GetMapping("/close")
    public ResponseWrapper close() {
        SyncScheduler.closeSync();
        log.info("close sync");
        return ResponseWrapper.ok();
    }

    @GetMapping("/status")
    public ResponseWrapper status() {
        return ResponseWrapper.ok().setData(SyncScheduler.syncStatus());
    }

    @GetMapping("/set")
    public ResponseWrapper setTime(@RequestParam(required = false) String time, @RequestParam(required = false) String cron) {
        if (cron == null && time == null) {
            return ResponseWrapper.error();
        }
        if (cron == null) {
            LocalTime localTime = LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME);
            int hour = localTime.getHour(), minute = localTime.getMinute(), second = localTime.getSecond();
            cron = String.format("%d %d %d * * *", second, minute, hour);
        }
        service.cancelSchedule();
        return ResponseWrapper.ok().setData(service.schedule(cron));
    }

    @GetMapping("/get")
    public ResponseWrapper getTime() {
        String cron = service.getCron();
        if (cron == null) {
            return ResponseWrapper.ok().setData(null);
        }
        String[] arr = cron.split(" ");
        String time = String.format("%02d:%02d:%02d", Integer.valueOf(arr[2]), Integer.valueOf(arr[1]), Integer.valueOf(arr[0]));
        log.info("当前定时[" + time + "]");
        return ResponseWrapper.ok().setData(time);
    }
}
