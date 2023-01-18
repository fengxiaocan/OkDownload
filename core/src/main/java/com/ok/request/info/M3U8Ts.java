package com.ok.request.info;

import com.ok.request.tool.XDownUtils;

import java.io.File;
import java.io.Serializable;


public class M3U8Ts implements Comparable<M3U8Ts>, Serializable {
    private float mDuration;                     //分片时长
    private int mIndex;                          //分片索引值,第一个为0
    private int mSequence;                       //分片的sequence, 根据initSequence自增得到的
    private String mUrl;                         //分片url
    private String mName;                        //分片名,可以自己定义
    private long mTsSize;                        //分片大小
    private boolean mHasDiscontinuity;           //分片前是否有#EXT-X-DISCONTINUITY标识
    private boolean mHasKey;                     //分片是否有#EXT-X-KEY
    private String mMethod;                      //加密的方式
    private String mKeyUri;                      //加密的url
    private String mKeyIV;                       //加密的IV
    private long mInitSegmentLength;             //MAP的url的Length
    private boolean mHasInitSegment;             //分片前是否有#EXT-X-MAP
    private String mInitSegmentUri;              //MAP的url
    private String mSegmentByteRange;            //MAP的range

    public M3U8Ts() {
    }

    public void initTsAttributes(String url, float duration, int index,
                                 int sequence, boolean hasDiscontinuity) {
        mUrl = url;
        mName = url;
        mDuration = duration;
        mIndex = index;
        mSequence = sequence;
        mHasDiscontinuity = hasDiscontinuity;
        mTsSize = 0L;
    }

    public void setKeyConfig(String method, String keyUri, String keyIV) {
        mHasKey = true;
        mMethod = method;
        mKeyUri = keyUri;
        mKeyIV = keyIV;
    }

    public void setInitSegmentInfo(String initSegmentUri, String segmentByteRange) {
        mHasInitSegment = true;
        mInitSegmentUri = initSegmentUri;
        mSegmentByteRange = segmentByteRange;
    }

    public int getSequence() {
        return mSequence;
    }

    public boolean hasKey() {
        return mHasKey;
    }

    public String getMethod() {
        return mMethod;
    }

    public String getKeyUri() {
        return mKeyUri;
    }

    public String getLocalKeyUri() {
        return "local_" + mIndex + ".key";
    }

    public File getKeyFile(File dir) {
        return new File(dir, getLocalKeyUri());
    }

    public String getKeyIV() {
        return mKeyIV;
    }

    public float getDuration() {
        return mDuration;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getName() {
        return mName;
    }

    /**
     * if ts is local file, name is video_{index}.ts
     * if ts is network resource , name is starting with http or https.
     *
     * @param name
     */
    public void setName(String name) {
        this.mName = name;
    }

    public String getIndexName() {
        String suffixName = "";
        String fileName = XDownUtils.getUrlName(mUrl).toLowerCase();
        suffixName = XDownUtils.getSuffixName(fileName);
        return "m3u8_" + mIndex + suffixName;
    }

    public File getTsFile(File dir) {
        return new File(dir, getIndexName());
    }

    public long getTsSize() {
        return mTsSize;
    }

    public void setTsSize(long tsSize) {
        mTsSize = tsSize;
    }

    public boolean hasDiscontinuity() {
        return mHasDiscontinuity;
    }

    public long getInitSegmentLength() {
        return mInitSegmentLength;
    }

    public void setInitSegmentLength(long mInitSegmentLength) {
        this.mInitSegmentLength = mInitSegmentLength;
    }

    public boolean hasInitSegment() {
        return mHasInitSegment;
    }

    public String getInitSegmentUri() {
        return mInitSegmentUri;
    }

    public String getSegmentByteRange() {
        return mSegmentByteRange;
    }

    public String getInitSegmentName() {
        String suffixName = "";
        String fileName = XDownUtils.getUrlName(mInitSegmentUri).toLowerCase();
        suffixName = XDownUtils.getSuffixName(fileName);
        return "init_segment_" + mIndex + suffixName;
    }

    public File getInitSegmentFile(File dir) {
        return new File(dir, getInitSegmentName());
    }


    @Override
    public String toString() {
        return "M3U8Ts{" +
                "mDuration=" + mDuration +
                ", mIndex=" + mIndex +
                ", mSequence=" + mSequence +
                ", mUrl='" + mUrl + '\'' +
                ", mName='" + mName + '\'' +
                ", mTsSize=" + mTsSize +
                ", mHasDiscontinuity=" + mHasDiscontinuity +
                ", mHasKey=" + mHasKey +
                ", mMethod='" + mMethod + '\'' +
                ", mKeyUri='" + mKeyUri + '\'' +
                ", mKeyIV='" + mKeyIV + '\'' +
                ", mInitSegmentLength=" + mInitSegmentLength +
                ", mHasInitSegment=" + mHasInitSegment +
                ", mInitSegmentUri='" + mInitSegmentUri + '\'' +
                ", mSegmentByteRange='" + mSegmentByteRange + '\'' +
                '}';
    }

    @Override
    public int compareTo(M3U8Ts object) {
        return mName.compareTo(object.mName);
    }
}

