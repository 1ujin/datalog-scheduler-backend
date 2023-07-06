package com.cesi.datalogscheduler.controller;

import com.cesi.datalogscheduler.component.Backupper;
import com.cesi.datalogscheduler.util.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/backup")
public class BackupController {

    @GetMapping("/open")
    public ResponseWrapper open() {
        Backupper.openBackup();
        log.info("open sync");
        return ResponseWrapper.ok();
    }

    @GetMapping("/close")
    public ResponseWrapper close() {
        Backupper.closeBackup();
        log.info("close sync");
        return ResponseWrapper.ok();
    }

    @GetMapping("/status")
    public ResponseWrapper status() {
        return ResponseWrapper.ok().setData(Backupper.backupStatus());
    }
}
