package com.atlassian.migration.datacenter.fs.processor;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.atlassian.migration.datacenter.fs.processor.services.SQSMessageProcessor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.context.config.annotation.EnableStackConfiguration;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.core.region.RegionProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest()
@ActiveProfiles("production,test")
@Slf4j
@EnableStackConfiguration(stackName = "migration-helper")
class FilesystemProcessorApplicationTests {

    @Autowired
    private AmazonSQSAsync sqsClient;

    @Autowired
    private AmazonS3 s3Client;

    @Autowired
    private MessageChannel outboundChannel;

    @Autowired
    private RegionProvider regionProvider;

    @Autowired
    private ResourceIdResolver idResolver;

    @Value("${app.jira.file.path}")
    private String jiraFilePath;

    @Autowired
    private SubscribableChannel inboundChannel;

    @Autowired
    private SQSMessageProcessor sqsMessageProcessor;

    public static BasicFileAttributes awaitFile(final Path target, long timeout) throws IOException, InterruptedException {
        final Path name = target.getFileName();
        final Path targetDir = target.getParent();

        // If path already exists, return early
        try {
            return Files.readAttributes(target, BasicFileAttributes.class);
        } catch (NoSuchFileException ignored) {
        }

        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            final WatchKey watchKey = targetDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
            // The file could have been created in the window between Files.readAttributes and Path.register
            try {
                return Files.readAttributes(target, BasicFileAttributes.class);
            } catch (NoSuchFileException ex) {
            }
            // The file is absent: watch events in parent directory
            WatchKey watchKey1 = null;
            boolean valid = true;
            do {
                long t0 = System.currentTimeMillis();
                watchKey1 = watchService.poll(timeout, TimeUnit.MILLISECONDS);
                if (watchKey1 == null) {
                    return null; // timed out
                }
                // Examine events associated with key
                for (WatchEvent<?> event : watchKey1.pollEvents()) {
                    Path path1 = (Path) event.context();
                    if (path1.getFileName().equals(name)) {
                        return Files.readAttributes(target, BasicFileAttributes.class);
                    }
                }
                // Did not receive an interesting event; re-register key to queue
                long elapsed = System.currentTimeMillis() - t0;
                timeout = elapsed < timeout ? (timeout - elapsed) : 0L;
                valid = watchKey1.reset();
            } while (valid);
        }

        return null;
    }

    @SneakyThrows
    private S3EventNotification createNotification() {

        String payload = RandomStringUtils.randomAlphabetic(75);
        this.s3Client.putObject(this.idResolver.resolveToPhysicalResourceId("MigrationBucket"), "bootstrap.properties", payload);

        S3EventNotification.S3ObjectEntity objEntity = new S3EventNotification.S3ObjectEntity("bootstrap.properties", 1L, null, null);
        S3EventNotification.S3BucketEntity bucketEntity = new S3EventNotification.S3BucketEntity(this.idResolver.resolveToPhysicalResourceId("MigrationBucket"), null, null);
        S3EventNotification.S3Entity entity = new S3EventNotification.S3Entity(null, bucketEntity, objEntity, null);

        S3EventNotification.S3EventNotificationRecord rec = new S3EventNotification.S3EventNotificationRecord(regionProvider.toString(), "s3:ObjectCreated:Put", null,
                "1970-01-01T00:00:00.000Z", null, null, null, entity, null);

        List<S3EventNotification.S3EventNotificationRecord> notifications = new ArrayList<>(2);
        notifications.add(rec);

        return new S3EventNotification(notifications);
    }

    @BeforeEach
    public void setup() {
        String queueURL;
        final String queueName = "MigrationQueue";
        queueURL = idResolver.resolveToPhysicalResourceId(queueName);
        if (queueURL.equals(queueName)) {
            CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName("atl-migration-queue-migration-helper.fifo");
            this.sqsClient.createQueue(createQueueRequest).getQueueUrl();
        }

    }

    @SneakyThrows
    @Test
    public void testQueueRead() {
        S3EventNotification eventNotification = createNotification();
        Message<S3EventNotification> message = MessageBuilder.withPayload(eventNotification)
                .setHeader("aws_queue", "MigrationQueue")
                .build();
        this.outboundChannel.send(message);
        /*
        File s3Dir = new File(jiraFilePath);
        assertTrue(s3Dir.mkdir(), "Directory creation failed!");

        File s3File = new File(jiraFilePath.concat("/bootstrap.properties"));
        BasicFileAttributes attributes = awaitFile(Paths.get(s3File.getAbsolutePath()),30000);
        assertNotNull(attributes, "File not created by consumer service.");
        assertTrue(attributes.isRegularFile());
        */
    }

    @SneakyThrows
    @Test
    public void testQueueSend() {
        final S3EventNotification eventNotification = createNotification();
        Message<S3EventNotification> message = MessageBuilder.withPayload(eventNotification)
                .setHeader("aws_queue", "MigrationQueue")
                .build();
        assertTrue(this.outboundChannel.send(message), "Message not sent to channel");
    }

    @Disabled
    @Ignore
    @Test
    void testReceiveMessage() {
        try {
            S3EventNotification eventNotification = createNotification();
            this.inboundChannel.send(MessageBuilder.withPayload(eventNotification).build());
        } catch (Exception ex) {
            fail(ex.getLocalizedMessage());
        }
    }

}
