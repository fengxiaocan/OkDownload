package com.x.down.tool;

import com.x.down.core.XDownloadRequest;
import com.x.down.impl.UnSafeTrustManager;

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

    public static void sleep(long ms) {
        long start = System.currentTimeMillis();
        long duration = ms;
        boolean interrupted = false;
        do {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                interrupted = true;
            }
            duration = start + ms - System.currentTimeMillis();
        } while (duration > 0);

        if (interrupted) {
            Thread.currentThread().interrupt();
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

    public static void disconnectHttp(HttpURLConnection connection) {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
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

    public static boolean writeFile(File file, byte[] arrays) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file, false);
            stream.write(arrays);
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
    public static File getTempCacheDir(XDownloadRequest request) {
        //保存路径
        String saveDir = request.getCacheDir();
        //获取MD5
        String md5 = request.getIdentifier();
        File dir = new File(saveDir, md5 + "_temp");
        dir.mkdirs();
        return dir;
    }

    /**
     * 获得临时文件的文件名
     *
     * @return
     */
    public static File getTempFile(XDownloadRequest request) {
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

    public static long getContentLength(HttpURLConnection http) {
        try {
            return Long.parseLong(http.getHeaderField("Content-Length"));
        } catch (Exception e) {
        }
        return 0;
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

    public static String getSuffixName(String name) {
        if (isEmpty(name)) {
            return "";
        }
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < name.length()) ? name.substring(dotIndex) : "";
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
        if (chLength <= 0)
            return false;
        float result = count / chLength;
        return result > 0.4;
    }

    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                ub == Character.UnicodeBlock.GENERAL_PUNCTUATION ||
                ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }
}
