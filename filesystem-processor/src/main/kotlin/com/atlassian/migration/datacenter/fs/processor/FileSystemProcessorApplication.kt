package com.atlassian.migration.datacenter.fs.processor

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication(scanBasePackages = ["com.atlassian.migration.datacenter"])
open class FileSystemProcessorApplication : CommandLineRunner {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(FileSystemProcessorApplication::class.java, *args)
        }
    }

    override fun run(vararg args: String?) {
        TODO("Not yet implemented")
    }

}