package com.atlassian.migration.datacenter.core.application;

/**
 * Copyright Atlassian: 04/03/2020
 */
public interface ApplicationConfiguration
{

    class ConfigurationReadException extends RuntimeException {
        public ConfigurationReadException(String message) {
            super(message);
        }

        public ConfigurationReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    class UnsupportedPasswordEncoding extends RuntimeException {
        public UnsupportedPasswordEncoding(String message)
        {
            super(message);
        }
    }

    DatabaseConfiguration getDatabaseConfiguration() throws ConfigurationReadException;
}
