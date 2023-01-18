package com.ok.request;

import android.os.Handler;
import android.os.Looper;

import com.ok.request.dispatch.Schedulers;


public final class AndroidSchedulers {

    public static Schedulers mainThread() {
        return MainHolder.DEFAULT;
    }

    public static Schedulers from(Handler handler) {
        return new HandlerScheduler(handler);
    }

    private static final class MainHolder {
        static final Schedulers DEFAULT = new HandlerScheduler(new Handler(Looper.getMainLooper()));
    }

    private static class HandlerScheduler implements Schedulers {
        private final Handler handler;

        public HandlerScheduler(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void schedule(Runnable runnable) {
            handler.post(runnable);
        }
    }
}
