package com.x.down.listener;

import com.x.down.base.IRequest;
import com.x.down.data.Response;

public interface OnResponseListener {
    void onResponse(IRequest request, Response response);

    void onError(IRequest request, Throwable exception);

    class IMPL implements OnResponseListener {

        @Override
        public void onResponse(IRequest request, Response response) {

        }

        @Override
        public void onError(IRequest request, Throwable exception) {

        }
    }
}
