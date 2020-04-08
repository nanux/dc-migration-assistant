package com.atlassian.migration.datacenter.fs.processor.services

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.event.S3EventNotification
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.io.File
import java.util.function.Consumer

@Component
class SQSMessageProcessor(private val s3Client: AmazonS3?, private val threadPoolTaskExecutor: ThreadPoolTaskExecutor?, @Value("\${app.jira.file.path}") private val jiraHome: String) : MessageHandler {

    private val log = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

    override fun handleMessage(message: Message<*>) {
        val s3EventNotificationRecord: S3EventNotification = message.payload as S3EventNotification
        log.debug("Received SQS message {}", s3EventNotificationRecord.toJson())
        val s3EventNotificationRecords = s3EventNotificationRecord.records
        log.debug("Received " + s3EventNotificationRecords.size.toString() + " records from S3.")
        val jiraHomePath = File(this.jiraHome)
        if (!jiraHomePath.exists()) {
            if (jiraHomePath.mkdir()) {
                log.debug("Created Jira Home path " + jiraHomePath.absolutePath)
            }
        }
        if (s3EventNotificationRecords.size == 1) {
            submitTask(s3Client, s3EventNotificationRecords[0].s3, jiraHome)
        } else if (s3EventNotificationRecords.size > 1) {
            s3EventNotificationRecords.forEach(Consumer { record: S3EventNotification.S3EventNotificationRecord -> submitTask(s3Client, record.s3, jiraHome) })
        }
    }

    private fun submitTask(s3Client: AmazonS3?, item: S3EventNotification.S3Entity, jiraHome: String?) {
        val fileWriter = S3ToFileWriter(s3Client, item, jiraHome)
        threadPoolTaskExecutor!!.submit(fileWriter)
    }


}