package com.ok.request.call;

import com.ok.request.request.Request;
import com.ok.request.request.Response;

public interface Connection {
    void request(Request request) throws Exception;

    Request responseRedirects() throws Exception;

    Response response() throws Exception;

    void terminated();
}
