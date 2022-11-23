package com.x.down.net;

import java.io.IOException;

public class HttpErrorException extends IOException {
    private int responseCode;

    /**
     * Constructs a new {@code HttpRetryException} from the
     * specified response code and exception detail message
     *
     * @param detail the detail message.
     * @param code   the HTTP response code from server.
     */
    public HttpErrorException(int code, String detail) {
        super(detail);
        responseCode = code;
    }

    /**
     * Returns the http response code
     *
     * @return The http response code.
     */
    public int responseCode() {
        return responseCode;
    }

    /**
     * Returns a string explaining why the http request could
     * not be retried.
     *
     * @return The reason string
     */
    public String getReason() {
        return super.getMessage();
    }
}
