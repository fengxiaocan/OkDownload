package com.x.down.listener;

import com.x.down.m3u8.M3U8Info;

import java.io.BufferedReader;

public interface OnM3u8ParseIntercept {
    M3U8Info intercept(BufferedReader reader);
}
