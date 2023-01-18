package com.ok.request.core;

import com.ok.request.base.Execute;
import com.ok.request.base.IExecuteRequest;
import com.ok.request.config.Config;
import com.ok.request.dispatch.Dispatcher;
import com.ok.request.dispatch.OnDispatcher;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.disposer.AutoRetryRecorder;
import com.ok.request.factory.ThreadTaskFactory;
import com.ok.request.listener.OnExecuteListener;

public class OkExecute implements IExecuteRequest, OnDispatcher {
    protected Object tag = this.hashCode();
    protected boolean isUseAutoRetry = Config.config().isUseAutoRetry();//是否使用出错自动重试
    protected int autoRetryTimes = Config.config().getAutoRetryTimes();//自动重试次数
    protected int autoRetryInterval = Config.config().getAutoRetryInterval();//自动重试间隔
    protected Schedulers schedulers;//调度器
    protected Execute runnable;
    protected OnExecuteListener executeListener;

    public OkExecute(Execute runnable) {
        this.runnable = runnable;
    }

    public static OkExecute with(Execute runnable) {
        return new OkExecute(runnable);
    }

    @Override
    public final Object getTag() {
        return tag;
    }

    @Override
    public OkExecute setTag(Object tag) {
        this.tag = tag;
        return this;
    }

    @Override
    public OkExecute setUseAutoRetry(boolean useAutoRetry) {
        this.isUseAutoRetry = useAutoRetry;
        return this;
    }

    @Override
    public OkExecute setAutoRetryTimes(int autoRetryTimes) {
        this.autoRetryTimes = autoRetryTimes;
        return this;
    }

    @Override
    public OkExecute setAutoRetryInterval(int autoRetryInterval) {
        this.autoRetryInterval = autoRetryInterval;
        return this;
    }

    @Override
    public OkExecute scheduleOn(Schedulers schedulers) {
        this.schedulers = schedulers;
        return this;
    }

    @Override
    public OkExecute setExecuteListener(OnExecuteListener executeListener) {
        this.executeListener = executeListener;
        return this;
    }

    public Execute getExecute() {
        return runnable;
    }

    @Override
    public Dispatcher start() {
        return ThreadTaskFactory.createExecuteRequest(this);
    }

    @Override
    public Schedulers schedulers() {
        return schedulers;
    }

    @Override
    public AutoRetryRecorder recorder() {
        return new AutoRetryRecorder(isUseAutoRetry, autoRetryTimes, autoRetryInterval);
    }

    @Override
    public OnExecuteListener executor() {
        return executeListener;
    }
}
