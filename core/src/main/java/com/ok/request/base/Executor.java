package com.ok.request.base;

import com.ok.request.dispatch.Dispatcher;

public interface Executor {
    Dispatcher call();

    Object tag();
}
