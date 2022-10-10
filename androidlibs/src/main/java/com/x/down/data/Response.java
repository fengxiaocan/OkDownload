package com.x.down.data;

public final class Response {
    private String url;
    private String result;
    private int code;
    private String error;
    private Headers headers;

    public static Response builderFailure(String url,int code, Headers headers, String error) {
        Response tResponse = new Response();
        tResponse.url = url;
        tResponse.code = code;
        tResponse.error = error;
        tResponse.headers = headers;
        return tResponse;
    }

    public static Response builderSuccess(String url,String result, int code, Headers headers) {
        Response tResponse = new Response();
        tResponse.url = url;
        tResponse.result = result;
        tResponse.code = code;
        tResponse.error = null;
        tResponse.headers = headers;
        return tResponse;
    }

    public String url() {
        return url;
    }

    public String result() {
        return result;
    }

    public int code() {
        return code;
    }

    public String error() {
        return error;
    }

    public Headers headers() {
        return headers;
    }

    public boolean isSuccess() {
        return code >= 200 && code < 400;
    }
}
