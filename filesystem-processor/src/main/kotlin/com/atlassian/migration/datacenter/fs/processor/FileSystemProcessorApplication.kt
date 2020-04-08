package com.atlassian.migration.datacenter.fs.processor

import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.atlassian.migration.datacenter"])
open class FileSystemProcessorApplication {

    fun main(args: Array<String>) {
        runApplication<FileSystemProcessorApplication>(*args) {
            webApplicationType = WebApplicationType.NONE
        }
    }


}