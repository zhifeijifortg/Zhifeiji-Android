package kk.files;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息过滤文件类型
 */
public enum KKFileTypes {
    ALL(""),
    IGNORE("_ignore"),
    UNKNOWN("_unknown"),
    VIDEO_FILTER("_video_filter"),
    TAG_VIDEO("_tag_video"),
    TAG_FILES("_tag_files"),
    TAG_OTHERS("_tag_others"),
    MKV(".mkv"),
    TXT(".txt"),
    MP4(".mp4"),
    MOV(".mov"),
    DOC(".doc"),
    APK(".apk");

    private final String suffix;

    public String getSuffix() {
        return suffix;
    }

    public String getQueryString() {
        if (isValidSingleType(this)) {
            return suffix;
        } else {
            return "";
        }
    }

    KKFileTypes(String suffix) {
        this.suffix = suffix;
    }

    public static KKFileTypes parseFileType(String suffix) {
        for (KKFileTypes value : KKFileTypes.values()) {
            if (value.suffix.equals(suffix.toLowerCase())) {
                return value;
            }
        }
        return UNKNOWN;
    }

    public static boolean isValidSingleType(KKFileTypes type) {
        return type.suffix.startsWith(".");
    }

    public static List<KKFileTypes> getTabShowTypes() {
        List<KKFileTypes> newList = new ArrayList<>();
        newList.add(ALL);
        newList.add(TAG_VIDEO);
        newList.add(TAG_FILES);
        newList.add(TAG_OTHERS);
        return newList;
    }

    public static KKFileTypes[] getQueries(KKFileTypes type) {
        switch (type) {
            case IGNORE:
            case UNKNOWN:
                throw new IllegalArgumentException("Wrong type " + type + " for getQueries()");
            case ALL:
                return new KKFileTypes[]{
                        VIDEO_FILTER,
                        MKV,
                        TXT,
                        MP4,
                        MOV,
                        DOC,
                        APK
                };
            case TAG_FILES:
                return new KKFileTypes[]{
                        TXT,
                        DOC
                };
            case TAG_VIDEO:
                return new KKFileTypes[]{
                        VIDEO_FILTER,
                        MP4,
                        MOV,
                        MKV
                };
            case TAG_OTHERS:
                return new KKFileTypes[]{
                        APK
                };
            case TXT:
            case MP4:
            case MOV:
            case MKV:
            case DOC:
            case APK:
            case VIDEO_FILTER:
                return new KKFileTypes[]{type};
            default:
                throw new IllegalStateException();
        }
    }
}
