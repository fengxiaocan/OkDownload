package com.ok.request.request;

import com.ok.request.base.RequestBody;
import com.ok.request.params.Headers;

import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.SSLSocketFactory;

public interface Request {
    URL url();

    String method();

    Headers headers();

    RequestBody body();

    SSLSocketFactory certificate();

    int TimeOut();


    void method(String method);

    void setHeaders(Headers headers);

    void addHeader(Headers headers);

    void addHeader(String name, String value);

    void body(RequestBody body);

    void certificate(SSLSocketFactory factory);

    void TimeOut(int timeOut);


    Request clone(String url) throws MalformedURLException;

    Request clone(URL url);
}
