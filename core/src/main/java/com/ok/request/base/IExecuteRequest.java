package com.ok.request.base;

import com.ok.request.dispatch.Dispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.listener.OnExecuteListener;

public interface IExecuteRequest {
    Object getTag();

    IExecuteRequest setTag(Object tag);

    IExecuteRequest setUseAutoRetry(boolean useAutoRetry);

    IExecuteRequest setAutoRetryTimes(int autoRetryTimes);

    IExecuteRequest setAutoRetryInterval(int autoRetryInterval);

    IExecuteRequest scheduleOn(Schedulers schedulers);

    IExecuteRequest setOnExecuteListener(OnExecuteListener executeListener);

    Dispatcher start();
}
