package com.hisense.hitsp;

/**
 * 版本信息
 */
public final class VERSION {
    public static final int MajorVersion = 1;
    public static final int MinorVersion = 0;
    public static final int RevisionVersion = 0;

    public VERSION() {
    }

    public static String getVersionNumber() {
        return "1.0.0";
    }

    public static String getDescription() {
        return "Thanks for JHipster";
    }
}
