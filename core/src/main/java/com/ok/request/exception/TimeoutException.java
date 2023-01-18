package com.ok.request.exception;

public class TimeoutException extends Exception {
    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
