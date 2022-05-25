package com.x.down.core;

import com.x.down.config.Config;
import com.x.down.dispatch.Schedulers;
import com.x.down.listener.OnExecuteListener;
import com.x.down.task.ThreadTaskFactory;
import com.x.down.tool.XDownUtils;

import java.util.UUID;

public class XExecuteRequest implements IExecuteRequest {
    protected String tag;//标记
    protected boolean isUseAutoRetry = Config.config().isUseAutoRetry();//是否使用出错自动重试
    protected int autoRetryTimes = Config.config().getAutoRetryTimes();//自动重试次数
    protected int autoRetryInterval = Config.config().getAutoRetryInterval();//自动重试间隔
    protected Schedulers schedulers;//调度器
    protected Execute runnable;
    protected OnExecuteListener executeListener;

    public XExecuteRequest(Execute runnable) {
        this.runnable = runnable;
    }

    public static XExecuteRequest with(Execute runnable) {
        return new XExecuteRequest(runnable);
    }

    @Override
    public String getTag() {
        if (XDownUtils.isEmpty(tag)) {
            tag = UUID.randomUUID().toString();
        }
        return tag;
    }

    @Override
    public XExecuteRequest setTag(String tag) {
        this.tag = tag;
        return this;
    }

    public boolean isUseAutoRetry() {
        return isUseAutoRetry;
    }

    @Override
    public XExecuteRequest setUseAutoRetry(boolean useAutoRetry) {
        this.isUseAutoRetry = useAutoRetry;
        return this;
    }

    public int getAutoRetryTimes() {
        return autoRetryTimes;
    }

    @Override
    public XExecuteRequest setAutoRetryTimes(int autoRetryTimes) {
        this.autoRetryTimes = autoRetryTimes;
        return this;
    }

    public int getAutoRetryInterval() {
        return autoRetryInterval;
    }

    @Override
    public XExecuteRequest setAutoRetryInterval(int autoRetryInterval) {
        this.autoRetryInterval = autoRetryInterval;
        return this;
    }

    public Schedulers getSchedulers() {
        return schedulers;
    }

    @Override
    public XExecuteRequest setSchedulers(Schedulers schedulers) {
        this.schedulers = schedulers;
        return this;
    }

    public OnExecuteListener getExecuteListener() {
        return executeListener;
    }

    @Override
    public XExecuteRequest setExecuteListener(OnExecuteListener executeListener) {
        this.executeListener = executeListener;
        return this;
    }

    public Execute getExecute() {
        return runnable;
    }

    @Override
    public String start() {
        ThreadTaskFactory.createExecuteRequest(this);
        return getTag();
    }

}
