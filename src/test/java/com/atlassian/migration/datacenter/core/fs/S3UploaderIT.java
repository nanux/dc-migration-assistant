package com.atlassian.migration.datacenter.core.fs;

import cloud.localstack.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.core.fs.reporting.DefaultFilesystemMigrationProgress;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationErrorReport;
import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationProgress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Tag("integration")
@ExtendWith({LocalstackDockerExtension.class, MockitoExtension.class})
@LocalstackDockerProperties(services = {"s3"}, imageTag = "0.10.8")
class S3UploaderIT {
    private static final String LOCALSTACK_S3_ENDPOINT = "http://localhost:4572";
    private static final String TREBUCHET_LOCALSTACK_BUCKET = "trebuchet-localstack-bucket";
    private BlockingQueue<Optional<Path>> queue;
    private S3Uploader uploader;
    private AtomicBoolean isCrawlDone;
    private FileSystemMigrationErrorReport errorReport;
    private FileSystemMigrationProgress progress;

    @Mock
    private AwsCredentialsProvider mockCredentialsProvider;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setup() {
        S3AsyncClient localStackS3Client = S3AsyncClient.builder()
                .credentialsProvider(mockCredentialsProvider)
                .endpointOverride(URI.create(LOCALSTACK_S3_ENDPOINT))
                .region(Region.US_EAST_1)
                .build();
        S3UploadConfig config = new S3UploadConfig(TREBUCHET_LOCALSTACK_BUCKET, localStackS3Client, tempDir);

        when(mockCredentialsProvider.resolveCredentials()).thenReturn(new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return "fake-access-key";
            }

            @Override
            public String secretAccessKey() {
                return "fake-secret-key";
            }
        });

        errorReport = new DefaultFileSystemMigrationErrorReport();
        progress = new DefaultFilesystemMigrationProgress();
        isCrawlDone = new AtomicBoolean(false);
        queue = new LinkedBlockingQueue<>();
        uploader = new S3Uploader(config, errorReport, progress);
    }

    @Test
    void uploadShouldUploadPathsFromQueueToS3() throws IOException, InterruptedException {
        final Path file = addFileToQueue("file");
        queue.put(Optional.empty());

        AmazonS3 s3Client = TestUtils.getClientS3();
        s3Client.createBucket(TREBUCHET_LOCALSTACK_BUCKET);

        uploader.upload(queue);

        assertTrue(queue.isEmpty());

        final List<S3ObjectSummary> objectSummaries = s3Client.listObjects(TREBUCHET_LOCALSTACK_BUCKET).getObjectSummaries();

        assertEquals(
                errorReport.getFailedFiles().size(),
                0,
                String.format(
                        "expected no upload errors but found %s",
                        errorReport.getFailedFiles()
                                .stream()
                                .reduce("",
                                        (acc, failedMigration) -> String.format("%s%s: %s\n", acc, failedMigration.getFilePath().toString(),
                                                failedMigration.getReason()), (acc, partial) -> acc + "\n" + partial)));
        assertEquals(objectSummaries.size(), 1);
        assertEquals(objectSummaries.get(0).getKey(), tempDir.relativize(file).toString());
        assertEquals(1, progress.getCountOfMigratedFiles());
    }

    Path addFileToQueue(String fileName) throws IOException {
        final Path file = tempDir.resolve(fileName);
        Files.write(file, "".getBytes());
        queue.add(Optional.of(file));
        return file;
    }
}
