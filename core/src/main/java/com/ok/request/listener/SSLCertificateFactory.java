package com.ok.request.listener;

import javax.net.ssl.SSLSocketFactory;

public interface SSLCertificateFactory {
    SSLSocketFactory createCertificate();
}
