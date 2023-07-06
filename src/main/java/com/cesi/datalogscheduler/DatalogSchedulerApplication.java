package com.cesi.datalogscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DatalogSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatalogSchedulerApplication.class, args);
    }

}
