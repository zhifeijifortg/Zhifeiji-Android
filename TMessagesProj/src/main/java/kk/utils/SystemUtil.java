package kk.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.telephony.TelephonyManager;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by LSD on 2021/3/3.
 * Desc
 */
public class SystemUtil {
    private static long lastTotalRxBytes = 0;
    private static long lastTimeStamp = 0;
    private static Map<String, Long> downLoadMap = new HashMap<>();
    private static Map<String, Long> timeMap = new HashMap<>();

    public static long getNetSpeed() {
        long nowTotalRxBytes = getTotalRxBytes();
        long nowTimeStamp = System.currentTimeMillis();
        long offset = nowTimeStamp - lastTimeStamp;
        if (offset == 0) offset = 1000;
        long speed = (nowTotalRxBytes - lastTotalRxBytes) * 1000 / offset;//秒转换
        if (speed == 0) speed = 100;
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return speed;
    }

    private static long getTotalRxBytes() {
        return TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);//转为KB
    }

    public static float getDownloadSpeed(int id, long downloadSize) {
        float speed;
        SharedPreferences g_preferences = MessagesController.getMainSettings(UserConfig.selectedAccount);
        long lastSize = g_preferences.getLong("downLoad_" + id, 0l);
        long lastTime = g_preferences.getLong("time_" + id, 0l);

        long now = System.currentTimeMillis();
        speed = (float) (((downloadSize - lastSize) / 1024.0) * 1000 / ((now - lastTime)));//秒转换

        g_preferences.edit().putLong("downLoad_" + id, downloadSize).commit();
        g_preferences.edit().putLong("time_" + id, now).commit();
        if (speed <= 0) speed = 100;
        return speed;
    }

    public static void removeDownloadData(int id) {
        SharedPreferences g_preferences = MessagesController.getMainSettings(UserConfig.selectedAccount);
        g_preferences.edit().remove("downLoad_" + id).commit();
        g_preferences.edit().remove("time_" + id).commit();
    }

    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }

    public static String getFileName(String fileUrl) {
        if ((fileUrl != null) && (fileUrl.length() > 0)) {
            int separator = fileUrl.lastIndexOf('/');
            if ((separator > -1) && (separator < (fileUrl.length() - 1))) {
                return fileUrl.substring(separator + 1);
            }
        }
        return "";
    }

    public static boolean deleteFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int String_length(String value) {
        int valueLength = 0;
        String chinese = "[\u4e00-\u9fa5]";
        for (int i = 0; i < value.length(); i++) {
            String temp = value.substring(i, i + 1);
            if (temp.matches(chinese)) {
                valueLength += 2;
            } else {
                valueLength += 1;
            }
        }
        return valueLength;
    }

    public static String timeTransfer(int time) {
        String h = String.format("%02d", time / 3600);
        String m = String.format("%02d", (time - Integer.parseInt(h) * 3600) / 60);
        String s = String.format("%02d", (time - Integer.parseInt(h) * 3600) % 60);
        String str = m + ":" + s;
        if (!h.equals("00")) {
            str = h + ":" + str;
        }
        return str;
    }

    public static String getSizeFormat(long sise) {
        String unit = "KB";
        float formatSize = sise / 1024.0f;//kb
        if (formatSize > 1024) {
            unit = "MB";
            formatSize = formatSize / 1024.0f;//mb
        }
        if (formatSize > 1024) {
            formatSize = formatSize / 1024.0f;//gb
            unit = "GB";
        }
        return String.format("%.1f", formatSize) + unit;
    }

    public static String getCountryZipCode(Context context) {
        String CountryZipCode = "";
        Locale locale = context.getResources().getConfiguration().locale;
        CountryZipCode = locale.getCountry();
        return CountryZipCode;
    }

    public static String getTelContry(Context context) {
        TelephonyManager teleMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (teleMgr != null) {
            String countryISOCode = teleMgr.getSimCountryIso();
            return countryISOCode;
        }
        return "";
    }
}
