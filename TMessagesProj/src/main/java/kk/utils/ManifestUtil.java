package kk.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

/**
 * Created by LSD on 16/6/16.
 */
public class ManifestUtil {

    public static String getUMChannel(Context context) {
        String channel = getMetaDataValue(context, "UMENG_CHANNEL");
        if (TextUtils.isEmpty(channel)) {
            channel = "official";
        }
        return channel;
    }

    public static String getUMKey(Context context) {
        String channel = getMetaDataValue(context, "UMENG_KEY");
        if (TextUtils.isEmpty(channel)) {
            channel = "";
        }
        return channel;
    }

    /**
     * 获取application中指定的meta-data
     *
     * @return 如果没有获取成功(没有对应值，或者异常)，则返回值为空
     */
    public static String getMetaDataValue(Context ctx, String key) {
        if (ctx == null || TextUtils.isEmpty(key)) {
            return null;
        }
        String resultData = null;
        try {
            PackageManager packageManager = ctx.getPackageManager();
            if (packageManager != null) {
                ApplicationInfo applicationInfo = packageManager.getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA);
                if (applicationInfo != null) {
                    if (applicationInfo.metaData != null) {
                        resultData = applicationInfo.metaData.getString(key);
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return resultData;
    }
}
