package com.atlassian.migration.datacenter.fs.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.atlassian.migration.datacenter")
public class FilesystemProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FilesystemProcessorApplication.class, args);
    }

}
