package com.ok.request.exception;

public class RetryTaskException extends Exception{
    public RetryTaskException() {
    }
    public RetryTaskException(String message) {
        super(message);
    }

    public RetryTaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public RetryTaskException(Throwable cause) {
        super(cause);
    }
}
