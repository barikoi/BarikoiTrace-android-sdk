package com.barikoi.barikoitrace.Utils;


public final class BufferError {

    public static <T> T m357a(String str, T t) {
        if (t != null) {
            return t;
        }
        throw new IllegalArgumentException(str + " can not be null");
    }


    public static void m358a(String str, boolean z) {
        if (!z) {
            throw new IllegalArgumentException("state should be: " + str);
        }
    }
}
