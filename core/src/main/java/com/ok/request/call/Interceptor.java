package com.ok.request.call;

import com.ok.request.request.Request;

public interface Interceptor {
    Connection intercept(Request request) throws Throwable;
}
