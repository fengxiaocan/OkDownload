package com.x.down.listener;

import com.x.down.m3u8.M3U8Info;

import java.io.BufferedReader;

public interface OnM3u8Intercept {
    void intercept(M3U8Info info);

    M3U8Info intercept(BufferedReader reader);

    class IMPL implements OnM3u8Intercept{

        @Override
        public void intercept(M3U8Info info) {
        }

        @Override
        public M3U8Info intercept(BufferedReader reader) {
            return null;
        }
    }
}
