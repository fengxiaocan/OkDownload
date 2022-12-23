package com.x.down.listener;

import java.net.HttpURLConnection;

public interface OnRequestInterceptor {
    HttpURLConnection onIntercept(HttpURLConnection httpURLConnection);
}
