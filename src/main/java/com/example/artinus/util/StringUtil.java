package com.example.artinus.util;

public class StringUtil {

    private StringUtil() {
    }

    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
}
