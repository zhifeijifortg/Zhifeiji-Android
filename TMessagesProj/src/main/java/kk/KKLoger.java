package kk;

import com.google.android.exoplayer2.util.Log;

public class KKLoger {

    public static void d(String tag, String msg) {
        log(tag, msg);
    }

    public static void logChatActivity(String msg) {
        log("ChatActivity", msg);
    }

    public static void logClassifyMessageManager(String msg) {
        log("ClassifyMessageManager", msg);
    }

    public static void logDialogs(String msg) {
        log("Dialogs", msg);
    }

    public static void logVideoDataManager(String msg) {
        log("VideoDataManager", msg);
    }

    public static void logLocalVideoManager(String msg) {
        log("LocalVideoManager", msg);
    }

    public static void logFileData(String msg) {
        log("FileData", msg);
    }

    public static void e(String tag, String msg) {
        if (Constants.DEBUG) {
            Log.e(tag, msg);
        }
    }

    public static void log(String tag, String msg) {
        if (Constants.DEBUG) {
            Log.d(tag, msg);
        }
    }
}
