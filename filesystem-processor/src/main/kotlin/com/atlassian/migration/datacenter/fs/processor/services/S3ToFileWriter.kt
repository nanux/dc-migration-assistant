package com.atlassian.migration.datacenter.fs.processor.services

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.event.S3EventNotification
import lombok.SneakyThrows
import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.nio.file.StandardOpenOption

class S3ToFileWriter(private val s3Client: AmazonS3?, private val entity: S3EventNotification.S3Entity?, private val jiraHome: String?) : Runnable {

    private val log = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

    @SneakyThrows
    override fun run() {
        val key = URLDecoder.decode(entity!!.getObject().key, Charset.defaultCharset().toString())
        try {
            s3Client!!.getObject(entity.bucket.name, key).use { s3object ->
                s3object.objectContent.use { inputStream ->
                    val localPath = File("$jiraHome/$key")
                    if (!localPath.parentFile.exists()) {
                        if (localPath.parentFile.mkdirs()) {

                            log.debug("Made the directory {}", localPath.path)
                        }
                    }
                    val fileChannel = AsynchronousFileChannel.open(localPath.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                    val bytes = IOUtils.toByteArray(inputStream)
                    val buffer = ByteBuffer.wrap(bytes)
                    fileChannel.write(buffer, 0, buffer, object : CompletionHandler<Int?, ByteBuffer?> {
                        override fun completed(result: Int?, attachment: ByteBuffer?) {
                            log.debug("Wrote the file {}", localPath.toString())
                        }

                        override fun failed(exc: Throwable, attachment: ByteBuffer?) {
                            log.error(exc.cause!!.localizedMessage)
                            log.error("Failed to write the file {}", localPath.toString())
                        }
                    })
                }
            }
        } catch (ex: Exception) {
            log.error("Failed to process " + ex.localizedMessage)
            log.error(ex.cause!!.localizedMessage)
        }
    }

}