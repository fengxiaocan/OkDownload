package com.x.down.task;

final class CancelTaskException extends RuntimeException{
    public CancelTaskException() {
    }
    public CancelTaskException(String message) {
        super(message);
    }

    public CancelTaskException(String message, Throwable cause) {
        super(message, cause);
    }

    public CancelTaskException(Throwable cause) {
        super(cause);
    }
}
