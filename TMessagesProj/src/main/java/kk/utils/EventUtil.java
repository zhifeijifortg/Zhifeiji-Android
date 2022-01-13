package kk.utils;

import android.content.Context;

import com.flurry.android.FlurryAgent;
import com.umeng.analytics.MobclickAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by LSD on 2020/12/26.
 * Desc
 */
public class EventUtil {
    public enum Even {
        第一次打开("first_open"),
        欢迎页展示("start_page_show"),
        欢迎页开始按钮点击("start_button_click"),
        电话号码页面展示("phone_number_show"),
        电话号码页面下一步("phone_number_next"),
        确认验证码页面展示("check_code_show"),
        app聊天页面展示("chat_page_show"),
        video页面展示("video_page_show"),
        video页面视频下载点击("video_page_download_click"),
        video页面浮动按钮点击("video_page_fab_click"),
        video页面缓存展示("video_page_cached_show"),
        视频播放("video_play");

        public String eventId;

        Even(String eventId) {
            this.eventId = eventId;
        }
    }

    public static void post(Context context, Even event, Map<String, Object> data) {
        if (data == null) data = new HashMap<>();
        if (!data.containsKey("uid")) {
            data.put("uid", LoginManager.getUserId() + "");
            data.put("token", LoginManager.getUserToken() + "");
        }
        //KKLoger.d("TTT",event.eventId);

        //友盟
        MobclickAgent.onEventObject(context, event.eventId, data);

        //flurry
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            map.put(entry.getKey(), (String) entry.getValue());
        }
        FlurryAgent.logEvent(event.eventId, map);
    }
}
