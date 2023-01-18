package com.ok.request.listener;

import com.ok.request.info.M3U8Info;

import java.io.BufferedReader;

public interface OnM3u8ParseIntercept {
    void intercept(M3U8Info info);

    M3U8Info intercept(BufferedReader reader);

    class IMPL implements OnM3u8ParseIntercept {

        @Override
        public void intercept(M3U8Info info) {
        }

        @Override
        public M3U8Info intercept(BufferedReader reader) {
            return null;
        }
    }
}
