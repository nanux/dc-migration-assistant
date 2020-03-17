package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.core.exceptions.FileUploadException;
import com.atlassian.migration.datacenter.core.util.UploadQueue;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Copyright Atlassian: 13/03/2020
 */
public class FilesystemUploader
{
    private Uploader uploader;
    private Crawler crawler;

    public FilesystemUploader(Crawler crawler, Uploader uploader)
    {
        this.uploader = uploader;
        this.crawler = crawler;
    }

    public void uploadDirectory(Path dir) throws FileUploadException
    {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        UploadQueue<Path> queue = new UploadQueue<>(uploader.maxConcurrent());

        Future<Boolean> crawlFuture = pool.submit(() -> {
            crawler.crawlDirectory(dir, queue);
            return true;
        });
        Future<Boolean> uploadFuture = pool.submit(() -> {
            uploader.upload(queue);
            return true;
        });

        try {
            crawlFuture.get();
            uploadFuture.get();
        } catch (InterruptedException e) {
            throw new FileUploadException("Failed to traverse/upload filesystem: "+dir,e);
        } catch (ExecutionException e) {
            throw new FileUploadException("Failed to traverse/upload filesystem: "+dir,e.getCause());
        }

        pool.shutdown();
    }


}
