package com.ok.request.call;

import com.ok.request.base.Call;
import com.ok.request.exception.CancelTaskException;
import com.ok.request.request.Request;
import com.ok.request.request.Response;

public class RequestCall implements Call {
    private volatile boolean isCanceled = false;
    private volatile Connection connection;
    private com.ok.request.call.Interceptor networkInterceptors;
    private Call.Interceptor interceptors;

    public void setInterceptors(Interceptor interceptors) {
        this.interceptors = interceptors;
    }

    public void setNetworkInterceptors(com.ok.request.call.Interceptor networkInterceptors) {
        this.networkInterceptors = networkInterceptors;
    }

    @Override
    public Response process(Request request) throws Throwable {
        terminated();
        if (interceptors != null) {
            Response intercept = interceptors.intercept(request);
            if (intercept != null) {
                return intercept;
            }
        }

        Request call = request;
        while (call != null) {
            terminated();
            if (networkInterceptors != null) {
                connection = networkInterceptors.intercept(call);
            } else {
                connection = new HttpConnection();
            }
            //开始连接
            connection.request(call);
            //检测取消
            checkCanceled();
            //重定向
            call = connection.responseRedirects();
        }
        //检测取消
        checkCanceled();
        return connection.response();
    }

    @Override
    public void terminated() {
        if (connection != null) {
            connection.terminated();
        }
        connection = null;
    }

    @Override
    public void cancel() {
        this.isCanceled = true;
        if (connection != null) {
            connection.terminated();
        }
    }

    @Override
    public boolean isCanceled() {
        return isCanceled;
    }

    public void checkCanceled() {
        if (isCanceled()) {
            throw new CancelTaskException();
        }
    }
}
