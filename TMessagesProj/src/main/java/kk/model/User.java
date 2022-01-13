package kk.model;

import java.io.Serializable;

/**
 * Created by LSD on 2020/10/11.
 * Desc
 */
public class User implements Serializable {
    public boolean newer;
    public UserEntity user;
    public String token;

    public static class UserEntity implements Serializable {
        public String name;
        public String avatar;
        public String desc;
        public String device;
        public int platform;
        public String user_id;
    }
}
