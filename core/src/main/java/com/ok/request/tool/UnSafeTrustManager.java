package com.ok.request.tool;

import javax.net.ssl.X509TrustManager;

/**
 * 接受所有的Https请求
 */
public class UnSafeTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
    }

    @Override
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[0];
    }
}