package com.ok.request.request;

import com.ok.request.base.RequestBody;
import com.ok.request.params.Headers;

import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.SSLSocketFactory;

public class URLRequest implements Request {
    private final URL url;
    private String method;
    private Headers headers;
    private RequestBody body;
    private SSLSocketFactory certificate;
    private int timeOut;

    public URLRequest(URL url) {
        this.url = url;
    }

    public URLRequest(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    @Override
    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    @Override
    public void addHeader(Headers headers) {
        if (this.headers != null) {
            this.headers.addHeaders(headers);
        } else {
            this.headers = headers;
        }
    }

    @Override
    public void addHeader(String name, String value) {
        if (this.headers == null) {
            this.headers = new Headers();
        }
        this.headers.addHeader(name, value);
    }

    @Override
    public void body(RequestBody body) {
        this.body = body;
    }

    @Override
    public void certificate(SSLSocketFactory factory) {
        this.certificate = factory;
    }

    @Override
    public void TimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    @Override
    public void method(String method) {
        this.method = method;
    }

    @Override
    public URL url() {
        return url;
    }

    @Override
    public String method() {
        return method;
    }

    @Override
    public Headers headers() {
        return headers;
    }

    @Override
    public RequestBody body() {
        return body;
    }

    @Override
    public SSLSocketFactory certificate() {
        return certificate;
    }

    @Override
    public int TimeOut() {
        return timeOut;
    }


    @Override
    public Request clone(String url) throws MalformedURLException {
        URLRequest httpRequest = new URLRequest(url);
        httpRequest.method = method;
        httpRequest.headers = headers;
        httpRequest.body = body;
        httpRequest.certificate = certificate;
        httpRequest.timeOut = timeOut;
        return httpRequest;
    }

    @Override
    public Request clone(URL url) {
        URLRequest httpRequest = new URLRequest(url);
        httpRequest.method = method;
        httpRequest.headers = headers;
        httpRequest.body = body;
        httpRequest.certificate = certificate;
        httpRequest.timeOut = timeOut;
        return httpRequest;
    }
}
