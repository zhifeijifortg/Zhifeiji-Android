package kk.utils;

import io.paperdb.Paper;
import kk.Constants;
import kk.model.SystemEntity;
import kk.model.User;

/**
 * Created by LSD on 2021/4/1.
 * Desc
 */
public class PaperUtil {
    //userInfo
    public static User userInfo() {
        return Paper.book().read("userInfo" + (Constants.DEBUG ? "DEBUG" : ""), null);
    }

    public static void userInfo(User user) {
        Paper.book().write("userInfo" + (Constants.DEBUG ? "DEBUG" : ""), user);
    }


    //systemInfo
    public static void systemInfo(SystemEntity system) {
        Paper.book().write("systemInfo" + (Constants.DEBUG ? "DEBUG" : ""), system);
    }

    public static SystemEntity systemInfo() {
        return Paper.book().read("systemInfo" + (Constants.DEBUG ? "DEBUG" : ""), null);
    }

    //firstLoad
    public static void firstLoad(boolean load) {
        Paper.book().write("firstLoad" + (Constants.DEBUG ? "DEBUG" : ""), load);
    }

    public static boolean firstLoad() {
        return Paper.book().read("firstLoad" + (Constants.DEBUG ? "DEBUG" : ""), true);
    }

    //firstLogin
    public static void firstLogin(boolean login) {
        Paper.book().write("firstLogin" + (Constants.DEBUG ? "DEBUG" : ""), login);
    }

    public static boolean firstLogin() {
        return Paper.book().read("firstLogin" + (Constants.DEBUG ? "DEBUG" : ""), true);
    }
}
