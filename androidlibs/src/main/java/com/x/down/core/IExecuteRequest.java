package com.x.down.core;

import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnExecuteListener;

public interface IExecuteRequest {
    String getTag();

    IExecuteRequest setTag(String tag);

    IExecuteRequest setUseAutoRetry(boolean useAutoRetry);

    IExecuteRequest setAutoRetryTimes(int autoRetryTimes);

    IExecuteRequest setAutoRetryInterval(int autoRetryInterval);

    IExecuteRequest setSchedulers(Schedulers schedulers);

    IExecuteRequest setExecuteListener(OnExecuteListener executeListener);

    String start();
}
