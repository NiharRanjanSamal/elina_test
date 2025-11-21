package com.elina.projects.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 response.
 */
public class NotFoundException extends RuntimeException {
    
    public NotFoundException(String message) {
        super(message);
    }
    
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

