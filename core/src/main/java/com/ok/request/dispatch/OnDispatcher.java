package com.ok.request.dispatch;

import com.ok.request.disposer.AutoRetryRecorder;
import com.ok.request.listener.OnExecuteListener;

public interface OnDispatcher {
    Object getTag();

    Schedulers schedulers();

    AutoRetryRecorder recorder();

    OnExecuteListener executor();
}
