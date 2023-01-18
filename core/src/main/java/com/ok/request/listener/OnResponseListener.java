package com.ok.request.listener;

import com.ok.request.base.Executor;
import com.ok.request.request.HttpResponse;

public interface OnResponseListener {
    void onResponse(Executor request, HttpResponse response);
}
