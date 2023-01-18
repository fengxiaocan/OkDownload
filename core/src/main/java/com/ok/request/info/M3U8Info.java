package com.ok.request.info;

import java.io.Serializable;
import java.util.ArrayList;

public class M3U8Info implements Serializable {

    private String mUrl;
    private final ArrayList<M3U8Ts> mTsList = new ArrayList<>();
    private float mTargetDuration;
    private int mInitSequence;
    private int mVersion = 3;
    private boolean mHasEndList;
    private boolean isNeedRedirect = false;

    public M3U8Info() {
        this("");
    }

    public M3U8Info(String url) {
        mUrl = url;
        mInitSequence = 0;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    public void addTs(M3U8Ts ts) {
        mTsList.add(ts);
    }

    public void setSequence(int sequence) {
        mInitSequence = sequence;
    }

    public void setHasEndList(boolean hasEndList) {
        mHasEndList = hasEndList;
    }

    public ArrayList<M3U8Ts> getTsList() {
        return mTsList;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        mVersion = version;
    }

    public float getTargetDuration() {
        return mTargetDuration;
    }

    public void setTargetDuration(float targetDuration) {
        mTargetDuration = targetDuration;
    }

    public int getInitSequence() {
        return mInitSequence;
    }

    public boolean hasEndList() {
        return mHasEndList;
    }

    public boolean isNeedRedirect() {
        return isNeedRedirect;
    }

    public void setNeedRedirect(boolean needRedirect) {
        isNeedRedirect = needRedirect;
    }

    public long getDuration() {
        long duration = 0L;
        for (M3U8Ts ts : mTsList) {
            duration += ts.getDuration();
        }
        return duration;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof M3U8Info) {
            M3U8Info m3U8Info = (M3U8Info) obj;
            return mUrl != null && mUrl.equals(m3U8Info.mUrl);
        }
        return false;
    }

    @Override
    public String toString() {
        return "M3U8Info{" +
                "mUrl='" + mUrl + '\'' +
                ", mTsList=" + mTsList.toString() +
                ", mTargetDuration=" + mTargetDuration +
                ", mInitSequence=" + mInitSequence +
                ", mVersion=" + mVersion +
                ", mHasEndList=" + mHasEndList +
                '}';
    }
}
