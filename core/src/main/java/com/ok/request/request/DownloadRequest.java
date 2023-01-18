package com.ok.request.request;

import com.ok.request.base.HttpConnect;
import com.ok.request.base.RequestBody;
import com.ok.request.params.Headers;

import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.SSLSocketFactory;

public class DownloadRequest implements Request {
    private final URL url;
    private Headers headers;
    private SSLSocketFactory certificate;
    private int timeOut;

    public DownloadRequest(URL url) {
        this.url = url;
    }

    public DownloadRequest(String url) throws MalformedURLException {
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
    }

    @Override
    public URL url() {
        return url;
    }

    @Override
    public String method() {
        return HttpConnect.Method.GET.getMethod();
    }

    @Override
    public Headers headers() {
        return headers;
    }

    @Override
    public RequestBody body() {
        return null;
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
        DownloadRequest httpRequest = new DownloadRequest(url);
        httpRequest.headers = headers;
        httpRequest.certificate = certificate;
        httpRequest.timeOut = timeOut;
        return httpRequest;
    }

    @Override
    public Request clone(URL url) {
        DownloadRequest httpRequest = new DownloadRequest(url);
        httpRequest.headers = headers;
        httpRequest.certificate = certificate;
        httpRequest.timeOut = timeOut;
        return httpRequest;
    }
}
