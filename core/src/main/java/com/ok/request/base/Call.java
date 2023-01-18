package com.ok.request.base;


import com.ok.request.request.Request;
import com.ok.request.request.Response;

public interface Call {
    Response process(Request request) throws Throwable;

    void terminated();

    void cancel();

    boolean isCanceled();

    interface Interceptor {
        Response intercept(Request request) throws Throwable;
    }
}
