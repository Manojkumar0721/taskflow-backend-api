package com.taskflow.exception;

/**
 * Custom runtime exception thrown when attempting to register with an email that already exists.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
