package kk.utils;

import android.text.TextUtils;

import kk.model.User;

/**
 * Created by LSD on 2017/6/30.
 */

public class LoginManager {

    //登录
    public static void login(User user) {
        PaperUtil.userInfo(user);
    }


    //登录？
    public static boolean isLogin() {
        User user = getUser();
        return (user != null && !TextUtils.isEmpty(user.token));
    }

    //获取User
    public static User getUser() {
        return PaperUtil.userInfo();
    }

    //用户getUserToken
    public static String getUserToken() {
        User user = getUser();
        if (user == null) {
            return "";
        } else {
            return user.token;
        }
    }

    //用户getUserId
    public static String getUserId() {
        User user = getUser();
        if (user == null) {
            return "";
        } else {
            return user.user.user_id;
        }
    }
}
