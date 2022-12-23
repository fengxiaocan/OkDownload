package com.x.down.task;

final class RetryTaskException extends RuntimeException{
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
