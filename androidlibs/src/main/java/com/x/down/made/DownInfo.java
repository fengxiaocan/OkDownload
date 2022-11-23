package com.x.down.made;

import java.io.Serializable;

public class DownInfo implements Serializable {
    private long length = 0;
    private boolean accecp;

    public DownInfo() {
    }

    public DownInfo(long length, boolean accecp) {
        this.length = length;
        this.accecp = accecp;
    }

    public long getLength() {
        return length;
    }

    public DownInfo setLength(long length) {
        this.length = length;
        return this;
    }

    public boolean isAccecp() {
        return accecp;
    }

    public DownInfo setAccecp(boolean accecp) {
        this.accecp = accecp;
        return this;
    }
}
