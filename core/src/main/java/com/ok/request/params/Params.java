package com.ok.request.params;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

public class Params {
    private final HashMap<String, String> params;

    public Params() {
        params = new HashMap<>();
    }

    public Params(Params other) {
        this.params = other.params;
    }

    public Params clear() {
        params.clear();
        return this;
    }

    public Params addParams(String name, String value) {
        params.put(name, value);
        return this;
    }

    public Params removeParams(String name) {
        params.remove(name);
        return this;
    }

    public String getValue(String name) {
        return params.get(name);
    }

    public Set<String> nameSet() {
        return params.keySet();
    }

    public Collection<String> values() {
        return params.values();
    }

    public int size() {
        return params.size();
    }

    public boolean containsName(String name) {
        return params.containsKey(name);
    }

    public boolean containsValue(String value) {
        return params.containsValue(value);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder builder) {
        for (String key : params.keySet()) {
            builder.append(key);
            builder.append("=");
            builder.append(params.get(key));
            builder.append("&");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder;
    }
}
