package com.atlassian.migration.datacenter.core.exceptions;

/**
 * Copyright Atlassian: 13/03/2020
 */
public class FileUploadException extends RuntimeException
{
    public FileUploadException(String message)
    {
        super(message);
    }

    public FileUploadException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
