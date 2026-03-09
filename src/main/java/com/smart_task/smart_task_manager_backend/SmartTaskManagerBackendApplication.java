package com.smart_task.smart_task_manager_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {"com.smart_task.smart_task_manager_backend", "com.smart_task.smart_task_manager_backend.security"})
public class SmartTaskManagerBackendApplication {

	public static void main(String[] args) {
        // Set server timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication.run(SmartTaskManagerBackendApplication.class, args);
	}

}
