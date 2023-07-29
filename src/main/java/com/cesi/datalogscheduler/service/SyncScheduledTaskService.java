package com.cesi.datalogscheduler.service;

import com.cesi.datalogscheduler.component.SyncScheduler;
import com.cesi.datalogscheduler.util.DynamicScheduledTaskRegistrar;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Slf4j
@Service
@Scope("prototype")
@RequiredArgsConstructor
public class SyncScheduledTaskService {
    private final DynamicScheduledTaskRegistrar registrar = new DynamicScheduledTaskRegistrar();

    private final SyncScheduler scheduler;

    private static final String NAME = "SYNC";

    @Value("${schedule.default-cron}")
    private String defaultCron;

    private Runnable syncRunnable;

    public Boolean schedule(String cron) {
        if (syncRunnable == null) {
            syncRunnable = scheduler::scheduledSync; // eta-conversion: syncRunnable = () -> { scheduler.sync(); };
        }
        Boolean result = registrar.addCronTask(NAME, cron, syncRunnable);
        log.info("定时任务[" + NAME + "]添加" + (result ? "成功" : "失败"));
        return result;
    }

    public void cancelSchedule() {
        ScheduledTask task = registrar.getScheduledTask(NAME);
        if (task == null) {
            return;
        }
        syncRunnable = task.getTask().getRunnable();
        registrar.cancelCronTask(NAME);
        log.info("定时任务[" + NAME + "]取消");
    }

    public String getCron() {
        CronTask cronTask = registrar.getCronTask(NAME);
        if (cronTask == null) {
            return null;
        }
        return cronTask.getExpression();
    }

    @PostConstruct
    private void initDefaultTask() {
        log.info("默认定时: " + defaultCron);
        schedule(defaultCron);
    }

    /**
     * 获取当前定时任务中正在执行的线程数量
     *
     * @return 线程数量
     */
    public Integer activeCount() {
        ThreadPoolTaskScheduler taskScheduler = (ThreadPoolTaskScheduler) registrar.getScheduler();
        if (taskScheduler == null) {
            return null;
        }
        return taskScheduler.getActiveCount();
    }
}
