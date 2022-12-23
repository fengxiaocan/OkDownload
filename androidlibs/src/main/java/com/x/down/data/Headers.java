package com.x.down.data;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Headers {
    private final Map<String, List<String>> headerMap;

    public Headers() {
        headerMap = new HashMap<>();
    }

    public Headers(Headers other) {
        this.headerMap = other.headerMap;
    }

    public Headers(Map<String, List<String>> map) {
        this.headerMap = map;
    }

    public Headers(HttpURLConnection connection) {
        headerMap = connection.getHeaderFields();
    }

    public Headers clear() {
        headerMap.clear();
        return this;
    }

    public Headers addHeader(String name, String value) {
        if (name != null) {
            if (value == null) value = "";
            if (headerMap.containsKey(name)) {
                headerMap.get(name).add(value);
            } else {
                List<String> list = new ArrayList<>();
                list.add(value);
                headerMap.put(name, list);
            }
        }
        return this;
    }

    public Headers addHeaders(String name, List<String> values) {
        headerMap.put(name, values);
        return this;
    }

    public Headers removeHeader(String name) {
        headerMap.remove(name);
        return this;
    }

    public String getValue(String name) {
        List<String> list = headerMap.get(name);
        if (list != null && !list.isEmpty()) {
            if (list.size() == 1) {
                return list.get(0);
            } else {
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < list.size(); i++) {
                    String s = list.get(i);
                    builder.append(s);
                    if (i < list.size() - 1) {
                        builder.append(",");
                    }
                }
                return builder.toString();
            }
        }
        return "";
    }

    public Set<String> keySet() {
        return headerMap.keySet();
    }

    public Collection<List<String>> values() {
        return headerMap.values();
    }

    public List<String> values(String name) {
        return headerMap.get(name);
    }

    public int size() {
        return headerMap.size();
    }

    public boolean containsKey(String name) {
        return headerMap.containsKey(name);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String key : headerMap.keySet()) {
            builder.append(key);
            builder.append("=");
            builder.append(headerMap.get(key));
            builder.append("&");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
