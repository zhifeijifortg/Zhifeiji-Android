package kk.files;

public class TagGlobalSearchFactory {
    public static TagSearch getTagGlobalSearch(KKFileTypes type) {
        switch (type) {
            case ALL:
            case IGNORE:
            case UNKNOWN:
            case TAG_OTHERS:
            case TAG_VIDEO:
            case TAG_FILES:
                throw new IllegalArgumentException("No TagGlobalSearch for " + type);
            case VIDEO_FILTER:
                return new VideoTagGlobalSearch(type);
            case APK:
            case DOC:
            case MKV:
            case MOV:
            case MP4:
            case TXT:
                return new DocumentTagGlobalSearch(type);
            default:
                throw new IllegalStateException();
        }
    }
}
