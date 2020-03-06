package com.atlassian.migration.datacenter.core.database;

import com.atlassian.migration.datacenter.core.database.datasource.LiquibaseDatasource;
import com.atlassian.migration.datacenter.core.database.enums.BackupFormat;
import com.atlassian.migration.datacenter.core.database.enums.SnapshotType;
import com.atlassian.migration.datacenter.core.database.properties.LiquibaseBackupProperties;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.StandardObjectChangeFilter;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class S3DatabaseBackupService implements DatabaseBackupService {

    private static final String CHANGELOG_FILE_NAME_TEMPLATE = "backup_%s.%s%s";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String ACCELLERATION_SUFFIX = ".s3-accelerate.dualstack.amazonaws.com";
    private final LiquibaseDatasource dataSource;
    private final LiquibaseBackupProperties properties;
    private final S3Client s3Client;

    @Inject
    public S3DatabaseBackupService(LiquibaseDatasource dataSource, LiquibaseBackupProperties properties, S3Client s3Client) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.s3Client = s3Client;
    }

    @Override
    public boolean dumpDatabaseToFile() {
        try {
            this.backup();
            return true;
        } catch (Exception ex) {
            log.error(ex.getLocalizedMessage());
            return false;
        }
    }

    public void backup() {
        log.info("Backing up data...");
        try (Connection connection = dataSource.getConnection()) {
            doBackup(connection, SnapshotType.DATA, this.properties.getAuthor(), makeDiffOutputControl());
        } catch (Exception e) {
            throw new RuntimeException("Error backing up data", e);
        }
        log.info("Backup completed");
    }

    @SneakyThrows
    private void doBackup(Connection connection, String snapshotTypes, String author, DiffOutputControl diffOutputControl) {
        String fileName = generateChangeLog(connection, snapshotTypes, author, diffOutputControl);
        File dumpFile = new File(fileName);

        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);

        CreateMultipartUploadRequest uploadRequest = CreateMultipartUploadRequest.builder().bucket(properties.getS3BucketName()).key(fileName).serverSideEncryption(ServerSideEncryption.AES256).build();
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(uploadRequest);
        String uploadId = response.uploadId();

        final int sizeToUpload = 100 * 1024 * 1024;
        byte[] buf = new byte[sizeToUpload];
        final List<Future<CompletedPart>> parts = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(dumpFile);
             BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
            int partNum = 1;
            while (bufferedInputStream.read(buf) > 0) {
                final int num = new Integer(partNum);
                final byte[] uploadBytes = buf.clone();

                parts.add(executor.submit(() -> {
                    UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                            .bucket(properties.getS3BucketName())
                            .key(fileName)
                            .uploadId(uploadId)
                            .partNumber(num)
                            .build();
                    String etag = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(uploadBytes)).eTag();
                    return CompletedPart.builder()
                            .partNumber(num)
                            .eTag(etag)
                            .build();
                }));

                partNum++;
            }

            try (Stream<Future<CompletedPart>> partStream = parts.parallelStream()) {

                List<CompletedPart> partList = partStream.map(future -> {
                    return future.get(60, TimeUnit.SECONDS);
                }).collect(Collectors.toList());

                CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder().parts(partList).build();
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        CompleteMultipartUploadRequest.builder()
                                .bucket(properties.getS3BucketName())
                                .key(fileName)
                                .uploadId(uploadId)
                                .multipartUpload(completedMultipartUpload)
                                .build();
                s3Client.completeMultipartUpload(completeMultipartUploadRequest);
            }


            if (properties.isDeleteFileAfterSend()) {
                deleteFile(fileName);
            }
        }
    }

    private String generateChangeLog(Connection connection, String snapshotTypes, String author, DiffOutputControl diffOutputControl) throws IOException, ParserConfigurationException, LiquibaseException {
        Database database = getDatabase(connection);
        String fileName = makeChangeLogFileName(database);
        CommandLineUtils.doGenerateChangeLog(fileName, database, null, null, snapshotTypes, author, null, null, diffOutputControl);
        return fileName;
    }

    @SneakyThrows
    private Database getDatabase(Connection connection) {
        DatabaseConnection databaseConnection = new JdbcConnection(connection);
        return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(databaseConnection);
    }

    private String makeChangeLogFileName(Database database) {
        String fileId = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        BackupFormat format = properties.getFormat();
        String extension = format.name().toLowerCase();
        String databaseType = format == BackupFormat.XML ? String.format("%s.", database.getShortName()) : "";
        return String.format(CHANGELOG_FILE_NAME_TEMPLATE, fileId, databaseType, extension);
    }

    private DiffOutputControl makeDiffOutputControl() {
        DiffOutputControl diffOutputControl = new DiffOutputControl(false, false, false, null);
        List<String> tables = properties.getTables();
        if (tables != null && !tables.isEmpty()) {
            setTablesFilter(diffOutputControl, tables);
        }
        return diffOutputControl;
    }

    private void setTablesFilter(DiffOutputControl diffOutputControl, List<String> tables) {
        String tableNamesPattern = tables.stream()
                .map(t -> MessageFormat.format("table:(?i){0}", t))
                .collect(Collectors.joining(","));
        StandardObjectChangeFilter filter = new StandardObjectChangeFilter(StandardObjectChangeFilter.FilterType.INCLUDE, tableNamesPattern);
        diffOutputControl.setObjectChangeFilter(filter);
    }

    private void deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.delete()) {
            log.info("File {} deleted", fileName);
        } else {
            log.info("File {} NOT deleted", fileName);
        }
    }

}
