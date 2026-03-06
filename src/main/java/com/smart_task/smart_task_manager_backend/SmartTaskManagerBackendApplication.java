package com.smart_task.smart_task_manager_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.smart_task.smart_task_manager_backend", "com.smart_task.smart_task_manager_backend.security"})
public class SmartTaskManagerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartTaskManagerBackendApplication.class, args);
	}

}
