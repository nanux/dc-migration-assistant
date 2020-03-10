package com.atlassian.migration.datacenter.core.database;

import com.atlassian.jira.config.util.JiraHome;
import com.atlassian.migration.datacenter.core.database.datasource.LiquibaseDatasource;
import com.atlassian.migration.datacenter.core.database.enums.BackupFormat;
import com.atlassian.migration.datacenter.core.database.enums.SnapshotType;
import com.atlassian.migration.datacenter.core.database.properties.LiquibaseBackupProperties;
import com.atlassian.migration.datacenter.core.fs.S3MultiPartUploader;
import com.atlassian.migration.datacenter.core.fs.S3UploadConfig;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
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
import software.amazon.awssdk.services.s3.S3AsyncClient;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class S3DatabaseBackupService implements DatabaseBackupService {

    private static final String CHANGELOG_FILE_NAME_TEMPLATE = "backup_%s.%s%s";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String ACCELLERATION_SUFFIX = ".s3-accelerate.dualstack.amazonaws.com";
    private final LiquibaseDatasource dataSource;
    private final LiquibaseBackupProperties properties;
    private final S3AsyncClient s3Client;
    private final JiraHome jiraHome;

    @Inject
    public S3DatabaseBackupService(LiquibaseDatasource dataSource, LiquibaseBackupProperties properties, S3AsyncClient s3Client, @ComponentImport JiraHome jiraHome) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.s3Client = s3Client;
        this.jiraHome = jiraHome;
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

        S3UploadConfig s3UploadConfig = new S3UploadConfig(this.properties.getS3BucketName(), this.s3Client, this.jiraHome.getHome().toPath());
        S3MultiPartUploader multiPartUploader = new S3MultiPartUploader(s3UploadConfig);

        multiPartUploader.multiPartUpload(dumpFile, fileName);

        if (properties.isDeleteFileAfterSend()) {
            deleteFile(fileName);
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
