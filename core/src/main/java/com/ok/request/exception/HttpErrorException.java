package com.ok.request.exception;

import java.io.IOException;

public final class HttpErrorException extends IOException {
    private int responseCode;
    private String url;
    private String reason;

    /**
     * Constructs a new {@code HttpRetryException} from the
     * specified response code and exception detail message
     *
     * @param reason the detail message.
     * @param code   the HTTP response code from server.
     */
    public HttpErrorException(int code, String url,String reason) {
        super(String.format("Http request is error:url=%s code=%d reason=%s", url,code,reason));
        responseCode = code;
        this.url = url;
        this.reason = reason;
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
        return reason;
    }
}
