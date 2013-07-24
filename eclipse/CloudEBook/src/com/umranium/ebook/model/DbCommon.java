package com.umranium.ebook.model;


public class DbCommon {

    //private static final String TAG = "DbCommmon";

    public static final int MAX_DESCR_STR_LEN = 200;
    public static final int MAX_CONTENT_URL_LEN = 100;
    public static final int MAX_CONTENT_LEN = 10 * 1024 * 1024;
    public static final int MAX_THUMBNAIL_LEN = 64 * 1024;
    public static final int MAX_DATA_LEN = 10 * 1024 * 1024;
    public static final int MAX_NOTE_LEN = 1024;
    public static final int MAX_SUMMARY_LEN = 100;

    private static final String ELLIPSIS = " ... ";

    public static String summarize(String str) {
        int strLen = str.length();

        if (strLen <= MAX_SUMMARY_LEN) {
            return str;
        }

        int ellipsisLen = ELLIPSIS.length();

        int startLen = (MAX_SUMMARY_LEN - ellipsisLen) / 2;

        int endLen = MAX_SUMMARY_LEN - ellipsisLen - startLen;

        return str.substring(0, startLen) + ELLIPSIS +
                str.substring(strLen - endLen, strLen);
    }

}
