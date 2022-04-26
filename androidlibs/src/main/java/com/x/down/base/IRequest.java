package com.x.down.base;

import com.x.down.core.HttpConnect;

public interface IRequest {
    /**
     * 获取请求
     *
     * @return
     */
    HttpConnect request();

    /**
     * 获取tag
     *
     * @return
     */
    String tag();

    /**
     * 获取Url
     *
     * @return
     */
    String url();

    /**
     * 重试次数
     *
     * @return
     */
    int retryCount();
}
