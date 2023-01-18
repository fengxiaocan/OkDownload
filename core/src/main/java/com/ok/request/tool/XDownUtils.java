package com.ok.request.tool;

import com.ok.request.config.Config;
import com.ok.request.core.OkDownloadRequest;
import com.ok.request.params.Headers;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class XDownUtils {
    private static String getMd5(byte[] btInput) {
        char[] hexDigits = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

        try {
            //获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            //使用指定的字节更新摘要
            mdInst.update(btInput);
            //获得密文
            byte[] md = mdInst.digest();
            //把密文转换成十六进制的字符串形式
            int j = md.length;
            char[] str = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getMd5(String msg) {
        byte[] bytes = msg.getBytes();
        return getMd5(bytes);
    }

    public static void closeIo(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getInputCharset(HttpURLConnection connection) {
        if (connection != null) {
            String type = connection.getHeaderField("Content-Type");
            if (type != null) {
                if (type.contains("charset=")) {
                    String substring = type.substring(type.indexOf("charset=") + "charset=".length());
                    int i = substring.indexOf(";");
                    if (i > 0) {
                        substring = substring.substring(0, i);
                    }
                    return substring;
                }
            }
        }
        return "utf-8";
    }


    /**
     * 是否支持断点下载
     *
     * @return
     */
    public static boolean isAcceptRanges(Headers headers) {
        String field = headers.getValue("Accept-Ranges");
        if (!XDownUtils.isEmpty(field)) {
            return !field.contains("none");
        }
        return true;
    }


    public static void disconnectHttp(HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 获取URL后缀名称
     *
     * @param url
     * @return
     */
    public static String getUrlName(final String url) {
        if (url == null || "".equals(url)) {
            return "";
        }
        final int index1 = url.indexOf("?");
        final String subUrl;
        if (index1 > 0) {
            subUrl = url.substring(0, index1);
        } else {
            subUrl = url;
        }
        int index2 = subUrl.lastIndexOf("/");
        if (index2 > 0) {
            return subUrl.substring(index2 + 1);
        } else {
            final int index = url.lastIndexOf(".");
            if (index > 0) {
                String matches = getFirstMatches("\\.[A-Za-z0-9]*", url.substring(index));
                if (matches == null) {
                    return getMd5(url) + ".unknown";
                } else {
                    return getMd5(url) + matches;
                }
            } else {
                return getMd5(url) + ".unknown";
            }
        }
    }

    public static boolean writeObject(File file, Object obj) {
        ObjectOutputStream stream = null;
        try {
            stream = new ObjectOutputStream(new FileOutputStream(file, false));
            stream.writeObject(obj);
            stream.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            XDownUtils.closeIo(stream);
        }
    }

    public static <T> T readObject(File file) {
        if (!file.exists()) {
            return null;
        }
        ObjectInputStream stream = null;
        try {
            stream = new ObjectInputStream(new FileInputStream(file));
            Object object = stream.readObject();
            return (T) object;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            XDownUtils.closeIo(stream);
        }
    }

    /**
     * 保存临时文件的文件夹
     *
     * @return
     */
    public static File getTempCacheDir(OkDownloadRequest request) {
        //保存路径
        String cacheDir = request.getCacheDir();
        //获取MD5
        String md5 = request.getIdentifier();
        return new File(cacheDir, md5 + "_temp");
    }

    /**
     * 保存临时文件的文件夹
     *
     * @return
     */
    public static File getTempCacheDir2(OkDownloadRequest request) {
        File cacheDir = getTempCacheDir(request);
        cacheDir.mkdirs();
        return cacheDir;
    }

    /**
     * 保存临时文件的文件夹
     *
     * @return
     */
    public static File getRecordCacheFile(OkDownloadRequest request) {
        String md5 = request.getIdentifier();
        //日志记录
        String recordDir = Config.config().getRecordDir();
        File file = new File(recordDir, md5);
        file.getParentFile().mkdirs();
        return file;
    }

    /**
     * 获得临时文件的文件名
     *
     * @return
     */
    public static File getTempFile(OkDownloadRequest request) {
        //没有设置保存文件名
        return new File(getTempCacheDir(request), request.getSaveName());
    }

    public static void deleteDir(File dir) {
        if (dir == null) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file1 : files) {
                if (file1.isDirectory()) {
                    deleteDir(file1);
                }
                file1.delete();
            }
        }
        dir.delete();
    }

    /**
     * 获取正则匹配的第一个
     *
     * @param regex
     * @param input
     * @return
     */
    private static String getFirstMatches(String regex, String input) {
        if (input == null) {
            return null;
        }
        String matches = null;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            matches = matcher.group();
        }
        return matches;
    }

    /**
     * 获取安全证书
     *
     * @param cerPath
     * @return
     */
    public static SSLSocketFactory getCertificate(String cerPath) {
        InputStream is = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            is = new FileInputStream(cerPath);
            Certificate ca = cf.generateCertificate(is);

            // Create a KeyStore containing our trusted CAs
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String defaultAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(defaultAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);//信任证书密钥
//            sslContext.init(null, new TrustManager[]{new UnSafeTrustManager()}, null);//信任所有
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeIo(is);
        }
        return null;
    }

    /**
     * 不安全的信任管理
     *
     * @return
     */
    public static SSLSocketFactory getUnSafeCertificate() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new UnSafeTrustManager()}, null);//信任所有
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String jsonString(Object... array) {
        StringBuilder builder = new StringBuilder();
        for (Object o : array) {
            builder.append(o);
        }
        return builder.toString();
    }

    public static boolean isEmpty(CharSequence sequence) {
        return sequence == null || "".equals(sequence);
    }

    /**
     * 获取后缀名字
     *
     * @param name
     * @return
     */
    public static String getSuffixName(String name) {
        if (isEmpty(name)) {
            return "";
        }
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < name.length()) ? name.substring(dotIndex) : "";
    }

    /**
     * 获取文件已经存在的大小
     *
     * @param file
     * @param contentLength
     * @return
     */
    public static long getFileExistsLength(File file, long contentLength) {
        if (file.exists()) {
            if (contentLength > 0) {
                if (file.length() == contentLength) {
                    return contentLength;
                } else if (file.length() > contentLength) {
                    file.deleteOnExit();
                    return 0;
                }
            }
            return file.length();
        }
        return 0;
    }

    public static boolean isMessyCode(String strName) {
        Pattern p = Pattern.compile("\\s*|t*|r*|n*");
        Matcher m = p.matcher(strName);
        String after = m.replaceAll("");
        String temp = after.replaceAll("\\p{P}", "");
        char[] ch = temp.trim().toCharArray();
        float chLength = ch.length;
        float count = 0;
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (!Character.isLetterOrDigit(c)) {
                if (!isChinese(c)) {
                    count = count + 1;
                }
            }
        }
        if (chLength <= 0) return false;
        float result = count / chLength;
        return result > 0.4;
    }

    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
