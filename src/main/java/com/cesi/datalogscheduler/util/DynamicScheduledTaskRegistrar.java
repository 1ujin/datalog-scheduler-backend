package com.cesi.datalogscheduler.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public class DynamicScheduledTaskRegistrar extends ScheduledTaskRegistrar {
    private final Map<String, ScheduledTask> scheduledTaskMap = new LinkedHashMap<>(16);

    private final Map<String, CronTask> cronTaskMap = new LinkedHashMap<>(16);

    public DynamicScheduledTaskRegistrar() {
        super();
        // 线程池任务调度类
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        // 设置线程数
        taskScheduler.setPoolSize(16);
        // 任务取消后立即从队列中移除
        taskScheduler.setRemoveOnCancelPolicy(true);
        // 等待所有任务都完成后再继续销毁其他的 Bean
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        // 超过这个时间还没有销毁就强制销毁，确保应用最后能够被关闭，而不是阻塞住
        // taskScheduler.setAwaitTerminationSeconds(60);
        // 设置线程名
        taskScheduler.setThreadNamePrefix("dynamic-scheduled-task-");
        // 初始化线程池
        taskScheduler.initialize();
        // 注册线程池任务调度类
        this.setScheduler(taskScheduler);
    }

    public Boolean addCronTask(String taskName, String cron, Runnable runnable) {
        if (scheduledTaskMap.containsKey(taskName)) {
            log.error("定时任务[" + taskName + "]已存在, 添加失败");
            return Boolean.FALSE;
        }
        CronTask cronTask = new CronTask(runnable, cron);
        if (taskName == null) {
            taskName = cronTask.toString();
        }
        ScheduledTask scheduledTask = this.scheduleCronTask(cronTask);
        cronTaskMap.put(taskName, cronTask);
        scheduledTaskMap.put(taskName, scheduledTask);
        log.info("定时任务[" + taskName + "]新增成功");
        return Boolean.TRUE;
    }

    public void cancelCronTask(String taskName) {
        ScheduledTask scheduledTask = scheduledTaskMap.get(taskName);
        if (null != scheduledTask) {
            scheduledTask.cancel();
            scheduledTaskMap.remove(taskName);
            cronTaskMap.remove(taskName);
            log.info("定时任务[" + taskName + "]删除成功");
        } else {
            log.info("定时任务[" + taskName + "]不存在");
        }
    }

    public ScheduledTask getScheduledTask(String taskName) {
        return scheduledTaskMap.get(taskName);
    }

    public CronTask getCronTask(String taskName) {
        return cronTaskMap.get(taskName);
    }

    @Override
    public void destroy() {
        super.destroy();
        scheduledTaskMap.values().forEach(ScheduledTask::cancel);
    }
}
