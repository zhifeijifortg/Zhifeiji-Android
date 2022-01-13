package kk.model;

import kk.files.KKFileTypes;

/**
 * Created by LSD on 2021/3/20.
 * Desc
 */
public class LocalVideoEntity {
    public int type;//1：SD卡扫描的，2：下载完成的
    public String name;
    public String path;
    public KKFileTypes fileType = KKFileTypes.ALL;
    public long time;
    public long size;
    public int duration = 0;
    public int videoId = -1;

    public String fromText;

    public boolean deleteSelect;//删除标志

    public LocalVideoEntity() {
    }

    public LocalVideoEntity(String name, String path, long time, long size) {
        this.name = name;
        this.path = path;
        this.time = time;
        this.size = size;
    }
}
