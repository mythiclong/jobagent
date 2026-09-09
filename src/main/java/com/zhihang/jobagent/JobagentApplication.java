package com.zhihang.jobagent;

import com.zhihang.jobagent.support.H2DatabaseRecoverySupport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobagentApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(JobagentApplication.class, args);
        } catch (Throwable startupFailure) {
            if (H2DatabaseRecoverySupport.recoverIfCorrupted(startupFailure, args)) {
                SpringApplication.run(JobagentApplication.class, args);
                return;
            }
            throw startupFailure;
        }
    }

}
