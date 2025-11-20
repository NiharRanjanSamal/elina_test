package com.elina.authorization.exception;

/**
 * Exception thrown when authentication fails.
 * Results in HTTP 400 response.
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

