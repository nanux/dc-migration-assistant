/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.atlassian.migration.datacenter.core.fs;

import com.atlassian.migration.datacenter.spi.fs.reporting.FileSystemMigrationReport;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilesystemUploaderRx {
    private static final Logger logger = LoggerFactory.getLogger(FilesystemUploaderRx.class);
    private S3UploadConfig config;
    private FileSystemMigrationReport reporter;

    public FilesystemUploaderRx(S3UploadConfig config, FileSystemMigrationReport reporter) {
        this.config = config;
        this.reporter = reporter;
    }

    //Deal with backpressure using cold observables
    //https://github.com/ReactiveX/RxJava/wiki/Backpressure
    public void upload(Path dir) throws Exception {
        SubscriberS3Uploader subscriberS3Uploader = new SubscriberS3Uploader(config, reporter);
        Observable<Path> pathObservable = new ObservableCrawler().create(dir);
        subscriberS3Uploader.upload(pathObservable);
        //TODO - Make subscriber return an observable and report on the result external to the upload
    }

}

class ObservableCrawler {
    private static Logger logger = LoggerFactory.getLogger(ObservableCrawler.class);

    public Observable<Path> create(Path rootDirectory) throws Exception {
        DirectoryStream<Path> rootDirectoryStream = Files.newDirectoryStream(rootDirectory);
        return Observable.<Path>create(emitter -> {
            listDirectories(rootDirectoryStream, emitter);
        }).subscribeOn(Schedulers.io());
    }

    private void listDirectories(DirectoryStream<Path> paths, ObservableEmitter<Path> emitter) {
        paths.forEach(p -> {
            if (Files.isDirectory(p)) {
                try (final DirectoryStream<Path> newPaths = Files.newDirectoryStream(p.toAbsolutePath())) {
                    listDirectories(newPaths, emitter);
                } catch (Exception e) {
                    logger.error("Error when traversing directory {}, with exception {}", p, e);
                    emitter.onError(e);
                }
            } else {
                logger.trace("queueing file: {}", p);
                emitter.onNext(p);
            }
        });
        emitter.onComplete();
    }
}


class SubscriberS3Uploader {

    private final S3Uploader s3Uploader;

    public SubscriberS3Uploader(S3UploadConfig config, FileSystemMigrationReport reporter) {
        s3Uploader = new S3Uploader(config, reporter);
    }

    public void upload(Observable<Path> paths) {
        //Because we do subscribe on it is not needed
        paths.observeOn(Schedulers.io())
                .blockingSubscribe(s3Uploader::uploadFile);

    }

}