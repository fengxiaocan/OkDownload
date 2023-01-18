package com.ok.request;

import com.ok.request.dispatch.Dispatcher;

import java.util.HashSet;
import java.util.Set;

final class TaskPool {
    private final Set<Dispatcher> pool = new HashSet<>();

    public TaskPool() {
    }

    public TaskPool(Dispatcher request) {
        this.pool.add(request);
    }

    public void addRequest(Dispatcher request) {
        this.pool.add(request);
    }

    public boolean removeRequest(Dispatcher request) {
        this.pool.remove(request);
        return this.pool.size() <= 0;
    }

    public Set<Dispatcher> getPool() {
        return pool;
    }
}
