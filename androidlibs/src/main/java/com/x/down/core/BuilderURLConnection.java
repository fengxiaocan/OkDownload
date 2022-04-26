package com.x.down.core;

import java.net.HttpURLConnection;

public interface BuilderURLConnection {
    HttpURLConnection buildConnect(String connectUrl) throws Exception;

    HttpURLConnection buildConnect() throws Exception;
}
