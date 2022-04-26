package com.x.down.data;

public class MediaType {
    public static MediaType CONTENT_TYPE = parse("application/x-www-form-urlencoded");
    public static MediaType TEXT = parse("text/plain");
    public static MediaType MIXED = parse("multipart/mixed");
    public static MediaType ALTERNATIVE = parse("multipart/alternative");
    public static MediaType DIGEST = parse("multipart/digest");
    public static MediaType PARALLEL = parse("multipart/parallel");
    public static MediaType FORM = parse("multipart/form-data");
    private final String type;

    private MediaType(String type) {
        this.type = type;
    }

    public static MediaType parse(String type) {
        return new MediaType(type);
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
