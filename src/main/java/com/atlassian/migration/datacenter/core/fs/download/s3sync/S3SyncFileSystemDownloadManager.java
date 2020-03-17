package com.atlassian.migration.datacenter.core.fs.download.s3sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class S3SyncFileSystemDownloadManager {

    private static final Logger logger = LoggerFactory.getLogger(S3SyncFileSystemDownloadManager.class);

    private final S3SyncFileSystemDownloader downloader;

    public S3SyncFileSystemDownloadManager(S3SyncFileSystemDownloader downloader) {
        this.downloader = downloader;
    }

    public void downloadFileSystem() throws S3SyncFileSystemDownloader.CannotLaunchCommandException {
        downloader.initiateFileSystemDownload();

        CompletableFuture<?> syncCompleteFuture = new CompletableFuture<>();


        ScheduledFuture<?> scheduledFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            try {
                S3SyncCommandStatus status = downloader.getFileSystemDownloadStatus();

                if (status.isComplete()) {
                    syncCompleteFuture.complete(null);
                }
            } catch (S3SyncFileSystemDownloader.IndeterminateS3SyncStatusException e) {
                logger.error("error when retrieving s3 sync status", e);
            }
        }, 0 , 5, TimeUnit.MINUTES);

        syncCompleteFuture.whenComplete((_i, _j) -> scheduledFuture.cancel(true));
    }
}
