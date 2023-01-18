package com.ok.request.call;

import com.ok.request.base.RequestBody;
import com.ok.request.params.Headers;
import com.ok.request.params.HttpIoSink;
import com.ok.request.params.MediaType;
import com.ok.request.request.HttpResponseBody;
import com.ok.request.request.Request;
import com.ok.request.request.Response;
import com.ok.request.request.URLResponse;
import com.ok.request.tool.XDownUtils;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

public class HttpConnection implements Connection {
    private Request request;
    private HttpURLConnection connection;

    @Override
    public void request(Request request) throws Exception {
        this.request = request;
        this.connection = (HttpURLConnection) request.url().openConnection();

        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection https = (HttpsURLConnection) connection;
            //处理https证书
            SSLSocketFactory certificate = request.certificate();
            if (certificate != null) {
                https.setSSLSocketFactory(certificate);
            }
        }
        int timeout = Math.max(request.TimeOut(), 1000);
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        //本次链接是否处理重定向
        connection.setInstanceFollowRedirects(true);
        connection.setDoInput(true);
        connection.setDoOutput(request.method().equalsIgnoreCase("post"));
        //设置请求方式
        connection.setRequestMethod(request.method());
        //设置http请求头
        connection.setRequestProperty("Connection", "Keep-Alive");
        Headers headers = request.headers();
        if (headers != null) {
            for (String key : headers.keySet()) {
                connection.setRequestProperty(key, headers.getValue(key));
            }
        }

        RequestBody body = request.body();
        if (body != null) {
            MediaType mediaType = body.contentType();
            if (mediaType.getType() != null) {
                connection.setRequestProperty("Content-Type", mediaType.getType());
            }
            if (body.contentLength() != -1) {
                connection.setRequestProperty("Content-Length", String.valueOf(body.contentLength()));
            }
            HttpIoSink ioSink = new HttpIoSink(connection.getOutputStream());
            body.writeTo(ioSink);
        }
    }

    @Override
    public Request responseRedirects() throws Exception {
        //是否需要重定向
        HttpURLConnection url = connection;
        int responseCode = url.getResponseCode();
        if (isNeedRedirects(responseCode)) {

            //获取指向
            String location = url.getHeaderField("Location");
            //获取重定向地址
            String redirectsUrl = getRedirectsUrl(url.getURL(), location);
            //断开原来的连接
            XDownUtils.disconnectHttp(url);

            Request newRequest = request.clone(redirectsUrl);

            Headers headers = new Headers(url.getHeaderFields());
            headers.removeHeader("Location");
            newRequest.addHeader(headers);

            if (responseCode == 307) {
                newRequest.method("GET");
            }
            return newRequest;
        }
        return null;
    }

    @Override
    public Response response() throws Exception {
        final int responseCode = connection.getResponseCode();
        String charset = XDownUtils.getInputCharset(connection);
        Headers headers = new Headers(connection.getHeaderFields());
        MediaType mediaType = MediaType.parse(connection.getContentType());
        final long date = connection.getDate();
        final long lastModified = connection.getLastModified();
        String encoding = connection.getContentEncoding();

        long contentLength = 0;
        String value = headers.getValue("Content-Length");

        try {
            contentLength = Long.parseLong(value);
        } catch (Exception e) {
        }
        HttpResponseBody responseBody;
        if (responseCode >= 200 && responseCode < 400) {
            responseBody = new HttpResponseBody(connection.getInputStream(), contentLength, encoding, charset, mediaType);
        } else {
            responseBody = new HttpResponseBody(connection.getErrorStream(), contentLength, encoding, charset, mediaType);
        }
        return new URLResponse(request, responseBody, responseCode, headers, date, lastModified);
    }

    @Override
    public void terminated() {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 获取重定向的真实地址
     *
     * @param url
     * @param location
     * @return
     */
    private String getRedirectsUrl(URL url, String location) {
        String redirectsUrl;
        if (location.startsWith("http")) {
            redirectsUrl = location;
        } else {
            StringBuilder builder = new StringBuilder(url.getProtocol());
            builder.append("://");
            builder.append(url.getHost());

            if (location.startsWith("/")) {
                builder.append(location);

                String query = url.getQuery();
                if (query != null) {
                    if (location.indexOf("?") > 0) {
                        builder.append("&");
                    } else {
                        builder.append("?");
                    }
                    builder.append(query);
                }
            } else {
                String urlPath = url.getPath();

                if (urlPath != null) {
                    int index = urlPath.lastIndexOf("/");
                    if (index > 0) {
                        String substring = urlPath.substring(0, index + 1);
                        builder.append(substring);
                    } else {
                        builder.append(urlPath);
                    }
                    builder.append(location);
                }

                String query = url.getQuery();
                if (query != null) {
                    if (location.indexOf("?") > 0) {
                        builder.append("&");
                    } else {
                        builder.append("?");
                    }
                    builder.append(query);
                }
            }
            redirectsUrl = builder.toString();
        }
        return redirectsUrl;
    }

    private boolean isNeedRedirects(int code) {
        switch (code) {
            case 301:
                //客户请求的文档在其他地方，新的URL在Location头中给出，浏览器应该自动地访问新的URL。
            case 302:
                //类似于301，但新的URL应该被视为临时性的替代，而不是永久性的。注意，在HTTP1.0中对应的状态信息是“Moved Temporatily”。
                //出现该状态代码时，浏览器能够自动访问新的URL，因此它是一个很有用的状态代码。
            case 303:
                //类似于301/302，不同之处在于，如果原来的请求是POST，Location头指定的重定向目标文档应该通过GET提取（HTTP 1.1新）。
            case 305:
                //客户请求的文档应该通过Location头所指明的代理服务器提取（HTTP 1.1新）。
            case 307:
                //和 302（Found）相同。许多浏览器会错误地响应302应答进行重定向，即使原来的请求是POST，即使它实际上只能在POST请求的应答是303时才 能重定向。由于这个原因，HTTP 1.1新增了307，以便更加清除地区分几个状态代码：当出现303应答时，浏览器可以跟随重定向的GET和POST请求；如果是307应答，则浏览器只 能跟随对GET请求的重定向。（HTTP 1.1新）
                return true;
            default:
                return false;
        }
    }
}
