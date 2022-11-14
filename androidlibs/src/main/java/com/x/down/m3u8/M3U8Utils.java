package com.x.down.m3u8;

import android.text.TextUtils;

import com.x.down.core.XDownloadRequest;
import com.x.down.listener.OnMergeFileListener;
import com.x.down.tool.XDownUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U8Utils {

    public static void mergeM3u8(XDownloadRequest request, File saveFile, M3U8Info info) throws Exception {
        File tempDir = XDownUtils.getTempCacheDir(request,true);
        File file = new File(saveFile.getParentFile(), request.getSaveName().replace(".m3u8", ""));
        tempDir.renameTo(file);
        createLocalM3U8File(saveFile, file, info);
        OnMergeFileListener listener = request.getOnMegerFileListener();
        if (listener != null) {
            List<File> tsList = new ArrayList<>();
            for (M3U8Ts m3u8Ts : info.getTsList()) {
                if (m3u8Ts.hasInitSegment()) {
                    tsList.add(new File(file, m3u8Ts.getInitSegmentName()));
                }
                if (m3u8Ts.hasKey()) {
                    if (m3u8Ts.getMethod() != null) {
                        if (m3u8Ts.getKeyUri() != null) {
                            File keyFile = new File(file, m3u8Ts.getLocalKeyUri());
                            if (!m3u8Ts.isMessyKey() && keyFile.exists()) {
                                tsList.add(keyFile);
                            }
                        }
                    }
                }
                tsList.add(new File(file, m3u8Ts.getIndexName()));
            }
            listener.onM3u8Merge(saveFile, tsList);
        }
    }

    public static void createLocalM3U8File(File saveFile, File tempDir, M3U8Info info) throws IOException {
        synchronized (Object.class) {
            if (saveFile.exists()) {
                saveFile.delete();
            }
            BufferedWriter bfw = new BufferedWriter(new FileWriter(saveFile, false));
            bfw.write(M3U8Constants.PLAYLIST_HEADER + "\n");
            bfw.write(M3U8Constants.TAG_VERSION + ":" + info.getVersion() + "\n");
            bfw.write(M3U8Constants.TAG_MEDIA_SEQUENCE + ":" + info.getInitSequence() + "\n");

            bfw.write(M3U8Constants.TAG_TARGET_DURATION + ":" + info.getTargetDuration() + "\n");

            for (M3U8Ts m3u8Ts : info.getTsList()) {
                if (m3u8Ts.hasInitSegment()) {
                    String initSegmentInfo;
                    String initSegmentFilePath = new File(tempDir, m3u8Ts.getInitSegmentName()).getAbsolutePath();
                    if (m3u8Ts.getSegmentByteRange() != null) {
                        initSegmentInfo = "URI=\"" + initSegmentFilePath + "\"" + ",BYTERANGE=\"" + m3u8Ts.getSegmentByteRange() + "\"";
                    } else {
                        initSegmentInfo = "URI=\"" + initSegmentFilePath + "\"";
                    }
                    bfw.write(M3U8Constants.TAG_INIT_SEGMENT + ":" + initSegmentInfo + "\n");
                }
                if (m3u8Ts.hasKey()) {
                    if (m3u8Ts.getMethod() != null) {
                        String key = "METHOD=" + m3u8Ts.getMethod();
                        if (m3u8Ts.getKeyUri() != null) {
                            File keyFile = new File(tempDir, m3u8Ts.getLocalKeyUri());
                            if (!m3u8Ts.isMessyKey() && keyFile.exists()) {
                                key += ",URI=\"" + keyFile.getAbsolutePath() + "\"";
                            } else {
                                key += ",URI=\"" + m3u8Ts.getKeyUri() + "\"";
                            }
                        }
                        if (m3u8Ts.getKeyIV() != null) {
                            key += ",IV=" + m3u8Ts.getKeyIV();
                        }
                        bfw.write(M3U8Constants.TAG_KEY + ":" + key + "\n");
                    }
                }
                if (m3u8Ts.hasDiscontinuity()) {
                    bfw.write(M3U8Constants.TAG_DISCONTINUITY + "\n");
                }
                bfw.write(M3U8Constants.TAG_MEDIA_DURATION + ":" + m3u8Ts.getDuration() + ",\n");
                bfw.write(new File(tempDir, m3u8Ts.getIndexName()).getAbsolutePath());
                bfw.newLine();
            }
            bfw.write(M3U8Constants.TAG_ENDLIST);
            bfw.flush();
            bfw.close();
        }
    }

    public static M3U8Info parseLocalM3U8File(File m3u8File) throws Exception {
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

    public static void createNetM3U8(File m3u8File, File tempDir, M3U8Info m3U8Info) throws IOException {
        BufferedWriter bfw = new BufferedWriter(new FileWriter(m3u8File, false));
        bfw.write(M3U8Constants.PLAYLIST_HEADER + "\n");
        bfw.write(M3U8Constants.TAG_VERSION + ":" + m3U8Info.getVersion() + "\n");
        bfw.write(M3U8Constants.TAG_MEDIA_SEQUENCE + ":" + m3U8Info.getInitSequence() + "\n");
        bfw.write(M3U8Constants.TAG_TARGET_DURATION + ":" + m3U8Info.getTargetDuration() + "\n");
        for (M3U8Ts m3u8Ts : m3U8Info.getTsList()) {
            if (m3u8Ts.hasInitSegment()) {
                String initSegmentInfo;
                if (m3u8Ts.getSegmentByteRange() != null) {
                    initSegmentInfo = "URI=\"" + m3u8Ts.getInitSegmentUri() + "\"" + ",BYTERANGE=\"" + m3u8Ts.getSegmentByteRange() + "\"";
                } else {
                    initSegmentInfo = "URI=\"" + m3u8Ts.getInitSegmentUri() + "\"";
                }
                bfw.write(M3U8Constants.TAG_INIT_SEGMENT + ":" + initSegmentInfo + "\n");
            }
            if (m3u8Ts.hasKey()) {
                if (m3u8Ts.getMethod() != null) {
                    try {
                        String key = "METHOD=" + m3u8Ts.getMethod();
                        if (m3u8Ts.getKeyUri() != null) {
                            String keyUri = m3u8Ts.getKeyUri();
                            key += ",URI=\"" + keyUri + "\"";
                            URL keyURL = new URL(keyUri);
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(keyURL.openStream()));
                            StringBuilder textBuilder = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                textBuilder.append(line);
                            }
                            boolean isMessyStr = XDownUtils.isMessyCode(textBuilder.toString());
                            m3u8Ts.setIsMessyKey(isMessyStr);

                            File keyFile = new File(tempDir, m3u8Ts.getLocalKeyUri());
                            FileOutputStream outputStream = new FileOutputStream(keyFile);
                            outputStream.write(textBuilder.toString().getBytes());
                            bufferedReader.close();
                            outputStream.close();
                            if (m3u8Ts.getKeyIV() != null) {
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
        bfw.close();
    }

    public static String getM3U8AbsoluteUrl(String videoUrl, String line) {
        if (XDownUtils.isEmpty(videoUrl) || XDownUtils.isEmpty(line)) {
            return "";
        }
        if (videoUrl.startsWith("file://") || videoUrl.startsWith("/")) {
            return videoUrl;
        }
        String baseUriPath = getBaseUrl(videoUrl);
        String hostUrl = getHostUrl(videoUrl);
        if (line.startsWith("//")) {
            String tempUrl = getSchema(videoUrl) + ":" + line;
            return tempUrl;
        }
        if (line.startsWith("/")) {
            String pathStr = getPathStr(videoUrl);
            String longestCommonPrefixStr = getLongestCommonPrefixStr(pathStr, line);
            if (hostUrl.endsWith("/")) {
                hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
            }
            String tempUrl = hostUrl + longestCommonPrefixStr + line.substring(longestCommonPrefixStr.length());
            return tempUrl;
        }
        if (line.startsWith("http")) {
            return line;
        }
        return baseUriPath + line;
    }

    private static String getSchema(String url) {
        if (XDownUtils.isEmpty(url)) {
            return "";
        }
        int index = url.indexOf("://");
        if (index != -1) {
            String result = url.substring(0, index);
            return result;
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
        if (TextUtils.equals(str1, str2)) {
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

