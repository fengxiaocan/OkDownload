package com.x.down.base;

import com.x.down.core.XExecuteRequest;

public interface IExecute {
    /**
     * 获取请求
     *
     * @return
     */
    XExecuteRequest request();

    /**
     * 获取tag
     *
     * @return
     */
    String tag();

    /**
     * 重试次数
     *
     * @return
     */
    int retryCount();
}
