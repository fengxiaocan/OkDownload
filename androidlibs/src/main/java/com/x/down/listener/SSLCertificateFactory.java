package com.x.down.listener;

import javax.net.ssl.SSLSocketFactory;

public interface SSLCertificateFactory {
    SSLSocketFactory createCertificate();
}
