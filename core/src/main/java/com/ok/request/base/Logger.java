package com.ok.request.base;

public class Logger {
    public static boolean openLog = true;

    public static void logE(String format, Object... args) {
        if (openLog) {
            System.err.println(String.format(format, args));
        }
    }

    public static void logV(String format, Object... args) {
        if (openLog) {
            System.out.println(String.format(format, args));
        }
    }
}
