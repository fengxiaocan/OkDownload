package com.ok.request.tool;


import com.ok.request.base.DownloadExecutor;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.dispatch.Schedulers;
import com.ok.request.info.M3U8Info;
import com.ok.request.info.M3U8Ts;
import com.ok.request.listener.OnExecuteListener;
import com.ok.request.listener.OnMergeM3u8Listener;
import com.ok.request.m3u8.M3U8Constants;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U8Utils {

    public static boolean mergeM3u8(final DownloadExecutor executor, OkDownloadRequest request, File saveFile, M3U8Info info) {
        File tempDir = XDownUtils.getTempCacheDir2(request);
        File m3u8Dir = request.getM3u8Dir();
        m3u8Dir.getParentFile().mkdirs();
        tempDir.renameTo(m3u8Dir);
        try {
            createLocalM3U8File(saveFile, m3u8Dir, info);
            OnMergeM3u8Listener listener = request.mergeM3u8Listener();
            if (listener != null) {
                listener.onM3u8Merge(executor, m3u8Dir, info);
            }
            return true;
        } catch (final Exception e) {
            callError(executor, request, e);
            return false;
        }
    }

    private static void callError(final DownloadExecutor executor, OkDownloadRequest request, final Exception e) {
        final OnExecuteListener listener = request.executor();
        //错误回调
        Schedulers schedulers = request.schedulers();
        if (listener != null) {
            if (schedulers != null) {
                schedulers.schedule(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(executor, e);
                    }
                });
            } else {
                listener.onError(executor, e);
            }
        }
        request.callDownloadFailure(executor);
    }

    /**
     * 解析网络上的M3u8信息
     *
     * @param baseUrl
     * @param bufferedReader
     * @return M3U8Info
     * @throws Exception
     */
    public static M3U8Info parseNetworkM3U8Info(final String baseUrl, BufferedReader bufferedReader) throws Exception {
        M3U8Info m3U8Info = new M3U8Info();
        m3U8Info.setUrl(baseUrl);
        float tsDuration = 0;
        int targetDuration = 0;
        int tsIndex = 0;
        int version = 0;
        int sequence = 0;
        boolean hasDiscontinuity = false;
        boolean hasEndList = false;
        boolean hasStreamInfo = false;
        boolean hasKey = false;
        boolean hasInitSegment = false;
        String method = null;
        String encryptionIV = null;
        String encryptionKeyUri = null;
        String initSegmentUri = null;
        String segmentByteRange = null;
        String line;
        String tempWaitKeyUri = null;
        while ((line = bufferedReader.readLine()) != null) {
            line = line.trim();
            if (XDownUtils.isEmpty(line)) {
                continue;
            }
            if (line.startsWith(M3U8Constants.TAG_PREFIX)) {
                if (line.startsWith(M3U8Constants.TAG_MEDIA_DURATION)) {
                    String ret = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_MEDIA_DURATION);
                    if (!XDownUtils.isEmpty(ret)) {
                        tsDuration = Float.parseFloat(ret);
                    }
                } else if (line.startsWith(M3U8Constants.TAG_TARGET_DURATION)) {
                    String ret = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_TARGET_DURATION);
                    if (!XDownUtils.isEmpty(ret)) {
                        targetDuration = Integer.parseInt(ret);
                    }
                } else if (line.startsWith(M3U8Constants.TAG_VERSION)) {
                    String ret = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_VERSION);
                    if (!XDownUtils.isEmpty(ret)) {
                        version = Integer.parseInt(ret);
                    }
                } else if (line.startsWith(M3U8Constants.TAG_MEDIA_SEQUENCE)) {
                    String ret = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_MEDIA_SEQUENCE);
                    if (!XDownUtils.isEmpty(ret)) {
                        sequence = Integer.parseInt(ret);
                    }
                } else if (line.startsWith(M3U8Constants.TAG_STREAM_INF)) {
                    //EXT-X-STREAM-INF 字段是用来表示一个可变视频流的标签。
                    hasStreamInfo = true;
                } else if (line.startsWith(M3U8Constants.TAG_DISCONTINUITY)) {
                    hasDiscontinuity = true;
                } else if (line.startsWith(M3U8Constants.TAG_ENDLIST)) {
                    hasEndList = true;
                } else if (line.startsWith(M3U8Constants.TAG_KEY)) {
                    hasKey = true;
                    method = M3U8Utils.parseOptionalStringAttr(line, M3U8Constants.REGEX_METHOD);
                    String keyFormat = M3U8Utils.parseOptionalStringAttr(line, M3U8Constants.REGEX_KEYFORMAT);
                    if (!M3U8Constants.METHOD_NONE.equals(method)) {
                        encryptionIV = M3U8Utils.parseOptionalStringAttr(line, M3U8Constants.REGEX_IV);
                        if (M3U8Constants.KEYFORMAT_IDENTITY.equals(keyFormat) || keyFormat == null) {
                            if (M3U8Constants.METHOD_AES_128.equals(method)) {
                                // The segment is fully encrypted using an identity key.
                                String tempKeyUri = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_URI);
                                if (tempKeyUri != null) {
                                    if (XDownUtils.isEmpty(baseUrl) || !baseUrl.startsWith("http")) {
                                        tempWaitKeyUri = tempKeyUri;
                                    }
                                    encryptionKeyUri = M3U8Utils.getM3U8AbsoluteUrl(baseUrl, tempKeyUri);
                                }
                            } else {
                                // Do nothing. Samples are encrypted using an identity key,
                                // but this is not supported. Hopefully, a traditional DRM
                                // alternative is also provided.
                            }
                        } else {
                            // Do nothing.
                        }
                    }
                } else if (line.startsWith(M3U8Constants.TAG_INIT_SEGMENT)) {
                    String tempInitSegmentUri = M3U8Utils.parseStringAttr(line, M3U8Constants.REGEX_URI);
                    if (!XDownUtils.isEmpty(tempInitSegmentUri)) {
                        hasInitSegment = true;
                        initSegmentUri = M3U8Utils.getM3U8AbsoluteUrl(baseUrl, tempInitSegmentUri);
                        segmentByteRange = M3U8Utils.parseOptionalStringAttr(line, M3U8Constants.REGEX_ATTR_BYTERANGE);
                    }
                }
                continue;
            }
            // It has '#EXT-X-STREAM-INF' tag;
            if (hasStreamInfo) {
                m3U8Info.setNeedRedirect(true);
                m3U8Info.setUrl(M3U8Utils.getM3U8AbsoluteUrl(baseUrl, line));
                return m3U8Info;
            }
            if (Math.abs(tsDuration) < 0.001f) {
                continue;
            }
            M3U8Ts ts = new M3U8Ts();

            if (tempWaitKeyUri != null && (XDownUtils.isEmpty(encryptionKeyUri) || !encryptionKeyUri.startsWith("http"))) {
                if (line.startsWith("http")) {
                    encryptionKeyUri = M3U8Utils.getM3U8AbsoluteUrl(getHostUrl(line), tempWaitKeyUri);
                }
            }
            ts.initTsAttributes(M3U8Utils.getM3U8AbsoluteUrl(baseUrl, line), tsDuration, tsIndex, sequence++, hasDiscontinuity);
            if (hasKey) {
                ts.setKeyConfig(method, encryptionKeyUri, encryptionIV);
            }
            if (hasInitSegment) {
                ts.setInitSegmentInfo(initSegmentUri, segmentByteRange);
            }
            m3U8Info.addTs(ts);
            tsIndex++;
            tsDuration = 0;
            hasStreamInfo = false;
            hasDiscontinuity = false;
            hasKey = false;
            hasInitSegment = false;
            method = null;
            tempWaitKeyUri = null;
            encryptionKeyUri = null;
            encryptionIV = null;
            initSegmentUri = null;
            segmentByteRange = null;
        }
        m3U8Info.setTargetDuration(targetDuration);
        m3U8Info.setVersion(version);
        m3U8Info.setSequence(sequence);
        m3U8Info.setHasEndList(hasEndList);
        return m3U8Info;
    }

    public static void createLocalM3U8File(File saveFile, File tempDir, M3U8Info info) throws IOException {
        BufferedWriter bfw = null;
        try {
            bfw = new BufferedWriter(new FileWriter(saveFile, false));
            bfw.write(M3U8Constants.PLAYLIST_HEADER + "\n");
            bfw.write(M3U8Constants.TAG_VERSION + ":" + info.getVersion() + "\n");
            bfw.write(M3U8Constants.TAG_MEDIA_SEQUENCE + ":" + info.getInitSequence() + "\n");
            bfw.write(M3U8Constants.TAG_TARGET_DURATION + ":" + info.getTargetDuration() + "\n");

            for (M3U8Ts m3u8Ts : info.getTsList()) {
                if (m3u8Ts.hasKey()) {
                    if (!XDownUtils.isEmpty(m3u8Ts.getMethod())) {
                        String key = "METHOD=" + m3u8Ts.getMethod();
                        if (!XDownUtils.isEmpty(m3u8Ts.getKeyUri())) {
                            File keyFile = m3u8Ts.getKeyFile(tempDir);
                            if (keyFile.exists()) {
                                key += ",URI=\"" + keyFile.getAbsolutePath() + "\"";
                            } else {
                                key += ",URI=\"" + m3u8Ts.getKeyUri() + "\"";
                            }
                            if (!XDownUtils.isEmpty(m3u8Ts.getKeyIV())) {
                                key += ",IV=" + m3u8Ts.getKeyIV();
                            }
                        }
                        bfw.write(M3U8Constants.TAG_KEY + ":" + key + "\n");
                    }
                }
                if (m3u8Ts.hasInitSegment()) {
                    String initSegmentInfo;
                    String initSegmentFilePath = m3u8Ts.getInitSegmentFile(tempDir).getAbsolutePath();

                    if (!XDownUtils.isEmpty(m3u8Ts.getSegmentByteRange())) {
                        initSegmentInfo = "URI=\"" + initSegmentFilePath + "\"" + ",BYTERANGE=\"" + m3u8Ts.getSegmentByteRange() + "\"";
                    } else {
                        initSegmentInfo = "URI=\"" + initSegmentFilePath + "\"";
                    }
                    bfw.write(M3U8Constants.TAG_INIT_SEGMENT + ":" + initSegmentInfo + "\n");
                }
                if (m3u8Ts.hasDiscontinuity()) {
                    bfw.write(M3U8Constants.TAG_DISCONTINUITY + "\n");
                }
                bfw.write(M3U8Constants.TAG_MEDIA_DURATION + ":" + m3u8Ts.getDuration() + ",\n");
                bfw.write(m3u8Ts.getTsFile(tempDir).getAbsolutePath());
                bfw.newLine();
            }
            bfw.write(M3U8Constants.TAG_ENDLIST);
            bfw.flush();
        } finally {
            XDownUtils.closeIo(bfw);
        }
    }

    public static M3U8Info parseLocalM3u8File(File m3u8File) throws Exception {
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            inputStreamReader = new InputStreamReader(new FileInputStream(m3u8File));
            bufferedReader = new BufferedReader(inputStreamReader);
            M3U8Info m3U8Info = new M3U8Info();
            float tsDuration = 0;
            int targetDuration = 0;
            int tsIndex = 0;
            int version = 0;
            int sequence = 0;
            boolean hasDiscontinuity = false;
            boolean hasKey = false;
            boolean hasInitSegment = false;
            String method = null;
            String encryptionIV = null;
            String encryptionKeyUri = null;
            String initSegmentUri = null;
            String segmentByteRange = null;
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(M3U8Constants.TAG_PREFIX)) {
                    if (line.startsWith(M3U8Constants.TAG_MEDIA_DURATION)) {
                        String ret = parseStringAttr(line, M3U8Constants.REGEX_MEDIA_DURATION);
                        if (!XDownUtils.isEmpty(ret)) {
                            tsDuration = Float.parseFloat(ret);
                        }
                    } else if (line.startsWith(M3U8Constants.TAG_TARGET_DURATION)) {
                        String ret = parseStringAttr(line, M3U8Constants.REGEX_TARGET_DURATION);
                        if (!XDownUtils.isEmpty(ret)) {
                            targetDuration = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(M3U8Constants.TAG_VERSION)) {
                        String ret = parseStringAttr(line, M3U8Constants.REGEX_VERSION);
                        if (!XDownUtils.isEmpty(ret)) {
                            version = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(M3U8Constants.TAG_MEDIA_SEQUENCE)) {
                        String ret = parseStringAttr(line, M3U8Constants.REGEX_MEDIA_SEQUENCE);
                        if (!XDownUtils.isEmpty(ret)) {
                            sequence = Integer.parseInt(ret);
                        }
                    } else if (line.startsWith(M3U8Constants.TAG_DISCONTINUITY)) {
                        hasDiscontinuity = true;
                    } else if (line.startsWith(M3U8Constants.TAG_KEY)) {
                        hasKey = true;
                        method = parseOptionalStringAttr(line, M3U8Constants.REGEX_METHOD);
                        String keyFormat = parseOptionalStringAttr(line, M3U8Constants.REGEX_KEYFORMAT);
                        if (!M3U8Constants.METHOD_NONE.equals(method)) {
                            encryptionIV = parseOptionalStringAttr(line, M3U8Constants.REGEX_IV);
                            if (M3U8Constants.KEYFORMAT_IDENTITY.equals(keyFormat) || keyFormat == null) {
                                if (M3U8Constants.METHOD_AES_128.equals(method)) {
                                    // The segment is fully encrypted using an identity key.
                                    encryptionKeyUri = parseStringAttr(line, M3U8Constants.REGEX_URI);
                                } else {
                                    // Do nothing. Samples are encrypted using an identity key,
                                    // but this is not supported. Hopefully, a traditional DRM
                                    // alternative is also provided.
                                }
                            } else {
                                // Do nothing.
                            }
                        }
                    } else if (line.startsWith(M3U8Constants.TAG_INIT_SEGMENT)) {
                        initSegmentUri = parseStringAttr(line, M3U8Constants.REGEX_URI);
                        if (!XDownUtils.isEmpty(initSegmentUri)) {
                            hasInitSegment = true;
                            segmentByteRange = parseOptionalStringAttr(line, M3U8Constants.REGEX_ATTR_BYTERANGE);
                        }
                    }
                    continue;
                }
                M3U8Ts ts = new M3U8Ts();
                ts.initTsAttributes(line, tsDuration, tsIndex, sequence++, hasDiscontinuity);
                if (hasKey) {
                    ts.setKeyConfig(method, encryptionKeyUri, encryptionIV);
                }
                if (hasInitSegment) {
                    ts.setInitSegmentInfo(initSegmentUri, segmentByteRange);
                }
                m3U8Info.addTs(ts);
                tsIndex++;
                tsDuration = 0;
                hasDiscontinuity = false;
                hasKey = false;
                hasInitSegment = false;
                method = null;
                encryptionKeyUri = null;
                encryptionIV = null;
                initSegmentUri = null;
                segmentByteRange = null;
            }
            m3U8Info.setTargetDuration(targetDuration);
            m3U8Info.setVersion(version);
            m3U8Info.setSequence(sequence);
            return m3U8Info;
        } finally {
            XDownUtils.closeIo(inputStreamReader);
            XDownUtils.closeIo(bufferedReader);
        }
    }

    public static String parseStringAttr(String line, Pattern pattern) {
        if (pattern == null)
            return null;
        Matcher matcher = pattern.matcher(line);
        if (matcher.find() && matcher.groupCount() == 1) {
            return matcher.group(1);
        }
        return null;
    }

    public static String parseOptionalStringAttr(String line, Pattern pattern) {
        if (pattern == null)
            return null;
        Matcher matcher = pattern.matcher(line);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static void createNetM3U8(File m3u8File, M3U8Info m3U8Info) throws IOException {
        BufferedWriter bfw = null;
        try {
            bfw = new BufferedWriter(new FileWriter(m3u8File, false));
            bfw.write(M3U8Constants.PLAYLIST_HEADER + "\n");
            bfw.write(M3U8Constants.TAG_VERSION + ":" + m3U8Info.getVersion() + "\n");
            bfw.write(M3U8Constants.TAG_MEDIA_SEQUENCE + ":" + m3U8Info.getInitSequence() + "\n");
            bfw.write(M3U8Constants.TAG_TARGET_DURATION + ":" + m3U8Info.getTargetDuration() + "\n");
            for (M3U8Ts m3u8Ts : m3U8Info.getTsList()) {
                if (m3u8Ts.hasInitSegment()) {
                    String initSegmentInfo;
                    if (!XDownUtils.isEmpty(m3u8Ts.getSegmentByteRange())) {
                        initSegmentInfo = "URI=\"" + m3u8Ts.getInitSegmentUri() + "\"" + ",BYTERANGE=\"" + m3u8Ts.getSegmentByteRange() + "\"";
                    } else {
                        initSegmentInfo = "URI=\"" + m3u8Ts.getInitSegmentUri() + "\"";
                    }
                    bfw.write(M3U8Constants.TAG_INIT_SEGMENT + ":" + initSegmentInfo + "\n");
                }
                if (m3u8Ts.hasKey()) {
                    if (!XDownUtils.isEmpty(m3u8Ts.getMethod())) {
                        try {
                            String key = "METHOD=" + m3u8Ts.getMethod();
                            if (!XDownUtils.isEmpty(m3u8Ts.getKeyUri())) {
                                String keyUri = m3u8Ts.getKeyUri();
                                key += ",URI=\"" + keyUri + "\"";
                                if (!XDownUtils.isEmpty(m3u8Ts.getKeyIV())) {
                                    key += ",IV=" + m3u8Ts.getKeyIV();
                                }
                            }
                            bfw.write(M3U8Constants.TAG_KEY + ":" + key + "\n");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (m3u8Ts.hasDiscontinuity()) {
                    bfw.write(M3U8Constants.TAG_DISCONTINUITY + "\n");
                }
                bfw.write(M3U8Constants.TAG_MEDIA_DURATION + ":" + m3u8Ts.getDuration() + ",\n");
                bfw.write(m3u8Ts.getUrl());
                bfw.newLine();
            }
            bfw.write(M3U8Constants.TAG_ENDLIST);
            bfw.flush();
        } finally {
            XDownUtils.closeIo(bfw);
        }

    }

    public static String getM3U8AbsoluteUrl(String videoUrl, final String line) {
        if (XDownUtils.isEmpty(line)) {
            return "";
        }
        if (line.startsWith("http")) {
            return line;
        }
        if (XDownUtils.isEmpty(videoUrl)) {
            return "";
        }
        if (videoUrl.startsWith("file://") || videoUrl.startsWith("/")) {
            //如果为本地的路径
            return videoUrl;
        }
        String baseUriPath = getBaseUrl(videoUrl);
        String hostUrl = getHostUrl(videoUrl);
        if (line.startsWith("//")) {
            return getSchema(videoUrl) + ":" + line;
        }
        if (line.startsWith("/")) {
            String pathStr = getPathStr(videoUrl);
            String longestCommonPrefixStr = getLongestCommonPrefixStr(pathStr, line);
            if (hostUrl.endsWith("/")) {
                hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
            }
            return hostUrl + longestCommonPrefixStr +
                    line.substring(longestCommonPrefixStr.length());
        }

        return baseUriPath + line;
    }

    private static String getSchema(String url) {
        if (XDownUtils.isEmpty(url)) {
            return "";
        }
        int index = url.indexOf("://");
        if (index != -1) {
            return url.substring(0, index);
        }
        return "";
    }

    /**
     * @param url
     * @return
     */
    public static String getBaseUrl(String url) {
        if (XDownUtils.isEmpty(url)) {
            return "";
        }
        int slashIndex = url.lastIndexOf("/");
        if (slashIndex != -1) {
            return url.substring(0, slashIndex + 1);
        }
        return url;
    }

    /**
     * @param url
     * @return
     */
    public static String getHostUrl(String url) {
        if (XDownUtils.isEmpty(url)) {
            return "";
        }
        try {
            URL formatURL = new URL(url);
            String host = formatURL.getHost();
            if (host == null) {
                return url;
            }
            int hostIndex = url.indexOf(host);
            if (hostIndex != -1) {
                int port = formatURL.getPort();
                String resultUrl;
                String substring = url.substring(0, hostIndex + host.length());
                if (port != -1) {
                    resultUrl = substring + ":" + port + "/";
                } else {
                    resultUrl = substring + "/";
                }
                return resultUrl;
            }
            return url;
        } catch (Exception e) {
            return url;
        }
    }

    /**
     * @param url
     * @return
     */
    public static String getPathStr(String url) {
        if (XDownUtils.isEmpty(url)) {
            return "";
        }
        String hostUrl = getHostUrl(url);
        if (XDownUtils.isEmpty(hostUrl)) {
            return url;
        }
        return url.substring(hostUrl.length() - 1);
    }

    /**
     * 获取两个字符串的最长公共前缀
     *
     * @param str1
     * @param str2
     * @return
     */
    public static String getLongestCommonPrefixStr(String str1, String str2) {
        if (XDownUtils.isEmpty(str1) || XDownUtils.isEmpty(str2)) {
            return "";
        }
        if (str1.equals(str2)) {
            return str1;
        }
        char[] arr1 = str1.toCharArray();
        char[] arr2 = str2.toCharArray();
        int j = 0;
        while (j < arr1.length && j < arr2.length) {
            if (arr1[j] != arr2[j]) {
                break;
            }
            j++;
        }
        return str1.substring(0, j);
    }

}

